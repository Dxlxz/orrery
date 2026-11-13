package com.linkedin.orrery.graphql.resolvers.connection;

import static com.linkedin.orrery.graphql.resolvers.ResolverUtils.*;

import com.orrery.authentication.Authentication;
import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.connection.OrreryConnectionDetailsType;
import com.linkedin.connection.OrreryJsonConnection;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.concurrency.GraphQLConcurrencyUtils;
import com.linkedin.orrery.graphql.exception.AuthorizationException;
import com.linkedin.orrery.graphql.generated.OrreryConnection;
import com.linkedin.orrery.graphql.generated.UpsertOrreryConnectionInput;
import com.linkedin.entity.EntityResponse;
import com.linkedin.metadata.connection.ConnectionService;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.orreryproject.metadata.services.SecretService;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class UpsertConnectionResolver implements DataFetcher<CompletableFuture<OrreryConnection>> {

  private final ConnectionService _connectionService;
  private final SecretService _secretService;

  public UpsertConnectionResolver(
      @Nonnull final ConnectionService connectionService,
      @Nonnull final SecretService secretService) {
    _connectionService =
        Objects.requireNonNull(connectionService, "connectionService cannot be null");
    _secretService = Objects.requireNonNull(secretService, "secretService cannot be null");
  }

  @Override
  public CompletableFuture<OrreryConnection> get(final DataFetchingEnvironment environment)
      throws Exception {

    final QueryContext context = environment.getContext();
    final UpsertOrreryConnectionInput input =
        bindArgument(environment.getArgument("input"), UpsertOrreryConnectionInput.class);
    final Authentication authentication = context.getAuthentication();

    return GraphQLConcurrencyUtils.supplyAsync(
        () -> {
          if (!ConnectionUtils.canManageConnections(context)) {
            throw new AuthorizationException(
                "Unauthorized to upsert Connection. Please contact your Orrery administrator for more information.");
          }

          try {
            final Urn connectionUrn =
                _connectionService.upsertConnection(
                    context.getOperationContext(),
                    input.getId(),
                    UrnUtils.getUrn(input.getPlatformUrn()),
                    OrreryConnectionDetailsType.valueOf(input.getType().toString()),
                    input.getJson() != null
                        // Encrypt payload
                        ? new OrreryJsonConnection()
                            .setEncryptedBlob(
                                _secretService.encrypt(
                                    context.getOperationContext(), input.getJson().getBlob()))
                        : null,
                    input.getName());

            final EntityResponse connectionResponse =
                _connectionService.getConnectionEntityResponse(
                    context.getOperationContext(), connectionUrn);
            return ConnectionMapper.map(context, connectionResponse, _secretService);
          } catch (Exception e) {
            throw new RuntimeException(
                String.format("Failed to upsert a Connection from input %s", input), e);
          }
        },
        this.getClass().getSimpleName(),
        "get");
  }
}
