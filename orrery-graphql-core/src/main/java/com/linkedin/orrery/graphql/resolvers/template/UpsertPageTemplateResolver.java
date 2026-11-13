package com.linkedin.orrery.graphql.resolvers.template;

import static com.linkedin.orrery.graphql.resolvers.ResolverUtils.bindArgument;

import com.linkedin.common.UrnArray;
import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.authorization.AuthorizationUtils;
import com.linkedin.orrery.graphql.concurrency.GraphQLConcurrencyUtils;
import com.linkedin.orrery.graphql.exception.AuthorizationException;
import com.linkedin.orrery.graphql.generated.OrreryPageTemplate;
import com.linkedin.orrery.graphql.generated.PageTemplateAssetSummaryInput;
import com.linkedin.orrery.graphql.generated.PageTemplateRowInput;
import com.linkedin.orrery.graphql.generated.PageTemplateScope;
import com.linkedin.orrery.graphql.generated.PageTemplateSurfaceType;
import com.linkedin.orrery.graphql.generated.UpsertPageTemplateInput;
import com.linkedin.orrery.graphql.types.template.PageTemplateMapper;
import com.linkedin.entity.EntityResponse;
import com.linkedin.metadata.service.PageTemplateService;
import com.linkedin.template.OrreryPageTemplateAssetSummary;
import com.linkedin.template.OrreryPageTemplateRow;
import com.linkedin.template.SummaryElement;
import com.linkedin.template.SummaryElementArray;
import com.linkedin.template.SummaryElementType;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class UpsertPageTemplateResolver
    implements DataFetcher<CompletableFuture<OrreryPageTemplate>> {

  private final PageTemplateService _pageTemplateService;

  @Override
  public CompletableFuture<OrreryPageTemplate> get(DataFetchingEnvironment environment)
      throws Exception {
    final QueryContext context = environment.getContext();
    final UpsertPageTemplateInput input =
        bindArgument(environment.getArgument("input"), UpsertPageTemplateInput.class);

    String urn = input.getUrn();
    List<PageTemplateRowInput> rows = input.getRows();
    PageTemplateScope scope = input.getScope();
    PageTemplateSurfaceType surfaceType = input.getSurfaceType();

    if (input.getScope().equals(PageTemplateScope.GLOBAL)
        && !AuthorizationUtils.canManageHomePageTemplates(context)) {
      throw new AuthorizationException("User does not have permission to update global templates.");
    }

    return GraphQLConcurrencyUtils.supplyAsync(
        () -> {
          try {
            final Urn templateUrn =
                _pageTemplateService.upsertPageTemplate(
                    context.getOperationContext(),
                    urn,
                    mapInputRows(rows),
                    com.linkedin.template.PageTemplateScope.valueOf(scope.toString()),
                    com.linkedin.template.PageTemplateSurfaceType.valueOf(surfaceType.toString()),
                    mapAssetSummary(input.getAssetSummary()));

            EntityResponse response =
                _pageTemplateService.getPageTemplateEntityResponse(
                    context.getOperationContext(), templateUrn);
            return PageTemplateMapper.map(context, response);
          } catch (Exception e) {
            throw new RuntimeException(
                String.format(
                    "Failed to perform upsert page template update against input %s", input),
                e);
          }
        },
        this.getClass().getSimpleName(),
        "get");
  }

  @Nonnull
  private List<OrreryPageTemplateRow> mapInputRows(List<PageTemplateRowInput> inputRows) {
    List<OrreryPageTemplateRow> finalRows = new ArrayList<>();
    inputRows.forEach(
        row -> {
          OrreryPageTemplateRow templateRow = new OrreryPageTemplateRow();
          UrnArray modules = new UrnArray();
          row.getModules().forEach(moduleUrn -> modules.add(UrnUtils.getUrn(moduleUrn)));
          templateRow.setModules(modules);
          finalRows.add(templateRow);
        });
    return finalRows;
  }

  @Nullable
  private OrreryPageTemplateAssetSummary mapAssetSummary(
      @Nullable final PageTemplateAssetSummaryInput inputAssetSummary) {
    if (inputAssetSummary == null) {
      return null;
    }

    OrreryPageTemplateAssetSummary assetSummary = new OrreryPageTemplateAssetSummary();

    SummaryElementArray summaryElements = new SummaryElementArray();
    inputAssetSummary
        .getSummaryElements()
        .forEach(
            el -> {
              SummaryElement element = new SummaryElement();
              element.setElementType(SummaryElementType.valueOf(el.getElementType().toString()));
              if (el.getStructuredPropertyUrn() != null) {
                element.setStructuredPropertyUrn(UrnUtils.getUrn(el.getStructuredPropertyUrn()));
              }
              summaryElements.add(element);
            });

    assetSummary.setSummaryElements(summaryElements);
    return assetSummary;
  }
}
