package com.linkedin.orrery.graphql.resolvers.group;

import static com.linkedin.orrery.graphql.authorization.AuthorizationUtils.*;
import static com.linkedin.orrery.graphql.resolvers.ResolverUtils.*;

import com.orrery.authentication.Authentication;
import com.orrery.authentication.group.GroupService;
import com.linkedin.common.Origin;
import com.linkedin.common.OriginType;
import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.concurrency.GraphQLConcurrencyUtils;
import com.linkedin.orrery.graphql.exception.AuthorizationException;
import com.linkedin.orrery.graphql.exception.OrreryGraphQLErrorCode;
import com.linkedin.orrery.graphql.exception.OrreryGraphQLException;
import com.linkedin.orrery.graphql.generated.RemoveGroupMembersInput;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class RemoveGroupMembersResolver implements DataFetcher<CompletableFuture<Boolean>> {

  private final GroupService _groupService;

  public RemoveGroupMembersResolver(final GroupService groupService) {
    _groupService = groupService;
  }

  @Override
  public CompletableFuture<Boolean> get(final DataFetchingEnvironment environment)
      throws Exception {

    final RemoveGroupMembersInput input =
        bindArgument(environment.getArgument("input"), RemoveGroupMembersInput.class);
    final String groupUrnStr = input.getGroupUrn();
    final QueryContext context = environment.getContext();
    final Authentication authentication = context.getAuthentication();

    if (!canEditGroupMembers(groupUrnStr, context)) {
      throw new AuthorizationException(
          "Unauthorized to perform this action. Please contact your Orrery administrator.");
    }

    final Urn groupUrn = Urn.createFromString(groupUrnStr);
    final List<Urn> userUrnList =
        input.getUserUrns().stream().map(UrnUtils::getUrn).collect(Collectors.toList());

    if (!_groupService.groupExists(context.getOperationContext(), groupUrn)) {
      // The group doesn't exist.
      throw new OrreryGraphQLException(
          String.format(
              "Failed to add remove members from group %s. Group does not exist.", groupUrnStr),
          OrreryGraphQLErrorCode.NOT_FOUND);
    }

    return GraphQLConcurrencyUtils.supplyAsync(
        () -> {
          Origin groupOrigin =
              _groupService.getGroupOrigin(context.getOperationContext(), groupUrn);
          if (groupOrigin == null || !groupOrigin.hasType()) {
            try {
              _groupService.migrateGroupMembershipToNativeGroupMembership(
                  context.getOperationContext(), groupUrn, context.getActorUrn());
            } catch (Exception e) {
              throw new RuntimeException(
                  String.format(
                      "Failed to migrate group membership when removing group members from group %s",
                      groupUrnStr),
                  e);
            }
          } else if (groupOrigin.getType() == OriginType.EXTERNAL) {
            throw new RuntimeException(
                String.format(
                    "Group %s was ingested from an external provider and cannot have members manually removed from it",
                    groupUrnStr));
          }
          try {
            _groupService.removeGroupMembers(context.getOperationContext(), groupUrn, userUrnList);
            return true;
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        },
        this.getClass().getSimpleName(),
        "get");
  }
}
