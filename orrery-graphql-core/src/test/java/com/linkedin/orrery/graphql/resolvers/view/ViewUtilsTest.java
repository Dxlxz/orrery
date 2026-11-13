package com.linkedin.orrery.graphql.resolvers.view;

import static com.linkedin.orrery.graphql.TestUtils.*;
import static com.linkedin.metadata.utils.CriterionUtils.buildCriterion;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.*;

import com.google.common.collect.ImmutableList;
import com.linkedin.common.AuditStamp;
import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.data.template.StringArray;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.OrreryViewDefinitionInput;
import com.linkedin.orrery.graphql.generated.OrreryViewFilterInput;
import com.linkedin.orrery.graphql.generated.EntityType;
import com.linkedin.orrery.graphql.generated.FacetFilterInput;
import com.linkedin.orrery.graphql.generated.FilterOperator;
import com.linkedin.orrery.graphql.generated.LogicalOperator;
import com.linkedin.metadata.Constants;
import com.linkedin.metadata.aspect.AspectRetriever;
import com.linkedin.metadata.query.filter.Condition;
import com.linkedin.metadata.query.filter.ConjunctiveCriterion;
import com.linkedin.metadata.query.filter.ConjunctiveCriterionArray;
import com.linkedin.metadata.query.filter.CriterionArray;
import com.linkedin.metadata.query.filter.Filter;
import com.linkedin.metadata.service.ViewService;
import com.linkedin.view.OrreryViewDefinition;
import com.linkedin.view.OrreryViewInfo;
import com.linkedin.view.OrreryViewType;
import graphql.Assert;
import org.mockito.Mockito;
import org.testng.annotations.Test;

public class ViewUtilsTest {

  private static final Urn TEST_AUTHORIZED_USER = UrnUtils.getUrn("urn:li:corpuser:auth");
  private static final Urn TEST_UNAUTHORIZED_USER = UrnUtils.getUrn("urn:li:corpuser:no-auth");

  private static final Urn TEST_VIEW_URN = UrnUtils.getUrn("urn:li:orreryView:test");

  @Test
  public static void testCanCreatePersonalViewAllowed() {
    boolean res =
        ViewUtils.canCreateView(OrreryViewType.PERSONAL, Mockito.mock(QueryContext.class));
    Assert.assertTrue(res);
  }

  @Test
  public static void testCanCreateGlobalViewAllowed() {
    QueryContext context = getMockAllowContext(TEST_AUTHORIZED_USER.toString());
    boolean res = ViewUtils.canCreateView(OrreryViewType.GLOBAL, context);
    Assert.assertTrue(res);
  }

  @Test
  public static void testCanCreateGlobalViewDenied() {
    QueryContext context = getMockDenyContext(TEST_AUTHORIZED_USER.toString());
    boolean res = ViewUtils.canCreateView(OrreryViewType.GLOBAL, context);
    Assert.assertFalse(res);
  }

  @Test
  public void testCanUpdateViewSuccessGlobalViewIsCreator() {
    ViewService mockService = initViewService(OrreryViewType.GLOBAL);
    QueryContext mockContext = getMockDenyContext(TEST_AUTHORIZED_USER.toString());

    assertTrue(ViewUtils.canUpdateView(mockService, TEST_VIEW_URN, mockContext));

    Mockito.verify(mockService, Mockito.times(1)).getViewInfo(any(), Mockito.eq(TEST_VIEW_URN));
  }

  @Test
  public void testCanUpdateViewSuccessGlobalViewCanManageGlobalViews() {
    ViewService mockService = initViewService(OrreryViewType.GLOBAL);
    QueryContext mockContext = getMockDenyContext(TEST_AUTHORIZED_USER.toString());

    assertTrue(ViewUtils.canUpdateView(mockService, TEST_VIEW_URN, mockContext));

    Mockito.verify(mockService, Mockito.times(1)).getViewInfo(any(), Mockito.eq(TEST_VIEW_URN));
  }

  @Test
  public void testGetFailureGlobalViewIsNotCreatorOrManager() {
    ViewService mockService = initViewService(OrreryViewType.GLOBAL);
    QueryContext mockContext = getMockDenyContext(TEST_UNAUTHORIZED_USER.toString());

    assertFalse(ViewUtils.canUpdateView(mockService, TEST_VIEW_URN, mockContext));

    Mockito.verify(mockService, Mockito.times(1)).getViewInfo(any(), Mockito.eq(TEST_VIEW_URN));
  }

  @Test
  public void testGetSuccessPersonalViewIsCreator() {
    ViewService mockService = initViewService(OrreryViewType.PERSONAL);
    QueryContext mockContext = getMockDenyContext(TEST_AUTHORIZED_USER.toString());

    assertTrue(ViewUtils.canUpdateView(mockService, TEST_VIEW_URN, mockContext));

    Mockito.verify(mockService, Mockito.times(1)).getViewInfo(any(), Mockito.eq(TEST_VIEW_URN));
  }

  @Test
  public void testGetFailurePersonalViewIsNotCreator() {
    ViewService mockService = initViewService(OrreryViewType.PERSONAL);
    QueryContext mockContext = getMockDenyContext(TEST_UNAUTHORIZED_USER.toString());

    assertFalse(ViewUtils.canUpdateView(mockService, TEST_VIEW_URN, mockContext));

    Mockito.verify(mockService, Mockito.times(1)).getViewInfo(any(), Mockito.eq(TEST_VIEW_URN));
  }

  @Test
  public void testMapDefinition() throws Exception {

    OrreryViewDefinitionInput input =
        new OrreryViewDefinitionInput(
            ImmutableList.of(EntityType.DATASET, EntityType.DASHBOARD),
            new OrreryViewFilterInput(
                LogicalOperator.AND,
                ImmutableList.of(
                    new FacetFilterInput(
                        "test1", ImmutableList.of("value1", "value2"), false, FilterOperator.IN),
                    new FacetFilterInput(
                        "test2",
                        ImmutableList.of("value3", "value4"),
                        true,
                        FilterOperator.CONTAIN)),
                null, // orFilters - using old format
                null)); // json

    OrreryViewDefinition expectedResult =
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
                                                    "test1", Condition.IN, "value1", "value2"),
                                                buildCriterion(
                                                    "test2",
                                                    Condition.CONTAIN,
                                                    true,
                                                    "value3",
                                                    "value4"))))))));

    assertEquals(ViewUtils.mapDefinition(input, mock(AspectRetriever.class)), expectedResult);
  }

  @Test
  public void testMapDefinitionWithOrFilters() throws Exception {
    // Test that orFilters takes precedence over operator+filters
    OrreryViewDefinitionInput input =
        new OrreryViewDefinitionInput(
            ImmutableList.of(EntityType.DATASET),
            new OrreryViewFilterInput(
                LogicalOperator.AND, // This should be ignored when orFilters is provided
                ImmutableList.of(
                    new FacetFilterInput(
                        "ignored", ImmutableList.of("ignored"), false, FilterOperator.IN)),
                ImmutableList.of(
                    new com.linkedin.orrery.graphql.generated.AndFilterInput(
                        ImmutableList.of(
                            new FacetFilterInput(
                                "field1",
                                ImmutableList.of("value1"),
                                false,
                                FilterOperator.EQUAL)))),
                null)); // json

    OrreryViewDefinition result = ViewUtils.mapDefinition(input, mock(AspectRetriever.class));

    assertNotNull(result);
    assertNotNull(result.getFilter());
    // Should use orFilters, not the operator+filters
    assertEquals(result.getFilter().getOr().size(), 1);
  }

  @Test
  public void testMapDefinitionUnknownOperatorThrows() throws Exception {
    // Test that unknown operators throw IllegalArgumentException
    OrreryViewFilterInput filterInput = new OrreryViewFilterInput();
    filterInput.setOperator(LogicalOperator.AND);
    filterInput.setFilters(
        ImmutableList.of(
            new FacetFilterInput(
                "field1", ImmutableList.of("value1"), false, FilterOperator.EQUAL)));

    OrreryViewDefinitionInput input =
        new OrreryViewDefinitionInput(ImmutableList.of(EntityType.DATASET), filterInput);

    // Manually set an unknown operator (this would require reflection or mocking)
    // For now, we test the explicit unknown operator validation
    OrreryViewFilterInput invalidFilter = new OrreryViewFilterInput();
    invalidFilter.setOperator(null); // Should be handled
    invalidFilter.setFilters(
        ImmutableList.of(
            new FacetFilterInput(
                "field1", ImmutableList.of("value1"), false, FilterOperator.EQUAL)));

    OrreryViewDefinitionInput invalidInput =
        new OrreryViewDefinitionInput(ImmutableList.of(EntityType.DATASET), invalidFilter);

    // Both operator and filters without valid operator should handle gracefully
    assertThrows(
        IllegalArgumentException.class,
        () -> ViewUtils.mapDefinition(invalidInput, mock(AspectRetriever.class)));
  }

  private static ViewService initViewService(OrreryViewType viewType) {
    ViewService mockService = Mockito.mock(ViewService.class);

    OrreryViewInfo testInfo =
        new OrreryViewInfo()
            .setType(viewType)
            .setName("test-name")
            .setDescription("test-description")
            .setCreated(new AuditStamp().setActor(TEST_AUTHORIZED_USER).setTime(0L))
            .setLastModified(new AuditStamp().setActor(TEST_AUTHORIZED_USER).setTime(0L))
            .setDefinition(
                new OrreryViewDefinition()
                    .setEntityTypes(new StringArray())
                    .setFilter(new Filter()));

    Mockito.when(mockService.getViewInfo(any(), Mockito.eq(TEST_VIEW_URN))).thenReturn(testInfo);

    return mockService;
  }
}
