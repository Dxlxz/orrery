package com.linkedin.orrery.graphql.types.rolemetadata.mappers;

import com.linkedin.common.RoleAssociationArray;
import com.linkedin.common.urn.Urn;
import com.linkedin.orrery.graphql.generated.EntityType;
import com.linkedin.orrery.graphql.generated.Role;
import com.linkedin.orrery.graphql.generated.RoleAssociation;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class AccessMapper {
  public static final AccessMapper INSTANCE = new AccessMapper();

  public static com.linkedin.orrery.graphql.generated.Access map(
      @Nonnull final com.linkedin.common.Access access, @Nonnull final Urn entityUrn) {
    return INSTANCE.apply(access, entityUrn);
  }

  public com.linkedin.orrery.graphql.generated.Access apply(
      @Nonnull final com.linkedin.common.Access access, @Nonnull final Urn entityUrn) {
    com.linkedin.orrery.graphql.generated.Access result =
        new com.linkedin.orrery.graphql.generated.Access();
    RoleAssociationArray roles =
        access.getRoles() != null ? access.getRoles() : new RoleAssociationArray();
    result.setRoles(
        roles.stream()
            .map(association -> this.mapRoleAssociation(association, entityUrn))
            .collect(Collectors.toList()));
    return result;
  }

  private RoleAssociation mapRoleAssociation(
      com.linkedin.common.RoleAssociation association, Urn entityUrn) {
    RoleAssociation roleAssociation = new RoleAssociation();
    Role role = new Role();
    role.setType(EntityType.ROLE);
    role.setUrn(association.getUrn().toString());
    roleAssociation.setRole(role);
    roleAssociation.setAssociatedUrn(entityUrn.toString());
    return roleAssociation;
  }
}
