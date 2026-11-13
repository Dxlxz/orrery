package com.linkedin.orrery.graphql.types.mappers;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import com.linkedin.common.AuditStamp;
import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.data.template.StringArray;
import com.linkedin.orrery.graphql.TestUtils;
import com.linkedin.orrery.graphql.generated.OrreryPolicy;
import com.linkedin.orrery.graphql.generated.PolicyState;
import com.linkedin.orrery.graphql.generated.PolicyType;
import com.linkedin.orrery.graphql.generated.SchemaFieldEntity;
import com.linkedin.orrery.graphql.types.policy.OrreryPolicyMapper;
import com.linkedin.orrery.graphql.types.schemafield.SchemaFieldMapper;
import com.linkedin.entity.Aspect;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.EnvelopedAspect;
import com.linkedin.entity.EnvelopedAspectMap;
import com.linkedin.metadata.Constants;
import com.linkedin.metadata.utils.TolerantHandling;
import com.linkedin.metadata.utils.UnknownDataHandlingsForTests;
import com.linkedin.policy.OrreryActorFilter;
import com.linkedin.policy.OrreryPolicyInfo;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Data a newer version wrote (read after a rollback) can hold enum values and references to entity
 * types this version doesn't know. Mappers fall back or leave those items out instead of failing
 * the entity, list or batch.
 */
public class RollbackToleranceMappersTest {

  private static final String NEWER_VALUE = "VALUE_FROM_NEWER_BUILD";
  private static final Urn DATASET =
      UrnUtils.getUrn("urn:li:dataset:(urn:li:dataPlatform:hive,db.t,PROD)");
  private static final Urn PLATFORM = UrnUtils.getUrn("urn:li:dataPlatform:hive");
  private static final Urn FROM_NEWER_BUILD = UrnUtils.getUrn("urn:li:entityFromNewerBuild:x");
  private static final AuditStamp STAMP =
      new AuditStamp().setTime(0L).setActor(UrnUtils.getUrn("urn:li:corpuser:u"));

  @BeforeMethod
  public void tolerateUnknownDataByDefault() {
    UnknownDataHandlingsForTests.use(TolerantHandling.INSTANCE);
  }

  @AfterMethod
  public void resetUnknownDataHandling() {
    UnknownDataHandlingsForTests.reset();
  }

  @Test
  public void testPolicyTypeAndStateFallBack() {
    OrreryPolicy unknown = mapPolicy(NEWER_VALUE, NEWER_VALUE);
    assertEquals(unknown.getPolicyType(), PolicyType.METADATA);
    assertEquals(unknown.getState(), PolicyState.INACTIVE);

    OrreryPolicy known = mapPolicy("PLATFORM", "ACTIVE");
    assertEquals(known.getPolicyType(), PolicyType.PLATFORM);
    assertEquals(known.getState(), PolicyState.ACTIVE);
  }

  @Test
  public void testSchemaFieldOfUnrepresentableParentKeepsTheField() {
    // A null field would null the non-null field or list that loaded it, so only parent is unset.
    EntityResponse response =
        new EntityResponse()
            .setUrn(UrnUtils.getUrn("urn:li:schemaField:(" + FROM_NEWER_BUILD + ",field)"))
            .setAspects(new EnvelopedAspectMap());

    SchemaFieldEntity field = SchemaFieldMapper.map(TestUtils.getMockAllowContext(), response);

    assertEquals(field.getFieldPath(), "field");
    assertNull(field.getParent());
  }

  @Test
  public void testSchemaFieldOfKnownParentIsMapped() {
    EntityResponse response =
        new EntityResponse()
            .setUrn(UrnUtils.getUrn("urn:li:schemaField:(" + DATASET + ",field)"))
            .setAspects(new EnvelopedAspectMap());

    assertEquals(
        SchemaFieldMapper.map(TestUtils.getMockAllowContext(), response).getFieldPath(), "field");
  }

  private static OrreryPolicy mapPolicy(String type, String state) {
    OrreryPolicyInfo info =
        new OrreryPolicyInfo()
            .setDisplayName("p")
            .setDescription("d")
            .setType("METADATA")
            .setState("ACTIVE")
            .setPrivileges(new StringArray())
            .setActors(new OrreryActorFilter())
            .setEditable(true);
    info.data().put("type", type);
    info.data().put("state", state);
    EnvelopedAspectMap aspects = new EnvelopedAspectMap();
    aspects.put(
        Constants.ORRERY_POLICY_INFO_ASPECT_NAME,
        new EnvelopedAspect().setValue(new Aspect(info.data())));
    return OrreryPolicyMapper.map(
        null,
        new EntityResponse().setUrn(UrnUtils.getUrn("urn:li:orreryPolicy:p")).setAspects(aspects));
  }
}
