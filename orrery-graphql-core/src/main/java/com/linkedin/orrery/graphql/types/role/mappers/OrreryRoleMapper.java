package com.linkedin.orrery.graphql.types.role.mappers;

import static com.linkedin.metadata.Constants.*;

import com.linkedin.data.DataMap;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.OrreryRole;
import com.linkedin.orrery.graphql.generated.EntityType;
import com.linkedin.orrery.graphql.types.common.mappers.util.MappingHelper;
import com.linkedin.orrery.graphql.types.mappers.ModelMapper;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.EnvelopedAspectMap;
import com.linkedin.policy.OrreryRoleInfo;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class OrreryRoleMapper implements ModelMapper<EntityResponse, OrreryRole> {

  public static final OrreryRoleMapper INSTANCE = new OrreryRoleMapper();

  public static OrreryRole map(
      @Nullable final QueryContext context, @Nonnull final EntityResponse entityResponse) {
    return INSTANCE.apply(context, entityResponse);
  }

  @Override
  public OrreryRole apply(
      @Nullable final QueryContext context, @Nonnull final EntityResponse entityResponse) {
    final OrreryRole result = new OrreryRole();

    result.setUrn(entityResponse.getUrn().toString());
    result.setType(EntityType.ORRERY_ROLE);
    EnvelopedAspectMap aspectMap = entityResponse.getAspects();
    MappingHelper<OrreryRole> mappingHelper = new MappingHelper<>(aspectMap, result);
    mappingHelper.mapToResult(ORRERY_ROLE_INFO_ASPECT_NAME, this::mapOrreryRoleInfo);
    // Guard against missing/incomplete aspect: GQL schema declares name/description as non-null.
    // A role entity can exist without a OrreryRoleInfo aspect (e.g. dangling roleMembership
    // references), which leaves getName() null and causes the entire query to fail.
    if (result.getName() == null) {
      result.setName(entityResponse.getUrn().getId());
    }
    if (result.getDescription() == null) {
      result.setDescription("");
    }
    return mappingHelper.getResult();
  }

  private void mapOrreryRoleInfo(@Nonnull OrreryRole role, @Nonnull DataMap dataMap) {
    OrreryRoleInfo roleInfo = new OrreryRoleInfo(dataMap);
    role.setName(roleInfo.getName());
    role.setDescription(roleInfo.getDescription());
  }
}
