package com.linkedin.orrery.graphql.resolvers.assertion;

import com.orrery.authorization.ConjunctivePrivilegeGroup;
import com.orrery.authorization.DisjunctivePrivilegeGroup;
import com.google.common.collect.ImmutableList;
import com.linkedin.common.urn.Urn;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.authorization.AuthorizationUtils;
import com.linkedin.metadata.authorization.PoliciesConfig;

public class AssertionUtils {
  public static boolean isAuthorizedToEditAssertionFromAssertee(
      final QueryContext context, final Urn asserteeUrn) {
    final DisjunctivePrivilegeGroup orPrivilegeGroups =
        new DisjunctivePrivilegeGroup(
            ImmutableList.of(
                AuthorizationUtils.ALL_PRIVILEGES_GROUP,
                new ConjunctivePrivilegeGroup(
                    ImmutableList.of(PoliciesConfig.EDIT_ENTITY_ASSERTIONS_PRIVILEGE.getType()))));
    return AuthorizationUtils.isAuthorized(
        context, asserteeUrn.getEntityType(), asserteeUrn.toString(), orPrivilegeGroups);
  }
}
