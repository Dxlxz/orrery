package com.linkedin.orrery.graphql.resolvers.module;

import static com.linkedin.orrery.graphql.resolvers.ResolverUtils.bindArgument;

import com.linkedin.common.UrnArray;
import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.authorization.AuthorizationUtils;
import com.linkedin.orrery.graphql.concurrency.GraphQLConcurrencyUtils;
import com.linkedin.orrery.graphql.exception.AuthorizationException;
import com.linkedin.orrery.graphql.generated.OrreryPageModule;
import com.linkedin.orrery.graphql.generated.OrreryPageModuleType;
import com.linkedin.orrery.graphql.generated.LinkModuleParamsInput;
import com.linkedin.orrery.graphql.generated.PageModuleScope;
import com.linkedin.orrery.graphql.generated.UpsertPageModuleInput;
import com.linkedin.orrery.graphql.types.module.PageModuleMapper;
import com.linkedin.entity.EntityResponse;
import com.linkedin.metadata.service.PageModuleService;
import com.linkedin.module.OrreryPageModuleParams;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class UpsertPageModuleResolver implements DataFetcher<CompletableFuture<OrreryPageModule>> {

  private final PageModuleService _pageModuleService;

  @Override
  public CompletableFuture<OrreryPageModule> get(DataFetchingEnvironment environment)
      throws Exception {
    final QueryContext context = environment.getContext();
    final UpsertPageModuleInput input =
        bindArgument(environment.getArgument("input"), UpsertPageModuleInput.class);

    String urn = input.getUrn();
    String name = input.getName();
    OrreryPageModuleType type = input.getType();
    PageModuleScope scope = input.getScope();
    com.linkedin.orrery.graphql.generated.PageModuleParamsInput paramsInput = input.getParams();

    if (input.getScope().equals(PageModuleScope.GLOBAL)
        && !AuthorizationUtils.canManageHomePageTemplates(context)) {
      throw new AuthorizationException("User does not have permission to update global modules.");
    }

    return GraphQLConcurrencyUtils.supplyAsync(
        () -> {
          try {
            // Map GraphQL input to GMS types
            com.linkedin.module.OrreryPageModuleType gmsType =
                com.linkedin.module.OrreryPageModuleType.valueOf(type.toString());
            com.linkedin.module.PageModuleScope gmsScope =
                com.linkedin.module.PageModuleScope.valueOf(scope.toString());
            OrreryPageModuleParams gmsParams = mapParamsInput(paramsInput);

            validateInput(gmsType, gmsParams);

            final Urn moduleUrn =
                _pageModuleService.upsertPageModule(
                    context.getOperationContext(), urn, name, gmsType, gmsScope, gmsParams);

            EntityResponse response =
                _pageModuleService.getPageModuleEntityResponse(
                    context.getOperationContext(), moduleUrn);
            return PageModuleMapper.map(context, response);
          } catch (Exception e) {
            throw new RuntimeException(
                String.format(
                    "Failed to perform upsert page module update against input %s", input),
                e);
          }
        },
        this.getClass().getSimpleName(),
        "get");
  }

  @Nonnull
  private OrreryPageModuleParams mapParamsInput(
      com.linkedin.orrery.graphql.generated.PageModuleParamsInput paramsInput) {
    OrreryPageModuleParams gmsParams = new OrreryPageModuleParams();

    if (paramsInput.getLinkParams() != null) {
      LinkModuleParamsInput inputValues = paramsInput.getLinkParams();
      com.linkedin.module.LinkModuleParams linkParams = new com.linkedin.module.LinkModuleParams();

      linkParams.setLinkUrl(inputValues.getLinkUrl());

      if (inputValues.getImageUrl() != null) {
        linkParams.setImageUrl(inputValues.getImageUrl());
      }
      if (inputValues.getDescription() != null) {
        linkParams.setDescription(inputValues.getDescription());
      }

      gmsParams.setLinkParams(linkParams);
    }

    if (paramsInput.getRichTextParams() != null) {
      com.linkedin.module.RichTextModuleParams richTextParams =
          new com.linkedin.module.RichTextModuleParams();
      richTextParams.setContent(paramsInput.getRichTextParams().getContent());
      gmsParams.setRichTextParams(richTextParams);
    }

    if (paramsInput.getAssetCollectionParams() != null) {
      com.linkedin.module.AssetCollectionModuleParams assetCollectionParams =
          new com.linkedin.module.AssetCollectionModuleParams();

      List<Urn> urns =
          paramsInput.getAssetCollectionParams().getAssetUrns().stream()
              .map(UrnUtils::getUrn)
              .collect(Collectors.toList());

      UrnArray urnArray = new UrnArray(urns);

      assetCollectionParams.setAssetUrns(urnArray);

      if (paramsInput.getAssetCollectionParams().getDynamicFilterJson() != null) {
        assetCollectionParams.setDynamicFilterJson(
            paramsInput.getAssetCollectionParams().getDynamicFilterJson());
      }

      gmsParams.setAssetCollectionParams(assetCollectionParams);
    }

    if (paramsInput.getHierarchyViewParams() != null) {
      com.linkedin.module.HierarchyModuleParams hierarchyViewParams =
          new com.linkedin.module.HierarchyModuleParams();
      if (paramsInput.getHierarchyViewParams().getAssetUrns() != null) {
        hierarchyViewParams.setAssetUrns(
            new UrnArray(
                paramsInput.getHierarchyViewParams().getAssetUrns().stream()
                    .map(UrnUtils::getUrn)
                    .collect(Collectors.toList())));
      }

      hierarchyViewParams.setShowRelatedEntities(
          paramsInput.getHierarchyViewParams().getShowRelatedEntities());

      if (paramsInput.getHierarchyViewParams().getRelatedEntitiesFilterJson() != null) {
        hierarchyViewParams.setRelatedEntitiesFilterJson(
            paramsInput.getHierarchyViewParams().getRelatedEntitiesFilterJson());
      }

      gmsParams.setHierarchyViewParams(hierarchyViewParams);
    }

    return gmsParams;
  }

  private void validateInput(
      @Nonnull final com.linkedin.module.OrreryPageModuleType type,
      @Nonnull final OrreryPageModuleParams params) {
    // check if we provide the correct params given the type of module we're creating
    if (type.equals(com.linkedin.module.OrreryPageModuleType.RICH_TEXT)) {
      if (params.getRichTextParams() == null) {
        throw new IllegalArgumentException("Did not provide rich text params for rich text module");
      }
    } else if (type.equals(com.linkedin.module.OrreryPageModuleType.LINK)) {
      if (params.getLinkParams() == null) {
        throw new IllegalArgumentException("Did not provide link params for link module");
      }
    } else if (type.equals(com.linkedin.module.OrreryPageModuleType.ASSET_COLLECTION)) {
      if (params.getAssetCollectionParams() == null) {
        throw new IllegalArgumentException(
            "Did not provide asset collection params for asset collection module");
      }
    } else if (type.equals(com.linkedin.module.OrreryPageModuleType.HIERARCHY)) {
      if (params.getHierarchyViewParams() == null) {
        throw new IllegalArgumentException(
            "Did not provide hierarchy view params for hierarchy view module");
      }
    } else {
      // TODO: add more blocks to this check as we support creating more types of modules to this
      // resolver
      // If someone tries to create one of the default modules this error will be thrown
      throw new IllegalArgumentException("Attempted to create an unsupported module type.");
    }
  }
}
