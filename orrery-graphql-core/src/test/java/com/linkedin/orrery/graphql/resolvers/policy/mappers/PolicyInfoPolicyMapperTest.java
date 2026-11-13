package com.linkedin.orrery.graphql.resolvers.policy.mappers;

import static com.linkedin.metadata.Constants.ORRERY_POLICY_INFO_ASPECT_NAME;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.expectThrows;

import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.data.DataList;
import com.linkedin.data.DataMap;
import com.linkedin.data.template.StringArray;
import com.linkedin.orrery.graphql.generated.OrreryPolicy;
import com.linkedin.orrery.graphql.generated.Policy;
import com.linkedin.orrery.graphql.generated.PolicyMatchCondition;
import com.linkedin.orrery.graphql.generated.PolicyState;
import com.linkedin.orrery.graphql.generated.PolicyType;
import com.linkedin.orrery.graphql.types.policy.OrreryPolicyMapper;
import com.linkedin.entity.Aspect;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.EnvelopedAspect;
import com.linkedin.entity.EnvelopedAspectMap;
import com.linkedin.metadata.utils.StrictHandling;
import com.linkedin.metadata.utils.TolerantHandling;
import com.linkedin.metadata.utils.UnknownDataHandlingsForTests;
import com.linkedin.policy.OrreryActorFilter;
import com.linkedin.policy.OrreryPolicyInfo;
import com.linkedin.policy.OrreryResourceFilter;
import com.linkedin.policy.PolicyMatchFilter;
import java.util.List;
import java.util.Map;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Guards against a single malformed policy taking down the entire {@code listPolicies} page.
 *
 * <p>{@link OrreryPolicyInfo#getType()} and {@link OrreryPolicyInfo#getState()} are free-form
 * strings in the metadata model, so a policy created via ingestion / the API can carry a value that
 * is not a member of the GraphQL {@link PolicyType} / {@link PolicyState} enums. An unguarded
 * {@code Enum.valueOf} throws {@link IllegalArgumentException}, which fails the whole request and
 * produces the UI's "Failed to load policies! An unexpected error occurred." on any page containing
 * such a policy.
 */
public class PolicyInfoPolicyMapperTest {

  @BeforeMethod
  public void tolerateUnknownDataByDefault() {
    UnknownDataHandlingsForTests.use(TolerantHandling.INSTANCE);
  }

  @AfterMethod
  public void resetUnknownDataHandling() {
    UnknownDataHandlingsForTests.reset();
  }

  private static OrreryPolicyInfo policyInfo(final String type, final String state) {
    final OrreryPolicyInfo info = new OrreryPolicyInfo();
    info.setDisplayName("My Policy");
    info.setDescription("desc");
    info.setType(type);
    info.setState(state);
    info.setPrivileges(new StringArray("EDIT_ENTITY_TAGS"));
    info.setActors(new OrreryActorFilter().setAllUsers(true).setAllGroups(false));
    info.setEditable(true);
    return info;
  }

  @Test
  public void testMapValidPolicy() {
    final Policy result = PolicyInfoPolicyMapper.map(null, policyInfo("METADATA", "ACTIVE"));
    assertEquals(result.getType(), PolicyType.METADATA);
    assertEquals(result.getState(), PolicyState.ACTIVE);
    assertEquals(result.getName(), "My Policy");
  }

  @Test
  public void testUnknownStateFallsBackToInactive() {
    // A state value not present in the GraphQL enum (e.g. ingested/backdoored data) must not throw.
    final Policy result = PolicyInfoPolicyMapper.map(null, policyInfo("METADATA", "ENABLED"));
    assertEquals(result.getState(), PolicyState.INACTIVE);
    // The rest of the policy still maps so it stays visible/manageable in the UI.
    assertEquals(result.getName(), "My Policy");
    assertEquals(result.getType(), PolicyType.METADATA);
  }

  @Test
  public void testUnknownTypeFallsBackToMetadata() {
    final Policy result = PolicyInfoPolicyMapper.map(null, policyInfo("SUPERUSER", "ACTIVE"));
    assertEquals(result.getType(), PolicyType.METADATA);
    assertEquals(result.getState(), PolicyState.ACTIVE);
  }

  private static OrreryPolicyInfo policyWithCondition(final String condition) {
    final DataMap criterion = new DataMap();
    criterion.put("field", "TYPE");
    criterion.put("values", new DataList(List.of("dataset")));
    criterion.put("condition", condition);
    final OrreryPolicyInfo info = policyInfo("METADATA", "ACTIVE");
    info.setResources(
        new OrreryResourceFilter()
            .setFilter(
                new PolicyMatchFilter(
                    new DataMap(Map.of("criteria", new DataList(List.of(criterion)))))));
    return info;
  }

  @Test
  public void testPolicyWithMatchConditionFromNewerVersionIsLeftOut() {
    // A guessed condition would be saved back by the policy editor and change what the policy
    // matches, so the policy is left out rather than shown with a different condition.
    assertTrue(
        PolicyInfoPolicyMapper.isUnrepresentable(
            policyWithCondition("CONDITION_FROM_NEWER_BUILD"), "urn:li:orreryPolicy:x"));

    final OrreryPolicyInfo known = policyWithCondition("STARTS_WITH");
    assertFalse(PolicyInfoPolicyMapper.isUnrepresentable(known, "urn:li:orreryPolicy:y"));
    assertEquals(
        PolicyInfoPolicyMapper.map(null, known)
            .getResources()
            .getFilter()
            .getCriteria()
            .get(0)
            .getCondition(),
        PolicyMatchCondition.STARTS_WITH);
  }

  @Test
  public void testEntityLoadOfPolicyWithMatchConditionFromNewerVersionKeepsThePolicy() {
    // Read-only path (entity, search): a null policy would null the field or list that loaded it,
    // so only the unknown condition is unset. The editor's list still leaves the policy out.
    final Urn urn = UrnUtils.getUrn("urn:li:orreryPolicy:x");
    final EntityResponse response =
        new EntityResponse()
            .setUrn(urn)
            .setEntityName("orreryPolicy")
            .setAspects(
                new EnvelopedAspectMap(
                    Map.of(
                        ORRERY_POLICY_INFO_ASPECT_NAME,
                        new EnvelopedAspect()
                            .setValue(
                                new Aspect(
                                    policyWithCondition("CONDITION_FROM_NEWER_BUILD").data())))));

    final OrreryPolicy policy = OrreryPolicyMapper.map(null, response);

    assertEquals(policy.getUrn(), urn.toString());
    assertNull(policy.getResources().getFilter().getCriteria().get(0).getCondition());
  }

  @Test
  public void testStrictHandlingFailsOnMatchConditionFromNewerVersion() {
    UnknownDataHandlingsForTests.use(StrictHandling.INSTANCE);
    try {
      final OrreryPolicyInfo info = policyWithCondition("CONDITION_FROM_NEWER_BUILD");
      assertFalse(PolicyInfoPolicyMapper.isUnrepresentable(info, "urn:li:orreryPolicy:x"));
      expectThrows(IllegalArgumentException.class, () -> PolicyInfoPolicyMapper.map(null, info));
    } finally {
      UnknownDataHandlingsForTests.reset();
    }
  }
}
