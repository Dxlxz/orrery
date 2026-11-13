package com.linkedin.orrery.graphql.resolvers.form;

import static com.linkedin.orrery.graphql.resolvers.ResolverUtils.bindArgument;

import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.authorization.AuthorizationUtils;
import com.linkedin.orrery.graphql.concurrency.GraphQLConcurrencyUtils;
import com.linkedin.orrery.graphql.exception.AuthorizationException;
import com.linkedin.orrery.graphql.generated.CreateDynamicFormAssignmentInput;
import com.linkedin.orrery.graphql.resolvers.mutate.util.FormUtils;
import com.linkedin.form.DynamicFormAssignment;
import com.linkedin.metadata.service.FormService;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public class CreateDynamicFormAssignmentResolver
    implements DataFetcher<CompletableFuture<Boolean>> {

  private final FormService _formService;

  public CreateDynamicFormAssignmentResolver(@Nonnull final FormService formService) {
    _formService = Objects.requireNonNull(formService, "formService must not be null");
  }

  @Override
  public CompletableFuture<Boolean> get(final DataFetchingEnvironment environment)
      throws Exception {
    final QueryContext context = environment.getContext();

    final CreateDynamicFormAssignmentInput input =
        bindArgument(environment.getArgument("input"), CreateDynamicFormAssignmentInput.class);
    final Urn formUrn = UrnUtils.getUrn(input.getFormUrn());
    final DynamicFormAssignment formAssignment = FormUtils.mapDynamicFormAssignment(input);

    return GraphQLConcurrencyUtils.supplyAsync(
        () -> {
          if (!AuthorizationUtils.canManageForms(context)) {
            throw new AuthorizationException(
                "Unauthorized to perform this action. Please contact your Orrery administrator.");
          }
          try {
            _formService.createDynamicFormAssignment(
                context.getOperationContext(), formAssignment, formUrn);
            return true;
          } catch (Exception e) {
            throw new RuntimeException(
                String.format("Failed to perform update against input %s", input), e);
          }
        },
        this.getClass().getSimpleName(),
        "get");
  }
}
