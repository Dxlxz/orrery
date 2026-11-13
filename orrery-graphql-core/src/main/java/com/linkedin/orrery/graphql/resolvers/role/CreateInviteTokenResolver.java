package com.linkedin.orrery.graphql.resolvers.role;

import static com.linkedin.orrery.graphql.authorization.AuthorizationUtils.*;
import static com.linkedin.orrery.graphql.resolvers.ResolverUtils.*;

import com.orrery.authentication.Authentication;
import com.orrery.authentication.invite.InviteTokenAuthorizationException;
import com.orrery.authentication.invite.InviteTokenService;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.concurrency.GraphQLConcurrencyUtils;
import com.linkedin.orrery.graphql.exception.AuthorizationException;
import com.linkedin.orrery.graphql.generated.CreateInviteTokenInput;
import com.linkedin.orrery.graphql.generated.InviteToken;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class CreateInviteTokenResolver implements DataFetcher<CompletableFuture<InviteToken>> {
  private final InviteTokenService _inviteTokenService;

  @Override
  public CompletableFuture<InviteToken> get(final DataFetchingEnvironment environment)
      throws Exception {
    final QueryContext context = environment.getContext();
    if (!canManageUserCredentials(context)) {
      throw new AuthorizationException(
          "Unauthorized to create invite tokens. Please contact your Orrery administrator if this needs corrective action.");
    }

    final CreateInviteTokenInput input =
        bindArgument(environment.getArgument("input"), CreateInviteTokenInput.class);
    final String roleUrnStr = input.getRoleUrn();
    final Authentication authentication = context.getAuthentication();

    return GraphQLConcurrencyUtils.supplyAsync(
        () -> {
          try {
            return new InviteToken(
                _inviteTokenService.getInviteToken(
                    context.getOperationContext(), roleUrnStr, true));
          } catch (InviteTokenAuthorizationException e) {
            throw new AuthorizationException(e.getMessage(), e);
          } catch (Exception e) {
            throw new RuntimeException(
                String.format("Failed to create invite token for role %s", roleUrnStr), e);
          }
        },
        this.getClass().getSimpleName(),
        "get");
  }
}
