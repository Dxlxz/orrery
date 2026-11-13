package com.linkedin.orrery.graphql.types.policy;

import static com.linkedin.metadata.Constants.*;
import static com.linkedin.metadata.Constants.ORRERY_POLICY_KEY_ASPECT_NAME;
import static com.linkedin.metadata.utils.UnknownDataHandlings.handling;

import com.google.common.collect.ImmutableSet;
import com.linkedin.common.urn.Urn;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.OrreryPolicy;
import com.linkedin.orrery.graphql.generated.Entity;
import com.linkedin.orrery.graphql.generated.EntityType;
import com.linkedin.orrery.graphql.resolvers.policy.mappers.PolicyInfoPolicyMapper;
import com.linkedin.orrery.graphql.util.AspectUtils;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.EnvelopedAspect;
import com.linkedin.entity.client.EntityClient;
import com.linkedin.policy.OrreryPolicyInfo;
import graphql.execution.DataFetcherResult;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OrreryPolicyType
    implements com.linkedin.orrery.graphql.types.EntityType<OrreryPolicy, String> {
  static final Set<String> ASPECTS_TO_FETCH = ImmutableSet.of(ORRERY_POLICY_INFO_ASPECT_NAME);
  private final EntityClient _entityClient;

  @Override
  public EntityType type() {
    return EntityType.ORRERY_POLICY;
  }

  @Override
  public Function<Entity, String> getKeyProvider() {
    return Entity::getUrn;
  }

  @Override
  public Class<OrreryPolicy> objectClass() {
    return OrreryPolicy.class;
  }

  @Override
  public List<DataFetcherResult<OrreryPolicy>> batchLoad(
      @Nonnull List<String> urns, @Nonnull QueryContext context) throws Exception {
    final List<Urn> roleUrns = urns.stream().map(this::getUrn).collect(Collectors.toList());

    try {
      // Determine optimal aspects to fetch based on GraphQL field selections
      // While unknown enum values are tolerated, the policy's info is always read, so a policy
      // with a match condition this version doesn't know is left out whatever the query selects.
      Set<String> aspectsToResolve =
          handling().toleratesUnknownEnum()
              ? AspectUtils.getOptimizedAspects(
                  context,
                  name(),
                  ASPECTS_TO_FETCH,
                  ORRERY_POLICY_KEY_ASPECT_NAME,
                  ORRERY_POLICY_INFO_ASPECT_NAME)
              : AspectUtils.getOptimizedAspects(
                  context, name(), ASPECTS_TO_FETCH, ORRERY_POLICY_KEY_ASPECT_NAME);
      final Map<Urn, EntityResponse> entities =
          new HashMap<>(
              _entityClient.batchGetV2(
                  context.getOperationContext(),
                  POLICY_ENTITY_NAME,
                  new HashSet<>(roleUrns),
                  aspectsToResolve));
      // PolicyMatchCriterion.condition is non-null: a policy with a condition this version
      // doesn't know is returned as missing, as listPolicies leaves it out.
      entities.values().removeIf(OrreryPolicyType::isUnrepresentable);

      final List<EntityResponse> gmsResults = new ArrayList<>();
      for (Urn urn : roleUrns) {
        gmsResults.add(entities.getOrDefault(urn, null));
      }
      return gmsResults.stream()
          .map(
              gmsResult ->
                  gmsResult == null
                      ? null
                      : DataFetcherResult.<OrreryPolicy>newResult()
                          .data(OrreryPolicyMapper.map(context, gmsResult))
                          .build())
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new RuntimeException("Failed to batch load Roles", e);
    }
  }

  private Urn getUrn(final String urnStr) {
    try {
      return Urn.createFromString(urnStr);
    } catch (URISyntaxException e) {
      throw new RuntimeException(String.format("Failed to convert urn string %s into Urn", urnStr));
    }
  }

  /**
   * Whether the response holds a policy GraphQL can't show (see {@link PolicyInfoPolicyMapper}).
   */
  private static boolean isUnrepresentable(@Nonnull final EntityResponse response) {
    final EnvelopedAspect info = response.getAspects().get(ORRERY_POLICY_INFO_ASPECT_NAME);
    return info != null
        && PolicyInfoPolicyMapper.isUnrepresentable(
            new OrreryPolicyInfo(info.getValue().data()), response.getUrn());
  }
}
