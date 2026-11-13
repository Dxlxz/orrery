package com.linkedin.orrery.graphql.resolvers.ingest.secret;

import static com.linkedin.orrery.graphql.resolvers.ResolverUtils.*;
import static com.linkedin.orrery.graphql.resolvers.mutate.MutationUtils.*;
import static com.linkedin.metadata.Constants.*;

import com.linkedin.common.AuditStamp;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.authorization.AuthorizationUtils;
import com.linkedin.orrery.graphql.concurrency.GraphQLConcurrencyUtils;
import com.linkedin.orrery.graphql.exception.AuthorizationException;
import com.linkedin.orrery.graphql.generated.CreateSecretInput;
import com.linkedin.orrery.graphql.types.ingest.secret.mapper.OrrerySecretValueMapper;
import com.linkedin.entity.client.EntityClient;
import com.linkedin.metadata.key.OrrerySecretKey;
import com.linkedin.metadata.utils.EntityKeyUtils;
import com.linkedin.mxe.MetadataChangeProposal;
import com.linkedin.secret.OrrerySecretValue;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.orreryproject.metadata.context.ReadPreference;
import io.orreryproject.metadata.services.SecretService;
import java.util.concurrent.CompletableFuture;

/**
 * Creates an encrypted Orrery secret. Uses AES symmetric encryption / decryption. Requires the
 * MANAGE_SECRETS privilege.
 */
public class CreateSecretResolver implements DataFetcher<CompletableFuture<String>> {

  private final EntityClient _entityClient;
  private final SecretService _secretService;

  public CreateSecretResolver(final EntityClient entityClient, final SecretService secretService) {
    _entityClient = entityClient;
    _secretService = secretService;
  }

  @Override
  public CompletableFuture<String> get(final DataFetchingEnvironment environment) throws Exception {
    final QueryContext context = environment.getContext();
    final CreateSecretInput input =
        bindArgument(environment.getArgument("input"), CreateSecretInput.class);

    return GraphQLConcurrencyUtils.supplyAsync(
        () -> {
          if (AuthorizationUtils.canManageSecrets(context)) {

            try {
              // Create the Ingestion source key --> use the display name as a unique id to ensure
              // it's not duplicated.
              final OrrerySecretKey key = new OrrerySecretKey();
              key.setId(input.getName());

              if (_entityClient.exists(
                  context.getOperationContext().withReadPreference(ReadPreference.PRIMARY),
                  EntityKeyUtils.convertEntityKeyToUrn(key, SECRETS_ENTITY_NAME))) {
                throw new IllegalArgumentException("This Secret already exists!");
              }

              // Create the secret value.
              final OrrerySecretValue value =
                  OrrerySecretValueMapper.map(
                      null,
                      input.getName(),
                      _secretService.encrypt(context.getOperationContext(), input.getValue()),
                      input.getDescription(),
                      new AuditStamp()
                          .setActor(UrnUtils.getUrn(context.getActorUrn()))
                          .setTime(System.currentTimeMillis()));

              final MetadataChangeProposal proposal =
                  buildMetadataChangeProposalWithKey(
                      key, SECRETS_ENTITY_NAME, SECRET_VALUE_ASPECT_NAME, value);
              return _entityClient.ingestProposal(context.getOperationContext(), proposal, false);
            } catch (Exception e) {
              throw new RuntimeException(
                  String.format("Failed to create new secret with name %s", input.getName()), e);
            }
          }
          throw new AuthorizationException(
              "Unauthorized to perform this action. Please contact your Orrery administrator.");
        },
        this.getClass().getSimpleName(),
        "get");
  }
}
