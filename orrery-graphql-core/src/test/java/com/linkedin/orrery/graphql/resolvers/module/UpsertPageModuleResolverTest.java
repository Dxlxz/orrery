package com.linkedin.orrery.graphql.resolvers.module;

import static com.linkedin.orrery.graphql.TestUtils.getMockAllowContext;
import static com.linkedin.orrery.graphql.TestUtils.getMockDenyContext;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertThrows;

import com.linkedin.common.AuditStamp;
import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.exception.AuthorizationException;
import com.linkedin.orrery.graphql.generated.OrreryPageModule;
import com.linkedin.orrery.graphql.generated.OrreryPageModuleType;
import com.linkedin.orrery.graphql.generated.HierarchyViewModuleParamsInput;
import com.linkedin.orrery.graphql.generated.LinkModuleParamsInput;
import com.linkedin.orrery.graphql.generated.PageModuleParamsInput;
import com.linkedin.orrery.graphql.generated.PageModuleScope;
import com.linkedin.orrery.graphql.generated.RichTextModuleParamsInput;
import com.linkedin.orrery.graphql.generated.UpsertPageModuleInput;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.EnvelopedAspect;
import com.linkedin.entity.EnvelopedAspectMap;
import com.linkedin.metadata.Constants;
import com.linkedin.metadata.service.PageModuleService;
import com.linkedin.module.OrreryPageModuleParams;
import com.linkedin.module.OrreryPageModuleProperties;
import com.linkedin.module.RichTextModuleParams;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class UpsertPageModuleResolverTest {

  private static final String TEST_MODULE_URN = "urn:li:orreryPageModule:test-module";
  private static final String TEST_MODULE_NAME = "Test Module";
  private static final String TEST_RICH_TEXT_CONTENT = "Test content";

  private PageModuleService mockService;
  private UpsertPageModuleResolver resolver;
  private DataFetchingEnvironment mockEnvironment;
  private QueryContext mockQueryContext;

  @BeforeMethod
  public void setUp() {
    mockService = mock(PageModuleService.class);
    resolver = new UpsertPageModuleResolver(mockService);
    mockEnvironment = mock(DataFetchingEnvironment.class);
    mockQueryContext = getMockAllowContext();
  }

  @Test
  public void testUpsertPageModuleSuccessWithUrn() throws Exception {
    // Arrange
    UpsertPageModuleInput input = new UpsertPageModuleInput();
    input.setUrn(TEST_MODULE_URN);
    input.setName(TEST_MODULE_NAME);
    input.setType(OrreryPageModuleType.RICH_TEXT);
    input.setScope(PageModuleScope.PERSONAL);

    RichTextModuleParamsInput richTextParams = new RichTextModuleParamsInput();
    richTextParams.setContent(TEST_RICH_TEXT_CONTENT);
    PageModuleParamsInput paramsInput = new PageModuleParamsInput();
    paramsInput.setRichTextParams(richTextParams);
    input.setParams(paramsInput);

    Urn moduleUrn = UrnUtils.getUrn(TEST_MODULE_URN);
    EntityResponse mockResponse = createMockEntityResponse(moduleUrn);

    when(mockEnvironment.getArgument("input")).thenReturn(input);
    when(mockEnvironment.getContext()).thenReturn(mockQueryContext);
    when(mockService.upsertPageModule(any(), eq(TEST_MODULE_URN), any(), any(), any(), any()))
        .thenReturn(moduleUrn);
    when(mockService.getPageModuleEntityResponse(any(), eq(moduleUrn))).thenReturn(mockResponse);

    // Act
    CompletableFuture<OrreryPageModule> future = resolver.get(mockEnvironment);
    OrreryPageModule result = future.get();

    // Assert
    assertNotNull(result);
    assertEquals(result.getUrn(), TEST_MODULE_URN);
    assertEquals(result.getType().toString(), "ORRERY_PAGE_MODULE");
    verify(mockService, times(1))
        .upsertPageModule(any(), eq(TEST_MODULE_URN), any(), any(), any(), any());
    verify(mockService, times(1)).getPageModuleEntityResponse(any(), eq(moduleUrn));
  }

  @Test
  public void testUpsertPageModuleSuccessWithGeneratedUrn() throws Exception {
    // Arrange
    UpsertPageModuleInput input = new UpsertPageModuleInput();
    input.setName(TEST_MODULE_NAME);
    input.setType(OrreryPageModuleType.RICH_TEXT);
    input.setScope(PageModuleScope.PERSONAL);

    RichTextModuleParamsInput richTextParams = new RichTextModuleParamsInput();
    richTextParams.setContent(TEST_RICH_TEXT_CONTENT);
    PageModuleParamsInput paramsInput = new PageModuleParamsInput();
    paramsInput.setRichTextParams(richTextParams);
    input.setParams(paramsInput);

    Urn moduleUrn = UrnUtils.getUrn(TEST_MODULE_URN);
    EntityResponse mockResponse = createMockEntityResponse(moduleUrn);

    when(mockEnvironment.getArgument("input")).thenReturn(input);
    when(mockEnvironment.getContext()).thenReturn(mockQueryContext);
    when(mockService.upsertPageModule(any(), eq(null), any(), any(), any(), any()))
        .thenReturn(moduleUrn);
    when(mockService.getPageModuleEntityResponse(any(), eq(moduleUrn))).thenReturn(mockResponse);

    // Act
    CompletableFuture<OrreryPageModule> future = resolver.get(mockEnvironment);
    OrreryPageModule result = future.get();

    // Assert
    assertNotNull(result);
    assertEquals(result.getUrn(), TEST_MODULE_URN);
    assertEquals(result.getType().toString(), "ORRERY_PAGE_MODULE");
    verify(mockService, times(1)).upsertPageModule(any(), eq(null), any(), any(), any(), any());
    verify(mockService, times(1)).getPageModuleEntityResponse(any(), eq(moduleUrn));
  }

  @Test
  public void testUpsertPageModuleWithLinkParams() throws Exception {
    // Arrange
    UpsertPageModuleInput input = new UpsertPageModuleInput();
    input.setName(TEST_MODULE_NAME);
    input.setType(OrreryPageModuleType.LINK);
    input.setScope(PageModuleScope.PERSONAL);

    PageModuleParamsInput paramsInput = new PageModuleParamsInput();
    paramsInput.setLinkParams(new LinkModuleParamsInput());
    paramsInput.getLinkParams().setLinkUrl("https://example.com/test-link");
    input.setParams(paramsInput);

    Urn moduleUrn = UrnUtils.getUrn(TEST_MODULE_URN);
    EntityResponse mockResponse = createMockEntityResponse(moduleUrn);

    when(mockEnvironment.getArgument("input")).thenReturn(input);
    when(mockEnvironment.getContext()).thenReturn(mockQueryContext);
    when(mockService.upsertPageModule(any(), any(), any(), any(), any(), any()))
        .thenReturn(moduleUrn);
    when(mockService.getPageModuleEntityResponse(any(), eq(moduleUrn))).thenReturn(mockResponse);

    // Act
    CompletableFuture<OrreryPageModule> future = resolver.get(mockEnvironment);
    OrreryPageModule result = future.get();

    // Assert
    assertNotNull(result);
    assertEquals(result.getUrn(), TEST_MODULE_URN);
    verify(mockService, times(1)).upsertPageModule(any(), any(), any(), any(), any(), any());
    verify(mockService, times(1)).getPageModuleEntityResponse(any(), eq(moduleUrn));
  }

  @Test
  public void testUpsertPageModuleWithHierarchyParams() throws Exception {
    // Arrange
    UpsertPageModuleInput input = new UpsertPageModuleInput();
    input.setName(TEST_MODULE_NAME);
    input.setType(OrreryPageModuleType.HIERARCHY);
    input.setScope(PageModuleScope.PERSONAL);

    PageModuleParamsInput paramsInput = new PageModuleParamsInput();
    paramsInput.setHierarchyViewParams(new HierarchyViewModuleParamsInput());
    paramsInput.getHierarchyViewParams().setAssetUrns(List.of("urn:li:domain:test-domain"));
    paramsInput.getHierarchyViewParams().setShowRelatedEntities(true);
    paramsInput.getHierarchyViewParams().setRelatedEntitiesFilterJson("{}");
    input.setParams(paramsInput);

    Urn moduleUrn = UrnUtils.getUrn(TEST_MODULE_URN);
    EntityResponse mockResponse = createMockEntityResponse(moduleUrn);

    when(mockEnvironment.getArgument("input")).thenReturn(input);
    when(mockEnvironment.getContext()).thenReturn(mockQueryContext);
    when(mockService.upsertPageModule(any(), any(), any(), any(), any(), any()))
        .thenReturn(moduleUrn);
    when(mockService.getPageModuleEntityResponse(any(), eq(moduleUrn))).thenReturn(mockResponse);

    // Act
    CompletableFuture<OrreryPageModule> future = resolver.get(mockEnvironment);
    OrreryPageModule result = future.get();

    // Assert
    assertNotNull(result);
    assertEquals(result.getUrn(), TEST_MODULE_URN);
    verify(mockService, times(1)).upsertPageModule(any(), any(), any(), any(), any(), any());
    verify(mockService, times(1)).getPageModuleEntityResponse(any(), eq(moduleUrn));
  }

  @Test
  public void testUpsertGlobalPageModuleSuccessWithPermission() throws Exception {
    // Arrange
    UpsertPageModuleInput input = new UpsertPageModuleInput();
    input.setName(TEST_MODULE_NAME);
    input.setType(OrreryPageModuleType.RICH_TEXT);
    input.setScope(PageModuleScope.GLOBAL);

    RichTextModuleParamsInput richTextParams = new RichTextModuleParamsInput();
    richTextParams.setContent(TEST_RICH_TEXT_CONTENT);
    PageModuleParamsInput paramsInput = new PageModuleParamsInput();
    paramsInput.setRichTextParams(richTextParams);

    input.setParams(paramsInput);

    Urn moduleUrn = UrnUtils.getUrn(TEST_MODULE_URN);
    EntityResponse mockResponse = createMockEntityResponse(moduleUrn);

    when(mockEnvironment.getArgument("input")).thenReturn(input);
    when(mockEnvironment.getContext()).thenReturn(mockQueryContext);
    when(mockService.upsertPageModule(any(), eq(null), any(), any(), any(), any()))
        .thenReturn(moduleUrn);
    when(mockService.getPageModuleEntityResponse(any(), eq(moduleUrn))).thenReturn(mockResponse);

    // Act
    CompletableFuture<OrreryPageModule> future = resolver.get(mockEnvironment);
    OrreryPageModule result = future.get();

    // Assert
    assertNotNull(result);
    assertEquals(result.getUrn(), TEST_MODULE_URN);
    assertEquals(result.getType().toString(), "ORRERY_PAGE_MODULE");
    verify(mockService, times(1)).upsertPageModule(any(), eq(null), any(), any(), any(), any());
    verify(mockService, times(1)).getPageModuleEntityResponse(any(), eq(moduleUrn));
  }

  @Test
  public void testUpsertGlobalPageModuleFailureWithoutPermission() throws Exception {
    // Arrange
    UpsertPageModuleInput input = new UpsertPageModuleInput();
    input.setName(TEST_MODULE_NAME);
    input.setType(OrreryPageModuleType.RICH_TEXT);
    input.setScope(PageModuleScope.GLOBAL);

    RichTextModuleParamsInput richTextParams = new RichTextModuleParamsInput();
    richTextParams.setContent(TEST_RICH_TEXT_CONTENT);
    PageModuleParamsInput paramsInput = new PageModuleParamsInput();
    paramsInput.setRichTextParams(richTextParams);
    input.setParams(paramsInput);

    QueryContext mockDenyContext = getMockDenyContext();

    when(mockEnvironment.getArgument("input")).thenReturn(input);
    when(mockEnvironment.getContext()).thenReturn(mockDenyContext);

    // Act & Assert
    assertThrows(AuthorizationException.class, () -> resolver.get(mockEnvironment).get());
  }

  @Test
  public void testUpsertPageModuleValidationFailureRichTextWithoutParams() throws Exception {
    // Arrange
    UpsertPageModuleInput input = new UpsertPageModuleInput();
    input.setName(TEST_MODULE_NAME);
    input.setType(OrreryPageModuleType.RICH_TEXT);
    input.setScope(PageModuleScope.PERSONAL);

    // Don't set rich text params
    PageModuleParamsInput paramsInput = new PageModuleParamsInput();
    input.setParams(paramsInput);

    when(mockEnvironment.getArgument("input")).thenReturn(input);
    when(mockEnvironment.getContext()).thenReturn(mockQueryContext);

    // Act & Assert
    assertThrows(RuntimeException.class, () -> resolver.get(mockEnvironment).join());
  }

  @Test
  public void testUpsertPageModuleValidationFailureLinkWithoutParams() throws Exception {
    // Arrange
    UpsertPageModuleInput input = new UpsertPageModuleInput();
    input.setName(TEST_MODULE_NAME);
    input.setType(OrreryPageModuleType.LINK);
    input.setScope(PageModuleScope.PERSONAL);

    // Don't set link params
    PageModuleParamsInput paramsInput = new PageModuleParamsInput();
    input.setParams(paramsInput);

    when(mockEnvironment.getArgument("input")).thenReturn(input);
    when(mockEnvironment.getContext()).thenReturn(mockQueryContext);

    // Act & Assert
    assertThrows(RuntimeException.class, () -> resolver.get(mockEnvironment).join());
  }

  @Test
  public void testUpsertPageModuleValidationFailureHierarchyWithoutParams() throws Exception {
    // Arrange
    UpsertPageModuleInput input = new UpsertPageModuleInput();
    input.setName(TEST_MODULE_NAME);
    input.setType(OrreryPageModuleType.HIERARCHY);
    input.setScope(PageModuleScope.PERSONAL);

    // Don't set hierarchy params
    PageModuleParamsInput paramsInput = new PageModuleParamsInput();
    input.setParams(paramsInput);

    when(mockEnvironment.getArgument("input")).thenReturn(input);
    when(mockEnvironment.getContext()).thenReturn(mockQueryContext);

    // Act & Assert
    assertThrows(RuntimeException.class, () -> resolver.get(mockEnvironment).join());
  }

  @Test
  public void testUpsertPageModuleValidationFailureUnsupportedModuleType() throws Exception {
    // Arrange
    UpsertPageModuleInput input = new UpsertPageModuleInput();
    input.setName(TEST_MODULE_NAME);
    input.setType(OrreryPageModuleType.ASSET_COLLECTION); // Unsupported type
    input.setScope(PageModuleScope.PERSONAL);

    PageModuleParamsInput paramsInput = new PageModuleParamsInput();
    input.setParams(paramsInput);

    when(mockEnvironment.getArgument("input")).thenReturn(input);
    when(mockEnvironment.getContext()).thenReturn(mockQueryContext);

    // Act & Assert
    assertThrows(RuntimeException.class, () -> resolver.get(mockEnvironment).join());
  }

  @Test
  public void testUpsertPageModuleValidationFailureRichTextWithWrongParams() throws Exception {
    // Arrange
    UpsertPageModuleInput input = new UpsertPageModuleInput();
    input.setName(TEST_MODULE_NAME);
    input.setType(OrreryPageModuleType.RICH_TEXT);
    input.setScope(PageModuleScope.PERSONAL);

    // Set link params instead of rich text params
    PageModuleParamsInput paramsInput = new PageModuleParamsInput();
    paramsInput.setLinkParams(new LinkModuleParamsInput());
    paramsInput.getLinkParams().setLinkUrl("https://example.com/test-link");
    input.setParams(paramsInput);

    when(mockEnvironment.getArgument("input")).thenReturn(input);
    when(mockEnvironment.getContext()).thenReturn(mockQueryContext);

    // Act & Assert
    assertThrows(RuntimeException.class, () -> resolver.get(mockEnvironment).join());
  }

  @Test
  public void testUpsertPageModuleValidationFailureLinkWithWrongParams() throws Exception {
    // Arrange
    UpsertPageModuleInput input = new UpsertPageModuleInput();
    input.setName(TEST_MODULE_NAME);
    input.setType(OrreryPageModuleType.LINK);
    input.setScope(PageModuleScope.PERSONAL);

    // Set rich text params instead of link params
    PageModuleParamsInput paramsInput = new PageModuleParamsInput();
    RichTextModuleParamsInput richTextParams = new RichTextModuleParamsInput();
    richTextParams.setContent(TEST_RICH_TEXT_CONTENT);
    paramsInput.setRichTextParams(richTextParams);
    input.setParams(paramsInput);

    when(mockEnvironment.getArgument("input")).thenReturn(input);
    when(mockEnvironment.getContext()).thenReturn(mockQueryContext);

    // Act & Assert
    assertThrows(RuntimeException.class, () -> resolver.get(mockEnvironment).join());
  }

  @Test
  public void testUpsertPageModuleValidationFailureHierarchyWithWrongParams() throws Exception {
    // Arrange
    UpsertPageModuleInput input = new UpsertPageModuleInput();
    input.setName(TEST_MODULE_NAME);
    input.setType(OrreryPageModuleType.HIERARCHY);
    input.setScope(PageModuleScope.PERSONAL);

    // Set link params instead of hierarchy params
    PageModuleParamsInput paramsInput = new PageModuleParamsInput();
    paramsInput.setLinkParams(new LinkModuleParamsInput());
    paramsInput.getLinkParams().setLinkUrl("urn:li:post:test-post");
    input.setParams(paramsInput);

    when(mockEnvironment.getArgument("input")).thenReturn(input);
    when(mockEnvironment.getContext()).thenReturn(mockQueryContext);

    // Act & Assert
    assertThrows(RuntimeException.class, () -> resolver.get(mockEnvironment).join());
  }

  private EntityResponse createMockEntityResponse(Urn moduleUrn) {
    OrreryPageModuleProperties properties = new OrreryPageModuleProperties();
    properties.setName(TEST_MODULE_NAME);
    properties.setType(com.linkedin.module.OrreryPageModuleType.RICH_TEXT);

    com.linkedin.module.PageModuleScope gmsScope = com.linkedin.module.PageModuleScope.PERSONAL;
    com.linkedin.module.OrreryPageModuleVisibility visibility =
        new com.linkedin.module.OrreryPageModuleVisibility();
    visibility.setScope(gmsScope);
    properties.setVisibility(visibility);

    OrreryPageModuleParams params = new OrreryPageModuleParams();
    RichTextModuleParams richTextParams = new RichTextModuleParams();
    richTextParams.setContent(TEST_RICH_TEXT_CONTENT);
    params.setRichTextParams(richTextParams);
    properties.setParams(params);

    AuditStamp auditStamp = new AuditStamp();
    auditStamp.setTime(System.currentTimeMillis());
    auditStamp.setActor(UrnUtils.getUrn("urn:li:corpuser:test-user"));
    properties.setCreated(auditStamp);
    properties.setLastModified(auditStamp);

    EntityResponse response = new EntityResponse();
    response.setUrn(moduleUrn);

    EnvelopedAspectMap aspectMap = new EnvelopedAspectMap();
    EnvelopedAspect aspect = new EnvelopedAspect();
    aspect.setValue(new com.linkedin.entity.Aspect(properties.data()));
    aspectMap.put(Constants.ORRERY_PAGE_MODULE_PROPERTIES_ASPECT_NAME, aspect);
    response.setAspects(aspectMap);

    return response;
  }
}
