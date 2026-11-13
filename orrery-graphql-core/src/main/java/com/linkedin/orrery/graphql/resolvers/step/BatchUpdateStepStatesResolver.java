package com.linkedin.orrery.graphql.resolvers.step;

import static com.linkedin.orrery.graphql.resolvers.ResolverUtils.*;
import static com.linkedin.metadata.Constants.*;
import static com.linkedin.metadata.entity.AspectUtils.*;

import com.orrery.authentication.Authentication;
import com.linkedin.common.AuditStamp;
import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.data.template.StringMap;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.concurrency.GraphQLConcurrencyUtils;
import com.linkedin.orrery.graphql.generated.BatchUpdateStepStatesInput;
import com.linkedin.orrery.graphql.generated.BatchUpdateStepStatesResult;
import com.linkedin.orrery.graphql.generated.StepStateInput;
import com.linkedin.orrery.graphql.generated.StringMapEntryInput;
import com.linkedin.orrery.graphql.generated.UpdateStepStateResult;
import com.linkedin.entity.client.EntityClient;
import com.linkedin.metadata.key.OrreryStepStateKey;
import com.linkedin.mxe.MetadataChangeProposal;
import com.linkedin.step.OrreryStepStateProperties;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.orreryproject.metadata.context.OperationContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class BatchUpdateStepStatesResolver
    implements DataFetcher<CompletableFuture<BatchUpdateStepStatesResult>> {
  private final EntityClient _entityClient;

  @Override
  public CompletableFuture<BatchUpdateStepStatesResult> get(
      @Nonnull final DataFetchingEnvironment environment) throws Exception {
    final QueryContext context = environment.getContext();
    final Authentication authentication = context.getAuthentication();

    final BatchUpdateStepStatesInput input =
        bindArgument(environment.getArgument("input"), BatchUpdateStepStatesInput.class);
    final List<StepStateInput> states = input.getStates();
    final String actorUrnStr = authentication.getActor().toUrnStr();

    return GraphQLConcurrencyUtils.supplyAsync(
        () -> {
          final Urn actorUrn = UrnUtils.getUrn(actorUrnStr);
          final AuditStamp auditStamp =
              new AuditStamp().setActor(actorUrn).setTime(System.currentTimeMillis());
          final BatchUpdateStepStatesResult result = new BatchUpdateStepStatesResult();
          result.setResults(updateStepStates(context.getOperationContext(), states, auditStamp));
          return result;
        },
        this.getClass().getSimpleName(),
        "get");
  }

  /**
   * Ingests every step state in a single batch so that all of them share one database transaction
   * instead of paying for a transaction (and its SELECT ... FOR UPDATE) per state.
   *
   * <p>The GraphQL contract reports success per id, but a failed batch does not identify which
   * state was at fault, so on failure we retry the states individually to recover that granularity.
   * The retry is safe because these are idempotent upserts.
   */
  @Nonnull
  private List<UpdateStepStateResult> updateStepStates(
      @Nonnull final OperationContext opContext,
      @Nonnull final List<StepStateInput> states,
      @Nonnull final AuditStamp auditStamp) {
    // Positionally aligned with `states`; null marks a state we could not convert to a proposal.
    final List<MetadataChangeProposal> proposals =
        states.stream()
            .map(state -> buildStepStateProposal(state, auditStamp))
            .collect(Collectors.toList());
    final List<MetadataChangeProposal> ingestable =
        proposals.stream().filter(Objects::nonNull).collect(Collectors.toList());

    boolean batchSucceeded = false;
    if (!ingestable.isEmpty()) {
      try {
        _entityClient.batchIngestProposals(opContext, ingestable, false);
        batchSucceeded = true;
      } catch (Exception e) {
        log.error(
            "Could not batch update {} step states, retrying them individually",
            ingestable.size(),
            e);
      }
    }

    final List<UpdateStepStateResult> results = new ArrayList<>(states.size());
    for (int i = 0; i < states.size(); i++) {
      final String id = states.get(i).getId();
      final MetadataChangeProposal proposal = proposals.get(i);
      final UpdateStepStateResult result = new UpdateStepStateResult();
      result.setId(id);
      result.setSucceeded(
          proposal != null && (batchSucceeded || ingestStepState(opContext, id, proposal)));
      results.add(result);
    }
    return results;
  }

  @Nullable
  private MetadataChangeProposal buildStepStateProposal(
      @Nonnull final StepStateInput state, @Nonnull final AuditStamp auditStamp) {
    try {
      final Map<String, String> properties =
          state.getProperties().stream()
              .collect(
                  Collectors.toMap(StringMapEntryInput::getKey, StringMapEntryInput::getValue));
      final OrreryStepStateKey stepStateKey = new OrreryStepStateKey().setId(state.getId());
      final OrreryStepStateProperties stepStateProperties =
          new OrreryStepStateProperties()
              .setProperties(new StringMap(properties))
              .setLastModified(auditStamp);
      return buildMetadataChangeProposal(
          ORRERY_STEP_STATE_ENTITY_NAME,
          stepStateKey,
          ORRERY_STEP_STATE_PROPERTIES_ASPECT_NAME,
          stepStateProperties);
    } catch (Exception e) {
      log.error("Could not build step state update for id {}", state.getId(), e);
      return null;
    }
  }

  private boolean ingestStepState(
      @Nonnull final OperationContext opContext,
      @Nonnull final String id,
      @Nonnull final MetadataChangeProposal proposal) {
    try {
      _entityClient.ingestProposal(opContext, proposal, false);
      return true;
    } catch (Exception e) {
      log.error("Could not update step state for id {}", id, e);
      return false;
    }
  }
}
