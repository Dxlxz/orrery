package com.linkedin.orrery.graphql.resolvers.test;

import static com.linkedin.orrery.graphql.resolvers.ResolverUtils.*;
import static com.linkedin.orrery.graphql.resolvers.mutate.MutationUtils.*;
import static com.linkedin.orrery.graphql.resolvers.test.TestUtils.*;
import static com.linkedin.metadata.Constants.*;

import com.orrery.authentication.Authentication;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.data.template.SetMode;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.concurrency.GraphQLConcurrencyUtils;
import com.linkedin.orrery.graphql.exception.AuthorizationException;
import com.linkedin.orrery.graphql.generated.UpdateTestInput;
import com.linkedin.entity.client.EntityClient;
import com.linkedin.mxe.MetadataChangeProposal;
import com.linkedin.test.TestInfo;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.concurrent.CompletableFuture;

/** Updates or updates a Test. Requires the MANAGE_TESTS privilege. */
public class UpdateTestResolver implements DataFetcher<CompletableFuture<String>> {

  private final EntityClient _entityClient;

  public UpdateTestResolver(final EntityClient entityClient) {
    _entityClient = entityClient;
  }

  @Override
  public CompletableFuture<String> get(final DataFetchingEnvironment environment) throws Exception {
    final QueryContext context = environment.getContext();
    final Authentication authentication = context.getAuthentication();

    return GraphQLConcurrencyUtils.supplyAsync(
        () -> {
          if (canManageTests(context)) {

            final String urn = environment.getArgument("urn");
            final UpdateTestInput input =
                bindArgument(environment.getArgument("input"), UpdateTestInput.class);

            // Update the Test info - currently this simply creates a new test with same urn.
            final TestInfo info = mapUpdateTestInput(input);

            final MetadataChangeProposal proposal =
                buildMetadataChangeProposalWithUrn(
                    UrnUtils.getUrn(urn), TEST_INFO_ASPECT_NAME, info);
            try {
              return _entityClient.ingestProposal(context.getOperationContext(), proposal, false);
            } catch (Exception e) {
              throw new RuntimeException(
                  String.format("Failed to perform update against Test with urn %s", input), e);
            }
          }
          throw new AuthorizationException(
              "Unauthorized to perform this action. Please contact your Orrery administrator.");
        },
        this.getClass().getSimpleName(),
        "get");
  }

  private static TestInfo mapUpdateTestInput(final UpdateTestInput input) {
    final TestInfo result = new TestInfo();
    result.setName(input.getName());
    result.setCategory(input.getCategory());
    result.setDescription(input.getDescription(), SetMode.IGNORE_NULL);
    result.setDefinition(mapDefinition(input.getDefinition()));
    return result;
  }
}
