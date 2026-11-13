package com.linkedin.orrery.graphql.types.policy;

import static com.linkedin.metadata.Constants.*;

import com.linkedin.common.UrnArray;
import com.linkedin.common.urn.Urn;
import com.linkedin.data.DataMap;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.ActorFilter;
import com.linkedin.orrery.graphql.generated.OrreryPolicy;
import com.linkedin.orrery.graphql.generated.EntityType;
import com.linkedin.orrery.graphql.generated.PolicyMatchCondition;
import com.linkedin.orrery.graphql.generated.PolicyMatchCriterion;
import com.linkedin.orrery.graphql.generated.PolicyMatchCriterionValue;
import com.linkedin.orrery.graphql.generated.PolicyMatchFilter;
import com.linkedin.orrery.graphql.generated.PolicyState;
import com.linkedin.orrery.graphql.generated.PolicyType;
import com.linkedin.orrery.graphql.generated.ResourceFilter;
import com.linkedin.orrery.graphql.generated.StructuredPropertyCriterionValue;
import com.linkedin.orrery.graphql.types.common.mappers.UrnToEntityMapper;
import com.linkedin.orrery.graphql.types.common.mappers.util.MappingHelper;
import com.linkedin.orrery.graphql.types.common.mappers.util.UnknownEnumValues;
import com.linkedin.orrery.graphql.types.mappers.ModelMapper;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.EnvelopedAspectMap;
import com.linkedin.policy.OrreryActorFilter;
import com.linkedin.policy.OrreryPolicyInfo;
import com.linkedin.policy.OrreryResourceFilter;
import java.net.URISyntaxException;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class OrreryPolicyMapper implements ModelMapper<EntityResponse, OrreryPolicy> {

  public static final OrreryPolicyMapper INSTANCE = new OrreryPolicyMapper();

  public static OrreryPolicy map(
      @Nullable QueryContext context, @Nonnull final EntityResponse entityResponse) {
    return INSTANCE.apply(context, entityResponse);
  }

  @Override
  public OrreryPolicy apply(
      @Nullable QueryContext context, @Nonnull final EntityResponse entityResponse) {
    final OrreryPolicy result = new OrreryPolicy();

    result.setUrn(entityResponse.getUrn().toString());
    result.setType(EntityType.ORRERY_POLICY);
    EnvelopedAspectMap aspectMap = entityResponse.getAspects();
    MappingHelper<OrreryPolicy> mappingHelper = new MappingHelper<>(aspectMap, result);
    mappingHelper.mapToResult(context, ORRERY_POLICY_INFO_ASPECT_NAME, this::mapOrreryPolicyInfo);
    return mappingHelper.getResult();
  }

  private void mapOrreryPolicyInfo(
      @Nullable QueryContext context, @Nonnull OrreryPolicy policy, @Nonnull DataMap dataMap) {
    OrreryPolicyInfo policyInfo = new OrreryPolicyInfo(dataMap);
    policy.setDescription(policyInfo.getDescription());
    // Type/state are free-form strings; while unknown enum values are tolerated, a value this
    // version doesn't know (ingested via the API, or written by a newer version before a rollback)
    // falls back to the same values PolicyInfoPolicyMapper uses. Otherwise it fails as before.
    policy.setPolicyType(
        UnknownEnumValues.fromString(PolicyType.class, policyInfo.getType(), PolicyType.METADATA));
    policy.setState(
        UnknownEnumValues.fromString(
            PolicyState.class, policyInfo.getState(), PolicyState.INACTIVE));
    policy.setName(policyInfo.getDisplayName()); // Rebrand to 'name'
    policy.setPrivileges(policyInfo.getPrivileges());
    policy.setActors(mapActors(policyInfo.getActors()));
    policy.setEditable(policyInfo.isEditable());
    if (policyInfo.hasResources()) {
      policy.setResources(mapResources(context, policyInfo.getResources()));
    }
  }

  private ActorFilter mapActors(final OrreryActorFilter actorFilter) {
    final ActorFilter result = new ActorFilter();
    result.setAllGroups(actorFilter.isAllGroups());
    result.setAllUsers(actorFilter.isAllUsers());
    result.setResourceOwners(actorFilter.isResourceOwners());
    // Change here is not executed at the moment - leaving it for the future
    UrnArray resourceOwnersTypes = actorFilter.getResourceOwnersTypes();
    if (resourceOwnersTypes != null) {
      result.setResourceOwnersTypes(
          resourceOwnersTypes.stream().map(Urn::toString).collect(Collectors.toList()));
    }
    if (actorFilter.hasGroups()) {
      result.setGroups(
          actorFilter.getGroups().stream().map(Urn::toString).collect(Collectors.toList()));
    }
    if (actorFilter.hasUsers()) {
      result.setUsers(
          actorFilter.getUsers().stream().map(Urn::toString).collect(Collectors.toList()));
    }
    if (actorFilter.hasRoles()) {
      result.setRoles(
          actorFilter.getRoles().stream().map(Urn::toString).collect(Collectors.toList()));
    }
    return result;
  }

  private ResourceFilter mapResources(
      @Nullable QueryContext context, final OrreryResourceFilter resourceFilter) {
    final ResourceFilter result = new ResourceFilter();
    result.setAllResources(resourceFilter.isAllResources());
    if (resourceFilter.hasType()) {
      result.setType(resourceFilter.getType());
    }
    if (resourceFilter.hasResources()) {
      result.setResources(resourceFilter.getResources());
    }
    if (resourceFilter.hasFilter()) {
      result.setFilter(mapFilter(context, resourceFilter.getFilter()));
    }
    return result;
  }

  private PolicyMatchFilter mapFilter(
      @Nullable QueryContext context, final com.linkedin.policy.PolicyMatchFilter filter) {
    return PolicyMatchFilter.builder()
        .setCriteria(
            filter.getCriteria().stream()
                .map(
                    criterion -> {
                      boolean hasStructuredProps = criterion.hasStructuredPropertyValues();
                      return PolicyMatchCriterion.builder()
                          .setField(criterion.getField())
                          .setValues(
                              criterion.getValues().stream()
                                  .map(c -> mapValue(context, c))
                                  .collect(Collectors.toList()))
                          .setCondition(
                              UnknownEnumValues.mapOrNull(
                                  PolicyMatchCondition.class, criterion.getCondition()))
                          .setStructuredPropertyValues(
                              hasStructuredProps
                                  ? criterion.getStructuredPropertyValues().stream()
                                      .map(this::mapStructuredPropertyValue)
                                      .collect(Collectors.toList())
                                  : null)
                          .build();
                    })
                .collect(Collectors.toList()))
        .build();
  }

  private PolicyMatchCriterionValue mapValue(@Nullable QueryContext context, final String value) {
    try {
      // If value is urn, set entity field
      Urn urn = Urn.createFromString(value);
      return PolicyMatchCriterionValue.builder()
          .setValue(value)
          .setEntity(UrnToEntityMapper.map(context, urn))
          .build();
    } catch (URISyntaxException e) {
      // Value is not an urn. Just set value
      return PolicyMatchCriterionValue.builder().setValue(value).build();
    }
  }

  private StructuredPropertyCriterionValue mapStructuredPropertyValue(
      final com.linkedin.policy.StructuredPropertyCriterionValue value) {
    final StructuredPropertyCriterionValue result = new StructuredPropertyCriterionValue();
    result.setPropertyUrn(value.getPropertyUrn().toString());
    result.setValues(value.getValues());
    return result;
  }
}
