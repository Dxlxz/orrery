package com.linkedin.orrery.graphql.resolvers.view;

import static com.linkedin.orrery.graphql.resolvers.ResolverUtils.*;

import com.linkedin.common.urn.Urn;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.concurrency.GraphQLConcurrencyUtils;
import com.linkedin.orrery.graphql.exception.AuthorizationException;
import com.linkedin.orrery.graphql.generated.CreateViewInput;
import com.linkedin.orrery.graphql.generated.OrreryView;
import com.linkedin.orrery.graphql.generated.OrreryViewDefinition;
import com.linkedin.orrery.graphql.generated.OrreryViewFilter;
import com.linkedin.orrery.graphql.generated.FacetFilter;
import com.linkedin.metadata.service.ViewService;
import com.linkedin.view.OrreryViewType;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;

/** Resolver responsible for updating a particular Orrery View */
@Slf4j
public class CreateViewResolver implements DataFetcher<CompletableFuture<OrreryView>> {

  private final ViewService _viewService;

  public CreateViewResolver(@Nonnull final ViewService viewService) {
    _viewService = Objects.requireNonNull(viewService);
  }

  @Override
  public CompletableFuture<OrreryView> get(final DataFetchingEnvironment environment)
      throws Exception {
    final QueryContext context = environment.getContext();
    final CreateViewInput input =
        bindArgument(environment.getArgument("input"), CreateViewInput.class);

    return GraphQLConcurrencyUtils.supplyAsync(
        () -> {
          if (ViewUtils.canCreateView(
              OrreryViewType.valueOf(input.getViewType().toString()), context)) {
            try {
              final Urn urn =
                  _viewService.createView(
                      context.getOperationContext(),
                      OrreryViewType.valueOf(input.getViewType().toString()),
                      input.getName(),
                      input.getDescription(),
                      ViewUtils.mapDefinition(
                          input.getDefinition(),
                          context.getOperationContext().getAspectRetriever()),
                      System.currentTimeMillis());
              return createView(urn, input);
            } catch (Exception e) {
              throw new RuntimeException(
                  String.format("Failed to create View with input: %s", input), e);
            }
          }
          throw new AuthorizationException(
              "Unauthorized to perform this action. Please contact your Orrery administrator.");
        },
        this.getClass().getSimpleName(),
        "get");
  }

  private OrreryView createView(@Nonnull final Urn urn, @Nonnull final CreateViewInput input) {
    final OrreryViewFilter.Builder filterBuilder = new OrreryViewFilter.Builder();

    if (input.getDefinition().getFilter().getJson() != null) {
      filterBuilder.setJson(input.getDefinition().getFilter().getJson());
    } else {
      // Legacy format fallback: operator + filters (for backward compatibility)
      if (input.getDefinition().getFilter().getOperator() != null) {
        filterBuilder.setOperator(input.getDefinition().getFilter().getOperator());
      }

      if (input.getDefinition().getFilter().getFilters() != null) {
        filterBuilder.setFilters(
            input.getDefinition().getFilter().getFilters().stream()
                .map(
                    filterInput ->
                        new FacetFilter(
                            filterInput.getField(),
                            filterInput.getCondition(),
                            filterInput.getValues(),
                            filterInput.getNegated()))
                .collect(Collectors.toList()));
      }
    }

    return new OrreryView.Builder()
        .setUrn(urn.toString())
        .setType(com.linkedin.orrery.graphql.generated.EntityType.ORRERY_VIEW)
        .setViewType(input.getViewType())
        .setName(input.getName())
        .setDescription(input.getDescription())
        .setDefinition(
            new OrreryViewDefinition(
                input.getDefinition().getEntityTypes(), filterBuilder.build()))
        .build();
  }
}
