package com.linkedin.orrery.graphql.resolvers.view;

import static com.linkedin.orrery.graphql.TestUtils.*;
import static com.linkedin.metadata.utils.CriterionUtils.buildCriterion;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.testng.Assert.*;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.linkedin.common.AuditStamp;
import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.data.template.StringArray;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.OrreryView;
import com.linkedin.orrery.graphql.generated.OrreryViewDefinitionInput;
import com.linkedin.orrery.graphql.generated.OrreryViewFilterInput;
import com.linkedin.orrery.graphql.generated.EntityType;
import com.linkedin.orrery.graphql.generated.FacetFilterInput;
import com.linkedin.orrery.graphql.generated.FilterOperator;
import com.linkedin.orrery.graphql.generated.LogicalOperator;
import com.linkedin.orrery.graphql.generated.UpdateViewInput;
import com.linkedin.entity.Aspect;
import com.linkedin.entity.AspectType;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.EnvelopedAspect;
import com.linkedin.entity.EnvelopedAspectMap;
import com.linkedin.entity.client.EntityClient;
import com.linkedin.metadata.Constants;
import com.linkedin.metadata.query.filter.Condition;
import com.linkedin.metadata.query.filter.ConjunctiveCriterion;
import com.linkedin.metadata.query.filter.ConjunctiveCriterionArray;
import com.linkedin.metadata.query.filter.CriterionArray;
import com.linkedin.metadata.query.filter.Filter;
import com.linkedin.metadata.service.ViewService;
import com.linkedin.view.OrreryViewDefinition;
import com.linkedin.view.OrreryViewInfo;
import com.linkedin.view.OrreryViewType;
import graphql.schema.DataFetchingEnvironment;
import java.util.concurrent.CompletionException;
import org.mockito.Mockito;
import org.testng.annotations.Test;

public class UpdateViewResolverTest {

  private static final Urn TEST_URN = UrnUtils.getUrn("urn:li:orreryView:test-id");
  private static final Urn TEST_AUTHORIZED_USER = UrnUtils.getUrn("urn:li:corpuser:auth");
  private static final Urn TEST_UNAUTHORIZED_USER = UrnUtils.getUrn("urn:li:corpuser:no-auth");

  private static final UpdateViewInput TEST_INPUT =
      new UpdateViewInput(
          "test-name",
          "test-description",
          new OrreryViewDefinitionInput(
              ImmutableList.of(EntityType.DATASET, EntityType.DASHBOARD),
              new OrreryViewFilterInput(
                  LogicalOperator.AND,
                  ImmutableList.of(
                      new FacetFilterInput(
                          "test1",
                          ImmutableList.of("value1", "value2"),
                          false,
                          FilterOperator.EQUAL),
                      new FacetFilterInput(
                          "test2", ImmutableList.of("value1", "value2"), true, FilterOperator.IN)),
                  null,
                  null)));

  @Test
  public void testGetSuccessGlobalViewIsCreator() throws Exception {
    ViewService mockService = initViewService(OrreryViewType.GLOBAL);
    UpdateViewResolver resolver = new UpdateViewResolver(mockService);

    // Execute resolver - user is allowed since he owns the thing.
    QueryContext mockContext = getMockDenyContext(TEST_AUTHORIZED_USER.toString());
    DataFetchingEnvironment mockEnv = Mockito.mock(DataFetchingEnvironment.class);
    Mockito.when(mockEnv.getArgument(Mockito.eq("urn"))).thenReturn(TEST_URN.toString());
    Mockito.when(mockEnv.getArgument(Mockito.eq("input"))).thenReturn(TEST_INPUT);
    Mockito.when(mockEnv.getContext()).thenReturn(mockContext);

    OrreryView view = resolver.get(mockEnv).get();
    assertEquals(view.getName(), TEST_INPUT.getName());
    assertEquals(view.getDescription(), TEST_INPUT.getDescription());
    assertEquals(view.getViewType(), com.linkedin.orrery.graphql.generated.OrreryViewType.GLOBAL);
    assertEquals(view.getType(), EntityType.ORRERY_VIEW);

    Mockito.verify(mockService, Mockito.times(1))
        .updateView(
            any(),
            Mockito.eq(TEST_URN),
            Mockito.eq(TEST_INPUT.getName()),
            Mockito.eq(TEST_INPUT.getDescription()),
            Mockito.eq(
                new OrreryViewDefinition()
                    .setEntityTypes(
                        new StringArray(
                            ImmutableList.of(
                                Constants.DATASET_ENTITY_NAME, Constants.DASHBOARD_ENTITY_NAME)))
                    .setFilter(
                        new Filter()
                            .setOr(
                                new ConjunctiveCriterionArray(
                                    ImmutableList.of(
                                        new ConjunctiveCriterion()
                                            .setAnd(
                                                new CriterionArray(
                                                    ImmutableList.of(
                                                        buildCriterion(
                                                            "test1",
                                                            Condition.EQUAL,
                                                            "value1",
                                                            "value2"),
                                                        buildCriterion(
                                                            "test2",
                                                            Condition.IN,
                                                            true,
                                                            "value1",
                                                            "value2"))))))))),
            Mockito.anyLong());
  }

  @Test
  public void testGetSuccessGlobalViewManageGlobalViews() throws Exception {
    ViewService mockService = initViewService(OrreryViewType.GLOBAL);
    UpdateViewResolver resolver = new UpdateViewResolver(mockService);

    // Execute resolver - user is allowed since he owns the thing.
    QueryContext mockContext = getMockAllowContext(TEST_UNAUTHORIZED_USER.toString());
    DataFetchingEnvironment mockEnv = Mockito.mock(DataFetchingEnvironment.class);
    Mockito.when(mockEnv.getArgument(Mockito.eq("urn"))).thenReturn(TEST_URN.toString());
    Mockito.when(mockEnv.getArgument(Mockito.eq("input"))).thenReturn(TEST_INPUT);
    Mockito.when(mockEnv.getContext()).thenReturn(mockContext);

    OrreryView view = resolver.get(mockEnv).get();
    assertEquals(view.getName(), TEST_INPUT.getName());
    assertEquals(view.getDescription(), TEST_INPUT.getDescription());
    assertEquals(view.getViewType(), com.linkedin.orrery.graphql.generated.OrreryViewType.GLOBAL);
    assertEquals(view.getType(), EntityType.ORRERY_VIEW);

    Mockito.verify(mockService, Mockito.times(1))
        .updateView(
            any(),
            Mockito.eq(TEST_URN),
            Mockito.eq(TEST_INPUT.getName()),
            Mockito.eq(TEST_INPUT.getDescription()),
            Mockito.eq(
                new OrreryViewDefinition()
                    .setEntityTypes(
                        new StringArray(
                            ImmutableList.of(
                                Constants.DATASET_ENTITY_NAME, Constants.DASHBOARD_ENTITY_NAME)))
                    .setFilter(
                        new Filter()
                            .setOr(
                                new ConjunctiveCriterionArray(
                                    ImmutableList.of(
                                        new ConjunctiveCriterion()
                                            .setAnd(
                                                new CriterionArray(
                                                    ImmutableList.of(
                                                        buildCriterion(
                                                            "test1",
                                                            Condition.EQUAL,
                                                            "value1",
                                                            "value2"),
                                                        buildCriterion(
                                                            "test2",
                                                            Condition.IN,
                                                            true,
                                                            "value1",
                                                            "value2"))))))))),
            Mockito.anyLong());
  }

  @Test
  public void testGetViewServiceException() throws Exception {
    // Update resolver
    ViewService mockService = Mockito.mock(ViewService.class);
    Mockito.doThrow(RuntimeException.class)
        .when(mockService)
        .updateView(any(), any(Urn.class), any(), any(), any(), Mockito.anyLong());

    UpdateViewResolver resolver = new UpdateViewResolver(mockService);

    // Execute resolver
    DataFetchingEnvironment mockEnv = Mockito.mock(DataFetchingEnvironment.class);
    QueryContext mockContext = getMockAllowContext();
    Mockito.when(mockEnv.getArgument(Mockito.eq("urn"))).thenReturn(TEST_URN.toString());
    Mockito.when(mockEnv.getArgument(Mockito.eq("input"))).thenReturn(TEST_INPUT);
    Mockito.when(mockEnv.getContext()).thenReturn(mockContext);

    assertThrows(CompletionException.class, () -> resolver.get(mockEnv).join());
  }

  @Test
  public void testGetUnauthorized() throws Exception {
    // Update resolver
    EntityClient mockClient = Mockito.mock(EntityClient.class);
    ViewService mockService = initViewService(OrreryViewType.GLOBAL);
    UpdateViewResolver resolver = new UpdateViewResolver(mockService);

    // Execute resolver
    DataFetchingEnvironment mockEnv = Mockito.mock(DataFetchingEnvironment.class);
    QueryContext mockContext = getMockDenyContext(TEST_UNAUTHORIZED_USER.toString());
    Mockito.when(mockEnv.getArgument(Mockito.eq("urn"))).thenReturn(TEST_URN.toString());
    Mockito.when(mockEnv.getArgument(Mockito.eq("input"))).thenReturn(TEST_INPUT);
    Mockito.when(mockEnv.getContext()).thenReturn(mockContext);

    assertThrows(CompletionException.class, () -> resolver.get(mockEnv).join());
    Mockito.verify(mockClient, Mockito.times(0)).ingestProposal(any(), any(), anyBoolean());
  }

  private static ViewService initViewService(OrreryViewType viewType) {
    ViewService mockService = Mockito.mock(ViewService.class);

    OrreryViewInfo testInfo =
        new OrreryViewInfo()
            .setType(viewType)
            .setName(TEST_INPUT.getName())
            .setDescription(TEST_INPUT.getDescription())
            .setCreated(new AuditStamp().setActor(TEST_AUTHORIZED_USER).setTime(0L))
            .setLastModified(new AuditStamp().setActor(TEST_AUTHORIZED_USER).setTime(0L))
            .setDefinition(
                new OrreryViewDefinition()
                    .setEntityTypes(new StringArray())
                    .setFilter(new Filter()));

    EntityResponse testEntityResponse =
        new EntityResponse()
            .setUrn(TEST_URN)
            .setEntityName(Constants.ORRERY_VIEW_ENTITY_NAME)
            .setAspects(
                new EnvelopedAspectMap(
                    ImmutableMap.of(
                        Constants.ORRERY_VIEW_INFO_ASPECT_NAME,
                        new EnvelopedAspect()
                            .setName(Constants.ORRERY_VIEW_INFO_ASPECT_NAME)
                            .setType(AspectType.VERSIONED)
                            .setValue(new Aspect(testInfo.data())))));

    Mockito.when(mockService.getViewInfo(any(), Mockito.eq(TEST_URN))).thenReturn(testInfo);

    Mockito.when(mockService.getViewEntityResponse(any(), Mockito.eq(TEST_URN)))
        .thenReturn(testEntityResponse);

    return mockService;
  }
}
