package com.linkedin.orrery.graphql.resolvers.domain;

import com.linkedin.common.urn.Urn;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.authorization.AuthorizationUtils;
import com.linkedin.orrery.graphql.concurrency.GraphQLConcurrencyUtils;
import com.linkedin.orrery.graphql.exception.AuthorizationException;
import com.linkedin.orrery.graphql.resolvers.mutate.util.DomainUtils;
import com.linkedin.entity.client.EntityClient;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;

/** Resolver responsible for hard deleting a particular Orrery Corp Group */
@Slf4j
public class DeleteDomainResolver implements DataFetcher<CompletableFuture<Boolean>> {

  private final EntityClient _entityClient;

  public DeleteDomainResolver(final EntityClient entityClient) {
    _entityClient = entityClient;
  }

  @Override
  public CompletableFuture<Boolean> get(final DataFetchingEnvironment environment)
      throws Exception {
    final QueryContext context = environment.getContext();
    final String domainUrn = environment.getArgument("urn");
    final Urn urn = Urn.createFromString(domainUrn);
    return GraphQLConcurrencyUtils.supplyAsync(
        () -> {
          if (AuthorizationUtils.canManageDomains(context)
              || AuthorizationUtils.canDeleteEntity(urn, context)) {
            try {
              // Make sure there are no child domains
              if (DomainUtils.hasChildDomains(urn, context, _entityClient)) {
                throw new RuntimeException(
                    String.format("Cannot delete domain %s which has child domains", domainUrn));
              }

              final var references =
                  _entityClient.deleteEntityThenReferences(context.getOperationContext(), urn);
              log.info(
                  String.format("I've successfully deleted the entity %s with urn", domainUrn));

              // Asynchronously Delete all references to the entity (to return quickly)
              CompletableFuture.runAsync(
                  () -> {
                    try {
                      references.run();
                    } catch (Exception e) {
                      log.error(
                          String.format(
                              "Caught exception while attempting to clear all entity references for Domain with urn %s",
                              urn),
                          e);
                    }
                  });

              return true;
            } catch (Exception e) {
              throw new RuntimeException(
                  String.format("Failed to perform delete against domain with urn %s", domainUrn),
                  e);
            }
          }
          throw new AuthorizationException(
              "Unauthorized to perform this action. Please contact your Orrery administrator.");
        },
        this.getClass().getSimpleName(),
        "get");
  }
}
