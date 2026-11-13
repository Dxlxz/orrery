package com.linkedin.orrery.graphql.resolvers.view;

import static com.linkedin.orrery.graphql.resolvers.ResolverUtils.*;

import com.google.common.collect.ImmutableList;
import com.linkedin.common.urn.Urn;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.concurrency.GraphQLConcurrencyUtils;
import com.linkedin.orrery.graphql.generated.AndFilterInput;
import com.linkedin.orrery.graphql.generated.OrreryView;
import com.linkedin.orrery.graphql.generated.OrreryViewType;
import com.linkedin.orrery.graphql.generated.EntityType;
import com.linkedin.orrery.graphql.generated.FacetFilterInput;
import com.linkedin.orrery.graphql.generated.FilterOperator;
import com.linkedin.orrery.graphql.generated.ListGlobalViewsInput;
import com.linkedin.orrery.graphql.generated.ListViewsResult;
import com.linkedin.entity.client.EntityClient;
import com.linkedin.metadata.Constants;
import com.linkedin.metadata.config.search.EntityIndexConfiguration;
import com.linkedin.metadata.query.filter.Filter;
import com.linkedin.metadata.query.filter.SortCriterion;
import com.linkedin.metadata.query.filter.SortOrder;
import com.linkedin.metadata.search.SearchEntity;
import com.linkedin.metadata.search.SearchResult;
import com.linkedin.metadata.search.elasticsearch.index.entity.v3.EntitySearchIndexResolver;
import com.linkedin.metadata.search.elasticsearch.index.entity.v3.MappingConstants;
import com.linkedin.metadata.search.elasticsearch.index.entity.v3.SearchFieldResolver.Intent;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.orreryproject.metadata.context.OperationContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

/** Resolver used for listing global Orrery Views. */
@Slf4j
public class ListGlobalViewsResolver implements DataFetcher<CompletableFuture<ListViewsResult>> {

  private static final String CREATED_AT_FIELD = "createdAt";
  private static final String VIEW_TYPE_FIELD = "type";
  private static final Integer DEFAULT_START = 0;
  private static final Integer DEFAULT_COUNT = 20;
  private static final String DEFAULT_QUERY = "";

  private final EntityClient _entityClient;
  @Nullable private final EntityIndexConfiguration _entityIndexConfiguration;

  public ListGlobalViewsResolver(@Nonnull final EntityClient entityClient) {
    this(entityClient, null);
  }

  public ListGlobalViewsResolver(
      @Nonnull final EntityClient entityClient,
      @Nullable final EntityIndexConfiguration entityIndexConfiguration) {
    _entityClient = Objects.requireNonNull(entityClient, "entityClient must not be null");
    _entityIndexConfiguration = entityIndexConfiguration;
  }

  @Override
  public CompletableFuture<ListViewsResult> get(final DataFetchingEnvironment environment)
      throws Exception {

    final QueryContext context = environment.getContext();
    final ListGlobalViewsInput input =
        bindArgument(environment.getArgument("input"), ListGlobalViewsInput.class);

    return GraphQLConcurrencyUtils.supplyAsync(
        () -> {
          final Integer start = input.getStart() == null ? DEFAULT_START : input.getStart();
          final Integer count = input.getCount() == null ? DEFAULT_COUNT : input.getCount();
          final String query = input.getQuery() == null ? DEFAULT_QUERY : input.getQuery();

          try {

            final SearchResult gmsResult =
                _entityClient.search(
                    context.getOperationContext().withSearchFlags(flags -> flags.setFulltext(true)),
                    Constants.ORRERY_VIEW_ENTITY_NAME,
                    query,
                    buildFilters(),
                    Collections.singletonList(sortCriterion(context.getOperationContext())),
                    start,
                    count);

            final ListViewsResult result = new ListViewsResult();
            result.setStart(gmsResult.getFrom());
            result.setCount(gmsResult.getPageSize());
            result.setTotal(gmsResult.getNumEntities());
            result.setViews(
                mapUnresolvedViews(
                    gmsResult.getEntities().stream()
                        .map(SearchEntity::getEntity)
                        .collect(Collectors.toList())));
            return result;
          } catch (Exception e) {
            throw new RuntimeException("Failed to list global Views", e);
          }
        },
        this.getClass().getSimpleName(),
        "get");
  }

  // This method maps urns returned from the list endpoint into Partial View objects which will be
  // resolved be a separate Batch resolver.
  private List<OrreryView> mapUnresolvedViews(final List<Urn> entityUrns) {
    final List<OrreryView> results = new ArrayList<>();
    for (final Urn urn : entityUrns) {
      final OrreryView unresolvedView = new OrreryView();
      unresolvedView.setUrn(urn.toString());
      unresolvedView.setType(EntityType.ORRERY_VIEW);
      results.add(unresolvedView);
    }
    return results;
  }

  private SortCriterion sortCriterion(@Nonnull final OperationContext opContext) {
    return new SortCriterion()
        .setField(
            EntitySearchIndexResolver.indexedField(
                _entityIndexConfiguration,
                opContext,
                CREATED_AT_FIELD,
                MappingConstants.SYSTEM_CREATED_TIME_FIELD,
                Intent.SORT))
        .setOrder(SortOrder.DESCENDING);
  }

  private Filter buildFilters() {
    final AndFilterInput globalCriteria = new AndFilterInput();
    List<FacetFilterInput> andConditions = new ArrayList<>();
    andConditions.add(
        new FacetFilterInput(
            VIEW_TYPE_FIELD,
            ImmutableList.of(OrreryViewType.GLOBAL.name()),
            false,
            FilterOperator.EQUAL));
    globalCriteria.setAnd(andConditions);
    return buildFilter(Collections.emptyList(), ImmutableList.of(globalCriteria));
  }
}
