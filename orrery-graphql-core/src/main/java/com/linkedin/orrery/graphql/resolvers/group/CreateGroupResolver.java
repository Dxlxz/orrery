package com.linkedin.orrery.graphql.resolvers.group;

import static com.linkedin.orrery.graphql.resolvers.ResolverUtils.*;

import com.orrery.authentication.Authentication;
import com.orrery.authentication.group.GroupService;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.authorization.AuthorizationUtils;
import com.linkedin.orrery.graphql.concurrency.GraphQLConcurrencyUtils;
import com.linkedin.orrery.graphql.exception.AuthorizationException;
import com.linkedin.orrery.graphql.generated.CreateGroupInput;
import com.linkedin.metadata.key.CorpGroupKey;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

// Currently, this resolver will override the group details, but not group membership, if a group
// with the same name already exists.
public class CreateGroupResolver implements DataFetcher<CompletableFuture<String>> {

  private final GroupService _groupService;

  public CreateGroupResolver(final GroupService groupService) {
    _groupService = groupService;
  }

  @Override
  public CompletableFuture<String> get(final DataFetchingEnvironment environment) throws Exception {
    final QueryContext context = environment.getContext();
    Authentication authentication = context.getAuthentication();

    if (!AuthorizationUtils.canManageUsersAndGroups(context)) {
      throw new AuthorizationException(
          "Unauthorized to perform this action. Please contact your Orrery administrator.");
    }
    final CreateGroupInput input =
        bindArgument(environment.getArgument("input"), CreateGroupInput.class);

    return GraphQLConcurrencyUtils.supplyAsync(
        () -> {
          try {
            // First, check if the group already exists.
            // Create the Group key.
            final CorpGroupKey key = new CorpGroupKey();
            final String id = input.getId() != null ? input.getId() : UUID.randomUUID().toString();
            final String description = input.getDescription() != null ? input.getDescription() : "";
            key.setName(id); // 'name' in the key really reflects nothing more than a stable "id".
            return _groupService.createNativeGroup(
                context.getOperationContext(), key, input.getName(), description);
          } catch (Exception e) {
            throw new RuntimeException("Failed to create group", e);
          }
        },
        this.getClass().getSimpleName(),
        "get");
  }
}
