package com.linkedin.orrery.graphql.resolvers.form;

import static com.linkedin.orrery.graphql.resolvers.ResolverUtils.bindArgument;

import com.linkedin.common.urn.Urn;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.authorization.AuthorizationUtils;
import com.linkedin.orrery.graphql.concurrency.GraphQLConcurrencyUtils;
import com.linkedin.orrery.graphql.exception.AuthorizationException;
import com.linkedin.orrery.graphql.generated.CreateFormInput;
import com.linkedin.orrery.graphql.generated.CreatePromptInput;
import com.linkedin.orrery.graphql.generated.Form;
import com.linkedin.orrery.graphql.generated.FormPromptType;
import com.linkedin.orrery.graphql.resolvers.mutate.util.FormUtils;
import com.linkedin.orrery.graphql.types.form.FormMapper;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.client.EntityClient;
import com.linkedin.form.FormInfo;
import com.linkedin.metadata.Constants;
import com.linkedin.metadata.service.FormService;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class CreateFormResolver implements DataFetcher<CompletableFuture<Form>> {

  private final EntityClient _entityClient;
  private final FormService _formService;

  public CreateFormResolver(
      @Nonnull final EntityClient entityClient, @Nonnull final FormService formService) {
    _entityClient = Objects.requireNonNull(entityClient, "entityClient must not be null");
    _formService = Objects.requireNonNull(formService, "formService must not be null");
  }

  @Override
  public CompletableFuture<Form> get(final DataFetchingEnvironment environment) throws Exception {
    final QueryContext context = environment.getContext();

    final CreateFormInput input =
        bindArgument(environment.getArgument("input"), CreateFormInput.class);
    final FormInfo formInfo = FormUtils.mapFormInfo(input);

    return GraphQLConcurrencyUtils.supplyAsync(
        () -> {
          try {
            if (!AuthorizationUtils.canManageForms(context)) {
              throw new AuthorizationException("Unable to create form. Please contact your admin.");
            }
            validatePrompts(input.getPrompts());

            Urn formUrn =
                _formService.createForm(context.getOperationContext(), formInfo, input.getId());
            EntityResponse response =
                _entityClient.getV2(
                    context.getOperationContext(), Constants.FORM_ENTITY_NAME, formUrn, null);
            return FormMapper.map(context, response);
          } catch (Exception e) {
            throw new RuntimeException(
                String.format("Failed to perform update against input %s", input), e);
          }
        },
        this.getClass().getSimpleName(),
        "get");
  }

  private void validatePrompts(@Nullable List<CreatePromptInput> prompts) {
    if (prompts == null) {
      return;
    }
    prompts.forEach(
        prompt -> {
          if (prompt.getType().equals(FormPromptType.STRUCTURED_PROPERTY)
              || prompt.getType().equals(FormPromptType.FIELDS_STRUCTURED_PROPERTY)) {
            if (prompt.getStructuredPropertyParams() == null) {
              throw new IllegalArgumentException(
                  "Provided prompt with type STRUCTURED_PROPERTY or FIELDS_STRUCTURED_PROPERTY and no structured property params");
            }
          }
        });
  }
}
