package com.linkedin.orrery.graphql.resolvers.post;

import com.orrery.authentication.Authentication;
import com.orrery.authentication.post.PostService;
import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.authorization.AuthorizationUtils;
import com.linkedin.orrery.graphql.concurrency.GraphQLConcurrencyUtils;
import com.linkedin.orrery.graphql.exception.AuthorizationException;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class DeletePostResolver implements DataFetcher<CompletableFuture<Boolean>> {
  private final PostService _postService;

  @Override
  public CompletableFuture<Boolean> get(final DataFetchingEnvironment environment)
      throws Exception {
    final QueryContext context = environment.getContext();

    if (!AuthorizationUtils.canManageGlobalAnnouncements(context)) {
      throw new AuthorizationException(
          "Unauthorized to delete posts. Please contact your Orrery administrator if this needs corrective action.");
    }

    final Urn postUrn = UrnUtils.getUrn(environment.getArgument("urn"));
    final Authentication authentication = context.getAuthentication();

    return GraphQLConcurrencyUtils.supplyAsync(
        () -> {
          try {
            return _postService.deletePost(context.getOperationContext(), postUrn);
          } catch (Exception e) {
            throw new RuntimeException("Failed to create a new post", e);
          }
        },
        this.getClass().getSimpleName(),
        "get");
  }
}
