package com.linkedin.orrery.graphql.resolvers.incident;

import static com.linkedin.orrery.graphql.resolvers.ResolverUtils.bindArgument;
import static com.linkedin.metadata.Constants.INCIDENT_INFO_ASPECT_NAME;

import com.linkedin.common.AuditStamp;
import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.concurrency.GraphQLConcurrencyUtils;
import com.linkedin.orrery.graphql.exception.OrreryGraphQLErrorCode;
import com.linkedin.orrery.graphql.exception.OrreryGraphQLException;
import com.linkedin.orrery.graphql.generated.UpsertIncidentInput;
import com.linkedin.incident.IncidentInfo;
import com.linkedin.metadata.entity.EntityService;
import com.linkedin.metadata.entity.EntityUtils;
import com.linkedin.metadata.service.IncidentInfoUpsert;
import com.linkedin.metadata.service.IncidentService;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;

/** Applies the editor's complete field snapshot to an incident as a JSON Patch. */
@RequiredArgsConstructor
public class UpsertIncidentResolver implements DataFetcher<CompletableFuture<Boolean>> {

  private final IncidentService _incidentService;
  private final EntityService _entityService;

  @Override
  public CompletableFuture<Boolean> get(final DataFetchingEnvironment environment)
      throws Exception {
    final QueryContext context = environment.getContext();
    final Urn incidentUrn = Urn.createFromString(environment.getArgument("urn"));
    final UpsertIncidentInput input =
        bindArgument(environment.getArgument("input"), UpsertIncidentInput.class);

    return GraphQLConcurrencyUtils.supplyAsync(
        () -> {
          final IncidentInfo info =
              (IncidentInfo)
                  EntityUtils.getAspectFromEntity(
                      context.getOperationContext(),
                      incidentUrn.toString(),
                      INCIDENT_INFO_ASPECT_NAME,
                      _entityService,
                      null);
          if (info == null) {
            throw new OrreryGraphQLException(
                "Failed to upsert incident. Incident does not exist.",
                OrreryGraphQLErrorCode.NOT_FOUND);
          }

          IncidentUtils.verifyAuthorizationOrThrow(context, info, input.getResourceUrns());
          AuditStamp actorStamp =
              new AuditStamp()
                  .setActor(UrnUtils.getUrn(context.getActorUrn()))
                  .setTime(System.currentTimeMillis());
          IncidentInfoUpsert upsert = IncidentUtils.mapIncidentUpsert(input, actorStamp);
          try {
            _incidentService.upsertIncident(context.getOperationContext(), incidentUrn, upsert);
            return true;
          } catch (Exception e) {
            throw new RuntimeException("Failed to upsert incident!", e);
          }
        },
        this.getClass().getSimpleName(),
        "get");
  }
}
