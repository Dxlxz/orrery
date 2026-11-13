package com.linkedin.orrery.graphql.types;

import static com.linkedin.orrery.graphql.TestUtils.getMockAllowContext;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.orrery.graphql.AspectLoadContext;
import com.linkedin.orrery.graphql.AspectMappingRegistry;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.featureflags.FeatureFlags;
import com.linkedin.orrery.graphql.types.file.OrreryFileType;
import com.linkedin.orrery.graphql.types.template.PageTemplateType;
import com.linkedin.orrery.graphql.util.AspectUtils;
import com.linkedin.entity.Aspect;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.EnvelopedAspect;
import com.linkedin.entity.EnvelopedAspectMap;
import com.linkedin.entity.client.EntityClient;
import com.linkedin.file.BucketStorageLocation;
import com.linkedin.file.OrreryFileInfo;
import com.linkedin.file.FileUploadScenario;
import com.linkedin.metadata.Constants;
import com.linkedin.metadata.config.OrreryAppConfiguration;
import com.linkedin.template.OrreryPageTemplateProperties;
import com.linkedin.template.OrreryPageTemplateRowArray;
import com.linkedin.template.OrreryPageTemplateSurface;
import com.linkedin.template.OrreryPageTemplateVisibility;
import com.linkedin.template.PageTemplateScope;
import com.linkedin.template.PageTemplateSurfaceType;
import graphql.execution.DataFetcherResult;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.testng.annotations.Test;

/**
 * Some loaders depend on an aspect no matter which fields were selected: OrreryFileMapper and
 * PageTemplateMapper map the entity to null without theirs (and both are non-null GraphQL fields,
 * so {@code createOrreryFile { file { urn } }} fails the whole operation), while DocumentMapper
 * feeds its aspects into an access decision. A selection made up entirely of {@code @noAspects}
 * fields must still fetch them.
 */
public class RequiredAspectLoaderTest {

  private static final String FILE_URN = "urn:li:orreryFile:test_file";
  private static final String TEMPLATE_URN = "urn:li:orreryPageTemplate:test_template";

  /** Request-scoped context whose accumulated selection is urn-only (no aspects required). */
  private static final class UrnOnlySelectionContext implements QueryContext {
    private final ConcurrentHashMap<String, AspectLoadContext> contexts = new ConcurrentHashMap<>();
    private final QueryContext delegate;
    private final com.linkedin.metadata.config.OrreryAppConfiguration appConfigOverride;

    UrnOnlySelectionContext(QueryContext delegate, String entityTypeName) {
      this(delegate, entityTypeName, null);
    }

    UrnOnlySelectionContext(
        QueryContext delegate,
        String entityTypeName,
        com.linkedin.metadata.config.OrreryAppConfiguration appConfigOverride) {
      this.delegate = delegate;
      this.appConfigOverride = appConfigOverride;
      this.contexts.put(entityTypeName, AspectLoadContext.of(Set.of()));
    }

    @Override
    public boolean isAuthenticated() {
      return delegate.isAuthenticated();
    }

    @Override
    public com.orrery.authentication.Authentication getAuthentication() {
      return delegate.getAuthentication();
    }

    @Override
    public com.orrery.plugins.auth.authorization.Authorizer getAuthorizer() {
      return delegate.getAuthorizer();
    }

    @Override
    public io.orreryproject.metadata.context.OperationContext getOperationContext() {
      return delegate.getOperationContext();
    }

    @Override
    public com.linkedin.metadata.config.OrreryAppConfiguration getOrreryAppConfig() {
      return appConfigOverride != null ? appConfigOverride : delegate.getOrreryAppConfig();
    }

    @Override
    public int getMaxParentDepth() {
      return delegate.getMaxParentDepth();
    }

    @Override
    public AspectMappingRegistry getAspectMappingRegistry() {
      return null;
    }

    @Override
    public void setAspectMappingRegistry(AspectMappingRegistry aspectMappingRegistry) {}

    @Override
    public void mergeAspectLoadContext(String entityTypeName, AspectLoadContext loadContext) {
      contexts.merge(entityTypeName, loadContext, AspectLoadContext::union);
    }

    @Override
    public AspectLoadContext getAspectLoadContext(String entityTypeName) {
      return contexts.get(entityTypeName);
    }
  }

  @Test
  public void testOrreryFileHydratesRequiredInfoForUrnOnlySelection() throws Exception {
    Urn urn = UrnUtils.getUrn(FILE_URN);
    EntityClient entityClient = mock(EntityClient.class);
    when(entityClient.batchGetV2(any(), eq(Constants.ORRERY_FILE_ENTITY_NAME), any(), any()))
        .thenAnswer(
            invocation -> {
              @SuppressWarnings("unchecked")
              Set<String> requested = (Set<String>) invocation.getArgument(3);
              Map<String, EnvelopedAspect> aspects = new HashMap<>();
              if (requested.contains(Constants.ORRERY_FILE_INFO_ASPECT_NAME)) {
                aspects.put(
                    Constants.ORRERY_FILE_INFO_ASPECT_NAME,
                    new EnvelopedAspect().setValue(new Aspect(fileInfo().data())));
              }
              return Map.of(
                  urn,
                  new EntityResponse()
                      .setEntityName(Constants.ORRERY_FILE_ENTITY_NAME)
                      .setUrn(urn)
                      .setAspects(new EnvelopedAspectMap(aspects)));
            });

    QueryContext context = new UrnOnlySelectionContext(getMockAllowContext(), "OrreryFile");
    List<DataFetcherResult<com.linkedin.orrery.graphql.generated.OrreryFile>> results =
        new OrreryFileType(entityClient).batchLoad(List.of(FILE_URN), context);

    assertNotNull(
        results.get(0).getData(),
        "urn-only selection must still fetch orreryFileInfo; the mapper nulls the entity without"
            + " it and OrreryFile is non-nullable");
  }

  @Test
  public void testPageTemplateHydratesRequiredPropertiesForUrnOnlySelection() throws Exception {
    Urn urn = UrnUtils.getUrn(TEMPLATE_URN);
    EntityClient entityClient = mock(EntityClient.class);
    when(entityClient.batchGetV2(
            any(), eq(Constants.ORRERY_PAGE_TEMPLATE_ENTITY_NAME), any(), any()))
        .thenAnswer(
            invocation -> {
              @SuppressWarnings("unchecked")
              Set<String> requested = (Set<String>) invocation.getArgument(3);
              Map<String, EnvelopedAspect> aspects = new HashMap<>();
              if (requested.contains(Constants.ORRERY_PAGE_TEMPLATE_PROPERTIES_ASPECT_NAME)) {
                aspects.put(
                    Constants.ORRERY_PAGE_TEMPLATE_PROPERTIES_ASPECT_NAME,
                    new EnvelopedAspect().setValue(new Aspect(templateProperties().data())));
              }
              return Map.of(
                  urn,
                  new EntityResponse()
                      .setEntityName(Constants.ORRERY_PAGE_TEMPLATE_ENTITY_NAME)
                      .setUrn(urn)
                      .setAspects(new EnvelopedAspectMap(aspects)));
            });

    QueryContext context =
        new UrnOnlySelectionContext(getMockAllowContext(), "OrreryPageTemplate");
    List<DataFetcherResult<com.linkedin.orrery.graphql.generated.OrreryPageTemplate>> results =
        new PageTemplateType(entityClient).batchLoad(List.of(TEMPLATE_URN), context);

    assertNotNull(
        results.get(0).getData(),
        "urn-only selection must still fetch orreryPageTemplateProperties; the mapper nulls the"
            + " entity without it");
  }

  /**
   * Aspect names each loader depends on, stated literally so this test is an independent source of
   * truth rather than a mirror of the production table. Reading the expectation back out of {@code
   * AspectUtils} would only catch removal of the fold-in, not a wrong or outdated aspect name.
   *
   * <p>Structural dependencies (mapper nulls or throws) for the first six; authorization dependency
   * (canViewDocument redacts) for Document.
   */
  private static final Map<String, Set<String>> EXPECTED_HYDRATION_REQUIRED_ASPECTS =
      Map.of(
          "OrreryFile", Set.of("orreryFileInfo"),
          "OrreryPageTemplate", Set.of("orreryPageTemplateProperties"),
          "Test", Set.of("testInfo"),
          "Incident", Set.of("incidentInfo"),
          "DataContract", Set.of("dataContractProperties"),
          "OrreryConnection", Set.of("orreryConnectionDetails", "dataPlatformInstance"),
          "Document", Set.of("documentInfo", "subTypes"));

  /**
   * A urn-only selection must still fetch every hydration-required aspect. This protects the
   * loaders whose mappers null/throw (Test, Incident, DataContract, OrreryConnection, File,
   * PageTemplate) and the one whose authorization depends on aspects (Document), without each
   * needing a bespoke behavioral test.
   */
  @Test
  public void testHydrationRequiredAspectsFoldedIntoOptimizedSet() {
    for (Map.Entry<String, Set<String>> expected : EXPECTED_HYDRATION_REQUIRED_ASPECTS.entrySet()) {
      String typeName = expected.getKey();
      QueryContext context = new UrnOnlySelectionContext(getMockAllowContext(), typeName);

      Set<String> optimized =
          AspectUtils.getOptimizedAspects(context, typeName, Set.of("someDefault"), "someKey");

      assertTrue(
          optimized.containsAll(expected.getValue()),
          typeName
              + " urn-only selection must still fetch "
              + expected.getValue()
              + ", got "
              + optimized);
    }
  }

  /**
   * Registration guard: the production table must cover exactly the types above. Catches both a
   * silently dropped entry and a new entry added without a stated expectation here.
   */
  @Test
  public void testHydrationRequiredTableMatchesExpectations() {
    assertEquals(
        AspectUtils.getHydrationRequiredTypes(),
        EXPECTED_HYDRATION_REQUIRED_ASPECTS.keySet(),
        "a type was registered or removed without updating the expectations in this test");
    for (Map.Entry<String, Set<String>> expected : EXPECTED_HYDRATION_REQUIRED_ASPECTS.entrySet()) {
      assertEquals(
          AspectUtils.getHydrationRequiredAspects(expected.getKey()),
          expected.getValue(),
          "registered aspects for " + expected.getKey() + " drifted from the expected set");
    }
  }

  /**
   * A selection built entirely from {@code @noAspects} fields must never resolve to an empty aspect
   * set. batchGetV2 returns no row for a zero-aspect request and loaders map a missing row to a
   * null entity, so the entity disappears rather than merely carrying fewer aspects. This is what
   * broke the per-column getLineageCounts query for SchemaFieldEntity (whose urn, type, fieldPath,
   * parent and lineage fields are all @noAspects), surfacing in the UI as "Column has no lineage".
   */
  @Test
  public void testEmptyOptimizedSetFallsBackToDefaults() {
    QueryContext context = new UrnOnlySelectionContext(getMockAllowContext(), "SchemaFieldEntity");
    Set<String> defaults = Set.of("structuredProperties", "deprecation");

    Set<String> resolved = AspectUtils.getOptimizedAspects(context, "SchemaFieldEntity", defaults);

    assertFalse(
        resolved.isEmpty(),
        "an all-@noAspects selection must not produce an empty batchGetV2 aspect set");
    assertEquals(resolved, defaults);
  }

  /** With the kill switch off, hydration must revert to the full default aspect set. */
  @Test
  public void testOptimizationDisabledFallsBackToDefaults() {
    FeatureFlags flags = new FeatureFlags();
    flags.setGraphQLAspectOptimizationEnabled(false);
    OrreryAppConfiguration appConfig = new OrreryAppConfiguration();
    appConfig.setFeatureFlags(flags);

    QueryContext context = new UrnOnlySelectionContext(getMockAllowContext(), "Dataset", appConfig);
    Set<String> defaults = Set.of("datasetKey", "ownership", "globalTags");

    Set<String> resolved =
        AspectUtils.getOptimizedAspects(context, "Dataset", defaults, "datasetKey");

    assertEquals(
        resolved, defaults, "disabled optimization must return the full default aspect set");
  }

  private static OrreryFileInfo fileInfo() {
    return new OrreryFileInfo()
        .setBucketStorageLocation(
            new BucketStorageLocation().setStorageBucket("bucket").setStorageKey("key"))
        .setOriginalFileName("file.txt")
        .setMimeType("text/plain")
        .setSizeInBytes(10L)
        .setScenario(FileUploadScenario.ASSET_DOCUMENTATION);
  }

  private static OrreryPageTemplateProperties templateProperties() {
    return new OrreryPageTemplateProperties()
        .setRows(new OrreryPageTemplateRowArray())
        .setSurface(
            new OrreryPageTemplateSurface().setSurfaceType(PageTemplateSurfaceType.HOME_PAGE))
        .setVisibility(new OrreryPageTemplateVisibility().setScope(PageTemplateScope.PERSONAL));
  }
}
