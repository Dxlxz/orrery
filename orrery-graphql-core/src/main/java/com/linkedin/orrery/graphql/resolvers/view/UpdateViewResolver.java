package com.linkedin.orrery.graphql.resolvers.view;

import static com.linkedin.orrery.graphql.resolvers.ResolverUtils.*;

import com.orrery.authentication.Authentication;
import com.linkedin.common.urn.Urn;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.concurrency.GraphQLConcurrencyUtils;
import com.linkedin.orrery.graphql.exception.AuthorizationException;
import com.linkedin.orrery.graphql.generated.OrreryView;
import com.linkedin.orrery.graphql.generated.UpdateViewInput;
import com.linkedin.orrery.graphql.types.view.OrreryViewMapper;
import com.linkedin.entity.EntityResponse;
import com.linkedin.metadata.service.ViewService;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

/** Resolver responsible for updating a particular Orrery View */
@Slf4j
public class UpdateViewResolver implements DataFetcher<CompletableFuture<OrreryView>> {

  private final ViewService _viewService;

  public UpdateViewResolver(@Nonnull final ViewService viewService) {
    _viewService = Objects.requireNonNull(viewService, "viewService must not be null");
  }

  @Override
  public CompletableFuture<OrreryView> get(final DataFetchingEnvironment environment)
      throws Exception {
    final QueryContext context = environment.getContext();
    final String urnStr = environment.getArgument("urn");
    final UpdateViewInput input =
        bindArgument(environment.getArgument("input"), UpdateViewInput.class);

    final Urn urn = Urn.createFromString(urnStr);
    return GraphQLConcurrencyUtils.supplyAsync(
        () -> {
          try {
            if (ViewUtils.canUpdateView(_viewService, urn, context)) {
              _viewService.updateView(
                  context.getOperationContext(),
                  urn,
                  input.getName(),
                  input.getDescription(),
                  ViewUtils.mapDefinition(
                      input.getDefinition(), context.getOperationContext().getAspectRetriever()),
                  System.currentTimeMillis());
              log.info(String.format("Successfully updated View %s with urn", urn));
              return getView(context, urn, context.getAuthentication());
            }
            throw new AuthorizationException(
                "Unauthorized to perform this action. Please contact your Orrery administrator.");
          } catch (AuthorizationException e) {
            throw e;
          } catch (Exception e) {
            throw new RuntimeException(
                String.format("Failed to perform update against View with urn %s", urn), e);
          }
        },
        this.getClass().getSimpleName(),
        "get");
  }

  private OrreryView getView(
      @Nullable QueryContext context,
      @Nonnull final Urn urn,
      @Nonnull final Authentication authentication) {
    final EntityResponse maybeResponse =
        _viewService.getViewEntityResponse(context.getOperationContext(), urn);
    // If there is no response, there is a problem.
    if (maybeResponse == null) {
      throw new RuntimeException(
          String.format(
              "Failed to perform update to View with urn %s. Failed to find view in GMS.", urn));
    }
    return OrreryViewMapper.map(context, maybeResponse);
  }
}
