package com.linkedin.orrery.graphql.resolvers.policy;

import static com.linkedin.orrery.graphql.resolvers.ResolverUtils.bindArgument;

import com.orrery.authorization.AuthorizerChain;
import com.orrery.authorization.OrreryAuthorizer;
import com.orrery.authorization.EntitySpec;
import com.orrery.authorization.PolicyEngine;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.concurrency.GraphQLConcurrencyUtils;
import com.linkedin.orrery.graphql.exception.AuthorizationException;
import com.linkedin.orrery.graphql.generated.GetGrantedPrivilegesInput;
import com.linkedin.orrery.graphql.generated.PolicyEvaluationDetail;
import com.linkedin.orrery.graphql.generated.Privileges;
import com.linkedin.orrery.graphql.types.entitytype.EntityTypeMapper;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;

/**
 * Resolver to support the getGrantedPrivileges end point Fetches all privileges that are granted
 * for the given actor for the given resource (optional)
 */
@Slf4j
public class GetGrantedPrivilegesResolver implements DataFetcher<CompletableFuture<Privileges>> {

  @Override
  public CompletableFuture<Privileges> get(final DataFetchingEnvironment environment)
      throws Exception {

    final QueryContext context = environment.getContext();
    final GetGrantedPrivilegesInput input =
        bindArgument(environment.getArgument("input"), GetGrantedPrivilegesInput.class);
    final String actor = input.getActorUrn();
    if (!isAuthorized(context, actor)) {
      throw new AuthorizationException("Unauthorized to get privileges for the given author.");
    }
    final Optional<EntitySpec> resourceSpec =
        Optional.ofNullable(input.getResourceSpec())
            .map(
                spec ->
                    new EntitySpec(
                        EntityTypeMapper.getName(spec.getResourceType()), spec.getResourceUrn()));

    if (context.getAuthorizer() instanceof AuthorizerChain) {
      OrreryAuthorizer orreryAuthorizer =
          ((AuthorizerChain) context.getAuthorizer()).getDefaultAuthorizer();

      return GraphQLConcurrencyUtils.supplyAsync(
          () -> {
            try {
              PolicyEngine.PolicyGrantedPrivileges evalResult =
                  orreryAuthorizer.getGrantedPrivileges(
                      actor, resourceSpec, context.getOperationContext());

              List<PolicyEvaluationDetail> evaluationDetailList = null;

              if (input.getIncludeEvaluationDetails()
                  && PolicyAuthUtils.canManagePolicies(context)) {
                evaluationDetailList = new ArrayList<>();
                for (Map.Entry<String, String> entry : evalResult.getReasonOfDeny().entrySet()) {
                  evaluationDetailList.add(
                      new PolicyEvaluationDetail(entry.getKey(), entry.getValue()));
                }
              }

              return Privileges.builder()
                  .setPrivileges(evalResult.getPrivileges())
                  .setEvaluationDetails(evaluationDetailList)
                  .build();
            } catch (Exception e) {
              log.error("Failed to get granted privileges", e);
              throw new RuntimeException("Failed to get granted privileges", e);
            }
          },
          this.getClass().getSimpleName(),
          "get");
    }
    throw new UnsupportedOperationException(
        String.format(
            "GetGrantedPrivileges function is not supported on authorizer of type %s",
            context.getAuthorizer().getClass().getSimpleName()));
  }

  private boolean isAuthorized(final QueryContext context, final String actor) {
    return PolicyAuthUtils.canManagePolicies(context) || actor.equals(context.getActorUrn());
  }
}
