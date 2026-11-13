package com.linkedin.orrery.graphql.resolvers.businessattribute;

import static com.linkedin.orrery.graphql.authorization.AuthorizationUtils.ALL_PRIVILEGES_GROUP;

import com.orrery.authorization.AuthUtil;
import com.orrery.authorization.ConjunctivePrivilegeGroup;
import com.orrery.authorization.DisjunctivePrivilegeGroup;
import com.google.common.collect.ImmutableList;
import com.linkedin.common.urn.Urn;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.authorization.AuthorizationUtils;
import com.linkedin.metadata.authorization.PoliciesConfig;
import java.net.URISyntaxException;
import javax.annotation.Nonnull;

public class BusinessAttributeAuthorizationUtils {
  private BusinessAttributeAuthorizationUtils() {}

  public static boolean canCreateBusinessAttribute(@Nonnull QueryContext context) {
    final DisjunctivePrivilegeGroup orPrivilegeGroups =
        new DisjunctivePrivilegeGroup(
            ImmutableList.of(
                new ConjunctivePrivilegeGroup(
                    ImmutableList.of(PoliciesConfig.CREATE_BUSINESS_ATTRIBUTE_PRIVILEGE.getType())),
                new ConjunctivePrivilegeGroup(
                    ImmutableList.of(
                        PoliciesConfig.MANAGE_BUSINESS_ATTRIBUTE_PRIVILEGE.getType()))));
    return AuthUtil.isAuthorized(context.getOperationContext(), orPrivilegeGroups, null);
  }

  public static boolean canManageBusinessAttribute(@Nonnull QueryContext context) {
    final DisjunctivePrivilegeGroup orPrivilegeGroups =
        new DisjunctivePrivilegeGroup(
            ImmutableList.of(
                new ConjunctivePrivilegeGroup(
                    ImmutableList.of(
                        PoliciesConfig.MANAGE_BUSINESS_ATTRIBUTE_PRIVILEGE.getType()))));
    return AuthUtil.isAuthorized(context.getOperationContext(), orPrivilegeGroups, null);
  }

  public static boolean isAuthorizedToEditBusinessAttribute(
      @Nonnull QueryContext context, String targetUrn) throws URISyntaxException {
    Urn schemaFieldUrn = Urn.createFromString(targetUrn);
    Urn datasetUrn = schemaFieldUrn.getIdAsUrn();
    final DisjunctivePrivilegeGroup orPrivilegeGroups =
        new DisjunctivePrivilegeGroup(
            ImmutableList.of(
                ALL_PRIVILEGES_GROUP,
                new ConjunctivePrivilegeGroup(
                    ImmutableList.of(
                        PoliciesConfig.EDIT_DATASET_COL_BUSINESS_ATTRIBUTE_PRIVILEGE.getType()))));

    return AuthorizationUtils.isAuthorized(
        context, datasetUrn.getEntityType(), datasetUrn.toString(), orPrivilegeGroups);
  }
}
