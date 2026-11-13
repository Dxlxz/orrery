package com.linkedin.orrery.graphql.resolvers.application;

import static com.linkedin.orrery.graphql.resolvers.ResolverUtils.bindArgument;
import static com.linkedin.orrery.graphql.resolvers.ResolverUtils.getQueryContext;

import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.concurrency.GraphQLConcurrencyUtils;
import com.linkedin.orrery.graphql.exception.AuthorizationException;
import com.linkedin.orrery.graphql.generated.Application;
import com.linkedin.orrery.graphql.generated.CreateApplicationInput;
import com.linkedin.orrery.graphql.generated.OwnerEntityType;
import com.linkedin.orrery.graphql.resolvers.mutate.util.OwnerUtils;
import com.linkedin.orrery.graphql.types.application.mappers.ApplicationMapper;
import com.linkedin.entity.EntityResponse;
import com.linkedin.metadata.entity.EntityService;
import com.linkedin.metadata.service.ApplicationService;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class CreateApplicationResolver implements DataFetcher<CompletableFuture<Application>> {

  private final ApplicationService applicationService;
  private final EntityService entityService;

  @Override
  public CompletableFuture<Application> get(final DataFetchingEnvironment environment)
      throws Exception {

    final QueryContext context = getQueryContext(environment);
    final CreateApplicationInput input =
        bindArgument(environment.getArgument("input"), CreateApplicationInput.class);

    return GraphQLConcurrencyUtils.supplyAsync(
        () -> {
          if (!ApplicationAuthorizationUtils.canCreateApplications(context)) {
            throw new AuthorizationException(
                "Unauthorized to perform this action. Please contact your Orrery administrator.");
          }

          try {
            final Urn applicationUrn =
                applicationService.createApplication(
                    context.getOperationContext(),
                    input.getId(),
                    input.getProperties().getName(),
                    input.getProperties().getDescription());
            if (input.getDomainUrn() != null) {
              applicationService.setDomain(
                  context.getOperationContext(),
                  applicationUrn,
                  UrnUtils.getUrn(input.getDomainUrn()));
            }
            Boolean shouldAddCreatorAsOwner = input.getShouldAddCreatorAsOwner();
            if (shouldAddCreatorAsOwner == null || shouldAddCreatorAsOwner) {
              OwnerUtils.addCreatorAsOwner(
                  context, applicationUrn.toString(), OwnerEntityType.CORP_USER, entityService);
            }
            EntityResponse response =
                applicationService.getApplicationEntityResponse(
                    context.getOperationContext(), applicationUrn);
            if (response != null) {
              return ApplicationMapper.map(context, response);
            }
            // should never happen
            log.error(String.format("Unable to find application with urn %s", applicationUrn));
            return null;
          } catch (Exception e) {
            throw new RuntimeException(
                String.format("Failed to create a new Application from input %s", input), e);
          }
        },
        this.getClass().getSimpleName(),
        "get");
  }
}
