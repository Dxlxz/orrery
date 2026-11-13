package com.linkedin.orrery.graphql.resolvers.businessattribute;

import static com.linkedin.orrery.graphql.resolvers.ResolverUtils.bindArgument;

import com.linkedin.businessattribute.BusinessAttributeInfo;
import com.linkedin.common.AuditStamp;
import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.concurrency.GraphQLConcurrencyUtils;
import com.linkedin.orrery.graphql.exception.AuthorizationException;
import com.linkedin.orrery.graphql.exception.OrreryGraphQLErrorCode;
import com.linkedin.orrery.graphql.exception.OrreryGraphQLException;
import com.linkedin.orrery.graphql.generated.BusinessAttribute;
import com.linkedin.orrery.graphql.generated.UpdateBusinessAttributeInput;
import com.linkedin.orrery.graphql.resolvers.mutate.util.BusinessAttributeUtils;
import com.linkedin.orrery.graphql.types.businessattribute.mappers.BusinessAttributeMapper;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.client.EntityClient;
import com.linkedin.metadata.Constants;
import com.linkedin.metadata.entity.AspectUtils;
import com.linkedin.metadata.service.BusinessAttributeService;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.orreryproject.metadata.context.OperationContext;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class UpdateBusinessAttributeResolver
    implements DataFetcher<CompletableFuture<BusinessAttribute>> {

  private final EntityClient _entityClient;
  private final BusinessAttributeService businessAttributeService;

  @Override
  public CompletableFuture<BusinessAttribute> get(DataFetchingEnvironment environment)
      throws Exception {
    QueryContext context = environment.getContext();
    UpdateBusinessAttributeInput input =
        bindArgument(environment.getArgument("input"), UpdateBusinessAttributeInput.class);
    final Urn businessAttributeUrn = UrnUtils.getUrn(environment.getArgument("urn"));
    if (!BusinessAttributeAuthorizationUtils.canCreateBusinessAttribute(context)) {
      throw new AuthorizationException(
          "Unauthorized to perform this action. Please contact your Orrery administrator.");
    }
    if (!_entityClient.exists(context.getOperationContext(), businessAttributeUrn)) {
      throw new RuntimeException(
          String.format("This urn does not exist: %s", businessAttributeUrn));
    }
    return GraphQLConcurrencyUtils.supplyAsync(
        () -> {
          try {
            Urn updatedBusinessAttributeUrn =
                updateBusinessAttribute(input, businessAttributeUrn, context);
            return BusinessAttributeMapper.map(
                context,
                businessAttributeService.getBusinessAttributeEntityResponse(
                    context.getOperationContext(), updatedBusinessAttributeUrn));
          } catch (OrreryGraphQLException e) {
            throw e;
          } catch (Exception e) {
            throw new RuntimeException(
                String.format(
                    "Failed to update Business Attribute with urn %s", businessAttributeUrn),
                e);
          }
        },
        this.getClass().getSimpleName(),
        "get");
  }

  private Urn updateBusinessAttribute(
      UpdateBusinessAttributeInput input, Urn businessAttributeUrn, QueryContext context) {
    try {
      BusinessAttributeInfo businessAttributeInfo =
          getBusinessAttributeInfo(context.getOperationContext(), businessAttributeUrn);
      // 1. Check whether the Business Attribute exists
      if (businessAttributeInfo == null) {
        throw new IllegalArgumentException(
            String.format(
                "Failed to update Business Attribute. Business Attribute with urn %s does not exist.",
                businessAttributeUrn));
      }

      // 2. Apply changes to existing Business Attribute
      if (Objects.nonNull(input.getName())) {
        if (BusinessAttributeUtils.hasNameConflict(input.getName(), context, _entityClient)) {
          throw new OrreryGraphQLException(
              String.format(
                  "\"%s\" already exists as Business Attribute. Please pick a unique name.",
                  input.getName()),
              OrreryGraphQLErrorCode.CONFLICT);
        }
        businessAttributeInfo.setName(input.getName());
        businessAttributeInfo.setFieldPath(input.getName());
      }
      if (Objects.nonNull(input.getDescription())) {
        businessAttributeInfo.setDescription(input.getDescription());
      }
      if (Objects.nonNull(input.getType())) {
        businessAttributeInfo.setType(
            BusinessAttributeUtils.mapSchemaFieldDataType(input.getType()));
      }
      businessAttributeInfo.setLastModified(
          new AuditStamp()
              .setActor(UrnUtils.getUrn(context.getActorUrn()))
              .setTime(System.currentTimeMillis()));
      // 3. Write changes to GMS
      return UrnUtils.getUrn(
          _entityClient.ingestProposal(
              context.getOperationContext(),
              AspectUtils.buildMetadataChangeProposal(
                  businessAttributeUrn,
                  Constants.BUSINESS_ATTRIBUTE_INFO_ASPECT_NAME,
                  businessAttributeInfo)));

    } catch (OrreryGraphQLException e) {
      throw e;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Nullable
  private BusinessAttributeInfo getBusinessAttributeInfo(
      @Nonnull OperationContext opContext, @Nonnull final Urn businessAttributeUrn) {
    Objects.requireNonNull(businessAttributeUrn, "businessAttributeUrn must not be null");
    Objects.requireNonNull(opContext, "opContext must not be null");
    final EntityResponse response =
        businessAttributeService.getBusinessAttributeEntityResponse(
            opContext, businessAttributeUrn);
    if (response != null
        && response.getAspects().containsKey(Constants.BUSINESS_ATTRIBUTE_INFO_ASPECT_NAME)) {
      return new BusinessAttributeInfo(
          response
              .getAspects()
              .get(Constants.BUSINESS_ATTRIBUTE_INFO_ASPECT_NAME)
              .getValue()
              .data());
    }
    // No aspect found
    return null;
  }
}
