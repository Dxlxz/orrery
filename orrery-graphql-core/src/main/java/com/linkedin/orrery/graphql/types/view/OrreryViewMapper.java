package com.linkedin.orrery.graphql.types.view;

import static com.linkedin.metadata.Constants.*;

import com.linkedin.data.DataMap;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.OrreryView;
import com.linkedin.orrery.graphql.generated.OrreryViewDefinition;
import com.linkedin.orrery.graphql.generated.OrreryViewFilter;
import com.linkedin.orrery.graphql.generated.OrreryViewType;
import com.linkedin.orrery.graphql.generated.EntityType;
import com.linkedin.orrery.graphql.generated.FacetFilter;
import com.linkedin.orrery.graphql.generated.FilterOperator;
import com.linkedin.orrery.graphql.generated.LogicalOperator;
import com.linkedin.orrery.graphql.types.common.mappers.util.MappingHelper;
import com.linkedin.orrery.graphql.types.entitytype.EntityTypeMapper;
import com.linkedin.orrery.graphql.types.mappers.ModelMapper;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.EnvelopedAspectMap;
import com.linkedin.metadata.query.filter.ConjunctiveCriterion;
import com.linkedin.metadata.query.filter.Criterion;
import com.linkedin.view.OrreryViewInfo;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrreryViewMapper implements ModelMapper<EntityResponse, OrreryView> {

  private static final String KEYWORD_FILTER_SUFFIX = ".keyword";
  public static final OrreryViewMapper INSTANCE = new OrreryViewMapper();

  public static OrreryView map(
      @Nullable final QueryContext context, @Nonnull final EntityResponse entityResponse) {
    return INSTANCE.apply(context, entityResponse);
  }

  @Override
  public OrreryView apply(
      @Nullable final QueryContext context, @Nonnull final EntityResponse entityResponse) {
    final OrreryView result = new OrreryView();
    result.setUrn(entityResponse.getUrn().toString());
    result.setType(EntityType.ORRERY_VIEW);
    EnvelopedAspectMap aspectMap = entityResponse.getAspects();
    MappingHelper<OrreryView> mappingHelper = new MappingHelper<>(aspectMap, result);
    mappingHelper.mapToResult(ORRERY_VIEW_INFO_ASPECT_NAME, this::mapOrreryViewInfo);
    return mappingHelper.getResult();
  }

  private void mapOrreryViewInfo(@Nonnull final OrreryView view, @Nonnull final DataMap dataMap) {
    OrreryViewInfo viewInfo = new OrreryViewInfo(dataMap);
    view.setDescription(viewInfo.getDescription());
    view.setViewType(OrreryViewType.valueOf(viewInfo.getType().toString()));
    view.setName(viewInfo.getName());
    view.setDescription(viewInfo.getDescription());
    view.setDefinition(mapViewDefinition(viewInfo.getDefinition()));
  }

  @Nonnull
  private OrreryViewDefinition mapViewDefinition(
      @Nonnull final com.linkedin.view.OrreryViewDefinition definition) {
    final OrreryViewDefinition result = new OrreryViewDefinition();
    result.setFilter(mapFilter(definition.getFilter(), definition.getJson()));
    result.setEntityTypes(
        definition.getEntityTypes().stream()
            .map(EntityTypeMapper::getType)
            .collect(Collectors.toList()));
    return result;
  }

  @Nullable
  private OrreryViewFilter mapFilter(
      @Nonnull final com.linkedin.metadata.query.filter.Filter filter,
      @Nullable final String json) {
    final OrreryViewFilter result = new OrreryViewFilter();

    // For new views, return only json and don't reconstruct deprecated operator+filters
    if (json != null) {
      result.setJson(json);
      return result;
    }

    // Fallback: reconstruct operator+filters from Filter object for old views
    if (filter.hasOr() && filter.getOr().size() == 1) {
      // Then we are looking at an AND with multiple sub conditions.
      result.setOperator(LogicalOperator.AND);
      result.setFilters(mapAndFilters(filter.getOr().get(0).getAnd()));
    } else {
      result.setOperator(LogicalOperator.OR);
      // Then we are looking at an OR with a group of sub conditions.
      result.setFilters(mapOrFilters(filter.getOr()));
    }

    return result;
  }

  /** This simply converts a List of leaf criterion into the FacetFiler equivalent. */
  @Nonnull
  private List<FacetFilter> mapAndFilters(@Nullable final List<Criterion> ands) {
    // If the array is missing, return empty array.
    if (ands == null) {
      log.warn("Found a View without any AND filter criteria. Returning empty filter list.");
      return Collections.emptyList();
    }
    return ands.stream().map(this::mapCriterion).collect(Collectors.toList());
  }

  /**
   * This converts a list of Conjunctive Criterion into a flattened list of FacetFilters. This
   * method makes the assumption that WE (our GraphQL API) has minted the View and that each or
   * criterion contains at maximum one nested condition.
   */
  @Nonnull
  private List<FacetFilter> mapOrFilters(@Nullable final List<ConjunctiveCriterion> ors) {
    if (ors == null) {
      log.warn("Found a View without any OR filter criteria. Returning empty filter list.");
      return Collections.emptyList();
    }
    if (ors.stream().anyMatch(or -> or.hasAnd() && or.getAnd().size() > 1)) {
      log.warn(
          String.format(
              "Detected a View with a malformed filter clause. OR view has children conjunctions with more than one Criterion. Returning empty filters. %s",
              ors));
      return Collections.emptyList();
    }
    // It is assumed that in this case, the view is a flat list of ORs. Thus, we filter
    // for all nested AND conditions containing only 1 entry.
    return ors.stream()
        .filter(or -> or.hasAnd() && or.getAnd().size() == 1)
        .map(or -> or.getAnd().get(0))
        .map(this::mapCriterion)
        .collect(Collectors.toList());
  }

  @Nonnull
  private FacetFilter mapCriterion(@Nonnull final Criterion andFilter) {
    final FacetFilter result = new FacetFilter();
    result.setField(stripKeyword(andFilter.getField()));
    result.setValues(andFilter.getValues());
    if (andFilter.hasCondition()) {
      result.setCondition(FilterOperator.valueOf(andFilter.getCondition().toString()));
    }
    if (andFilter.hasNegated()) {
      result.setNegated(andFilter.isNegated());
    }
    return result;
  }

  private String stripKeyword(final String fieldName) {
    // When the Filter is persisted, it may include a .keyword suffix.
    if (fieldName.endsWith(KEYWORD_FILTER_SUFFIX)) {
      return fieldName.substring(0, fieldName.length() - KEYWORD_FILTER_SUFFIX.length());
    }
    return fieldName;
  }
}
