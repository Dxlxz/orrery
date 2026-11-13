package com.linkedin.orrery.graphql.resolvers.form;

import static com.linkedin.orrery.graphql.resolvers.ResolverUtils.bindArgument;

import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.concurrency.GraphQLConcurrencyUtils;
import com.linkedin.orrery.graphql.generated.FormPromptType;
import com.linkedin.orrery.graphql.generated.SubmitFormPromptInput;
import com.linkedin.orrery.graphql.resolvers.mutate.util.FormUtils;
import com.linkedin.metadata.service.FormService;
import com.linkedin.structured.PrimitivePropertyValueArray;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;

public class SubmitFormPromptResolver implements DataFetcher<CompletableFuture<Boolean>> {

  private final FormService _formService;

  public SubmitFormPromptResolver(@Nonnull final FormService formService) {
    _formService = Objects.requireNonNull(formService, "formService must not be null");
  }

  @Override
  public CompletableFuture<Boolean> get(final DataFetchingEnvironment environment)
      throws Exception {
    final QueryContext context = environment.getContext();

    final Urn entityUrn = UrnUtils.getUrn(environment.getArgument("urn"));
    final SubmitFormPromptInput input =
        bindArgument(environment.getArgument("input"), SubmitFormPromptInput.class);
    final String promptId = input.getPromptId();
    final Urn formUrn = UrnUtils.getUrn(input.getFormUrn());
    final String fieldPath = input.getFieldPath();

    return GraphQLConcurrencyUtils.supplyAsync(
        () -> {
          try {
            if (input.getType().equals(FormPromptType.STRUCTURED_PROPERTY)) {
              if (input.getStructuredPropertyParams() == null) {
                throw new IllegalArgumentException(
                    "Failed to provide structured property params for prompt type STRUCTURED_PROPERTY");
              }
              final Urn structuredPropertyUrn =
                  UrnUtils.getUrn(input.getStructuredPropertyParams().getStructuredPropertyUrn());
              final PrimitivePropertyValueArray values =
                  FormUtils.getStructuredPropertyValuesFromInput(input);

              return _formService.submitStructuredPropertyPromptResponse(
                  context.getOperationContext(),
                  entityUrn,
                  structuredPropertyUrn,
                  values,
                  formUrn,
                  promptId);
            } else if (input.getType().equals(FormPromptType.FIELDS_STRUCTURED_PROPERTY)) {
              if (input.getStructuredPropertyParams() == null) {
                throw new IllegalArgumentException(
                    "Failed to provide structured property params for prompt type FIELDS_STRUCTURED_PROPERTY");
              }
              if (fieldPath == null) {
                throw new IllegalArgumentException(
                    "Failed to provide fieldPath for prompt type FIELDS_STRUCTURED_PROPERTY");
              }
              final Urn structuredPropertyUrn =
                  UrnUtils.getUrn(input.getStructuredPropertyParams().getStructuredPropertyUrn());
              final PrimitivePropertyValueArray values =
                  FormUtils.getStructuredPropertyValuesFromInput(input);

              return _formService.submitFieldStructuredPropertyPromptResponse(
                  context.getOperationContext(),
                  entityUrn,
                  structuredPropertyUrn,
                  values,
                  formUrn,
                  promptId,
                  fieldPath);
            }
            return false;
          } catch (Exception e) {
            throw new RuntimeException(
                String.format("Failed to perform update against input %s", input), e);
          }
        },
        this.getClass().getSimpleName(),
        "get");
  }
}
