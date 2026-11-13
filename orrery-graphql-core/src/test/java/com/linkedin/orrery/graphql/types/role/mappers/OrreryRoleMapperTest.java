package com.linkedin.orrery.graphql.types.role.mappers;

import static org.testng.Assert.*;

import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.orrery.graphql.generated.OrreryRole;
import com.linkedin.orrery.graphql.generated.EntityType;
import com.linkedin.entity.Aspect;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.EnvelopedAspect;
import com.linkedin.entity.EnvelopedAspectMap;
import com.linkedin.policy.OrreryRoleInfo;
import org.testng.annotations.Test;

public class OrreryRoleMapperTest {

  private static final String ORRERY_ROLE_INFO = "orreryRoleInfo";

  @Test
  public void testMapWithRoleInfo() {
    Urn urn = UrnUtils.getUrn("urn:li:orreryRole:Admin");

    OrreryRoleInfo info = new OrreryRoleInfo();
    info.setName("Admin");
    info.setDescription("Can do everything on the platform.");

    EntityResponse entityResponse = new EntityResponse();
    entityResponse.setUrn(urn);
    EnvelopedAspectMap aspectMap = new EnvelopedAspectMap();
    EnvelopedAspect aspect = new EnvelopedAspect();
    aspect.setValue(new Aspect(info.data()));
    aspectMap.put(ORRERY_ROLE_INFO, aspect);
    entityResponse.setAspects(aspectMap);

    OrreryRole result = OrreryRoleMapper.map(null, entityResponse);

    assertEquals(result.getUrn(), urn.toString());
    assertEquals(result.getType(), EntityType.ORRERY_ROLE);
    assertEquals(result.getName(), "Admin");
    assertEquals(result.getDescription(), "Can do everything on the platform.");
  }

  /**
   * A roleMembership can reference a OrreryRole URN that has no OrreryRoleInfo aspect - a
   * dangling reference, e.g. a sample_data_-prefixed role URN that was never defined.
   * OrreryRole.name and description are non-null in the GraphQL schema, so a null here propagates
   * and fails the entire query (this is what crashed the trial Users page, CAT-2446). The mapper
   * must fall back to a non-null value rather than leave them null. This test is the guard.
   */
  @Test
  public void testMapMissingRoleInfoFallsBackInsteadOfNull() {
    Urn urn = UrnUtils.getUrn("urn:li:orreryRole:sample_data_Admin");

    EntityResponse entityResponse = new EntityResponse();
    entityResponse.setUrn(urn);
    entityResponse.setAspects(new EnvelopedAspectMap()); // no orreryRoleInfo aspect

    OrreryRole result = OrreryRoleMapper.map(null, entityResponse);

    assertNotNull(
        result, "Mapper should return a role even when the OrreryRoleInfo aspect is absent");
    assertEquals(result.getUrn(), urn.toString());
    assertEquals(result.getType(), EntityType.ORRERY_ROLE);
    // The guard under test: non-null fallbacks, not null.
    assertNotNull(result.getName(), "name must not be null - it is a non-null GraphQL field");
    assertEquals(result.getName(), "sample_data_Admin", "name should fall back to the URN id");
    assertNotNull(
        result.getDescription(), "description must not be null - it is a non-null GraphQL field");
    assertEquals(result.getDescription(), "", "description should fall back to empty string");
  }
}
