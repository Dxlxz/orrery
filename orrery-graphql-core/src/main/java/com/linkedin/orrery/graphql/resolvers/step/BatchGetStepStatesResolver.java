package com.linkedin.orrery.graphql.resolvers.step;

import static com.linkedin.orrery.graphql.resolvers.ResolverUtils.*;
import static com.linkedin.metadata.Constants.*;
import static com.linkedin.metadata.utils.EntityKeyUtils.*;

import com.orrery.authentication.Authentication;
import com.google.common.collect.ImmutableSet;
import com.linkedin.common.urn.Urn;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.concurrency.GraphQLConcurrencyUtils;
import com.linkedin.orrery.graphql.generated.BatchGetStepStatesInput;
import com.linkedin.orrery.graphql.generated.BatchGetStepStatesResult;
import com.linkedin.orrery.graphql.generated.StepStateResult;
import com.linkedin.orrery.graphql.generated.StringMapEntry;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.EnvelopedAspectMap;
import com.linkedin.entity.client.EntityClient;
import com.linkedin.metadata.key.OrreryStepStateKey;
import com.linkedin.step.OrreryStepStateProperties;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class BatchGetStepStatesResolver
    implements DataFetcher<CompletableFuture<BatchGetStepStatesResult>> {
  private final EntityClient _entityClient;

  @Override
  public CompletableFuture<BatchGetStepStatesResult> get(
      @Nonnull final DataFetchingEnvironment environment) throws Exception {
    final QueryContext context = environment.getContext();
    final Authentication authentication = context.getAuthentication();
    final BatchGetStepStatesInput input =
        bindArgument(environment.getArgument("input"), BatchGetStepStatesInput.class);

    return GraphQLConcurrencyUtils.supplyAsync(
        () -> {
          Map<Urn, String> urnsToIdsMap;
          Set<Urn> urns;
          Map<Urn, EntityResponse> entityResponseMap;

          try {

            urnsToIdsMap = buildUrnToIdMap(input.getIds());
            urns = urnsToIdsMap.keySet();
            entityResponseMap =
                _entityClient.batchGetV2(
                    context.getOperationContext(),
                    ORRERY_STEP_STATE_ENTITY_NAME,
                    urns,
                    ImmutableSet.of(ORRERY_STEP_STATE_PROPERTIES_ASPECT_NAME));
          } catch (Exception e) {
            throw new RuntimeException(e);
          }

          final Map<Urn, OrreryStepStateProperties> stepStatePropertiesMap = new HashMap<>();
          for (Map.Entry<Urn, EntityResponse> entry : entityResponseMap.entrySet()) {
            final Urn urn = entry.getKey();
            final OrreryStepStateProperties stepStateProperties =
                getStepStateProperties(urn, entry.getValue());
            if (stepStateProperties != null) {
              stepStatePropertiesMap.put(urn, stepStateProperties);
            }
          }

          final List<StepStateResult> results =
              stepStatePropertiesMap.entrySet().stream()
                  .map(
                      entry ->
                          buildStepStateResult(urnsToIdsMap.get(entry.getKey()), entry.getValue()))
                  .collect(Collectors.toList());
          final BatchGetStepStatesResult result = new BatchGetStepStatesResult();
          result.setResults(results);
          return result;
        },
        this.getClass().getSimpleName(),
        "get");
  }

  /**
   * No existence check is needed here: {@code batchGetV2} omits the properties aspect for step
   * states that were never written, and those are filtered out downstream. Probing with {@code
   * exists} first would only add one query per id on top of the batch fetch.
   */
  @Nonnull
  private Map<Urn, String> buildUrnToIdMap(@Nonnull final List<String> ids) {
    final Map<Urn, String> urnToIdMap = new HashMap<>();
    for (final String id : ids) {
      urnToIdMap.put(getStepStateUrn(id), id);
    }

    return urnToIdMap;
  }

  @Nonnull
  private Urn getStepStateUrn(@Nonnull final String id) {
    final OrreryStepStateKey stepStateKey = new OrreryStepStateKey().setId(id);
    return convertEntityKeyToUrn(stepStateKey, ORRERY_STEP_STATE_ENTITY_NAME);
  }

  @Nullable
  private OrreryStepStateProperties getStepStateProperties(
      @Nonnull final Urn urn, @Nonnull final EntityResponse entityResponse) {
    final EnvelopedAspectMap aspectMap = entityResponse.getAspects();
    // A missing aspect just means the step was never completed, which is the expected state for any
    // step the user has not reached yet - not an error.
    if (!aspectMap.containsKey(ORRERY_STEP_STATE_PROPERTIES_ASPECT_NAME)) {
      log.debug("No step state properties for urn: {}", urn);
      return null;
    }
    return new OrreryStepStateProperties(
        aspectMap.get(ORRERY_STEP_STATE_PROPERTIES_ASPECT_NAME).getValue().data());
  }

  @Nonnull
  private StepStateResult buildStepStateResult(
      @Nonnull final String id, @Nonnull final OrreryStepStateProperties stepStateProperties) {
    final StepStateResult result = new StepStateResult();
    result.setId(id);
    final List<StringMapEntry> mappedProperties =
        stepStateProperties.getProperties().entrySet().stream()
            .map(entry -> buildStringMapEntry(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());
    result.setProperties(mappedProperties);
    return result;
  }

  @Nonnull
  private StringMapEntry buildStringMapEntry(
      @Nonnull final String key, @Nonnull final String value) {
    final StringMapEntry entry = new StringMapEntry();
    entry.setKey(key);
    entry.setValue(value);
    return entry;
  }
}
