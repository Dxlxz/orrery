package com.linkedin.orrery.graphql.resolvers.ingest.source;

import com.linkedin.common.urn.Urn;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.authorization.AuthorizationUtils;
import com.linkedin.orrery.graphql.concurrency.GraphQLConcurrencyUtils;
import com.linkedin.orrery.graphql.exception.AuthorizationException;
import com.linkedin.entity.client.EntityClient;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.concurrent.CompletableFuture;

/**
 * Resolver responsible for hard deleting a particular Orrery Ingestion Source. Requires
 * MANAGE_INGESTION privilege.
 */
public class DeleteIngestionSourceResolver implements DataFetcher<CompletableFuture<String>> {

  private final EntityClient _entityClient;

  public DeleteIngestionSourceResolver(final EntityClient entityClient) {
    _entityClient = entityClient;
  }

  @Override
  public CompletableFuture<String> get(final DataFetchingEnvironment environment) throws Exception {
    final QueryContext context = environment.getContext();
    if (AuthorizationUtils.canManageIngestion(context)) {
      final String ingestionSourceUrn = environment.getArgument("urn");
      final Urn urn = Urn.createFromString(ingestionSourceUrn);
      return GraphQLConcurrencyUtils.supplyAsync(
          () -> {
            try {
              _entityClient.deleteEntity(context.getOperationContext(), urn);
              return ingestionSourceUrn;
            } catch (Exception e) {
              throw new RuntimeException(
                  String.format(
                      "Failed to perform delete against ingestion source with urn %s",
                      ingestionSourceUrn),
                  e);
            }
          },
          this.getClass().getSimpleName(),
          "get");
    }
    throw new AuthorizationException(
        "Unauthorized to perform this action. Please contact your Orrery administrator.");
  }
}
