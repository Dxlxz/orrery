package com.linkedin.orrery.graphql.resolvers.mutate;

import static com.linkedin.orrery.graphql.resolvers.ResolverUtils.*;

import com.google.common.collect.ImmutableList;
import com.linkedin.common.urn.CorpuserUrn;
import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.concurrency.GraphQLConcurrencyUtils;
import com.linkedin.orrery.graphql.exception.AuthorizationException;
import com.linkedin.orrery.graphql.generated.AddTagsInput;
import com.linkedin.orrery.graphql.generated.ResourceRefInput;
import com.linkedin.orrery.graphql.resolvers.mutate.util.LabelUtils;
import com.linkedin.metadata.Constants;
import com.linkedin.metadata.entity.EntityService;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class AddTagsResolver implements DataFetcher<CompletableFuture<Boolean>> {

  private final EntityService _entityService;

  @Override
  public CompletableFuture<Boolean> get(DataFetchingEnvironment environment) throws Exception {
    final QueryContext context = environment.getContext();
    final AddTagsInput input = bindArgument(environment.getArgument("input"), AddTagsInput.class);
    List<Urn> tagUrns =
        input.getTagUrns().stream().map(UrnUtils::getUrn).collect(Collectors.toList());
    Urn targetUrn = Urn.createFromString(input.getResourceUrn());

    return GraphQLConcurrencyUtils.supplyAsync(
        () -> {
          if (!LabelUtils.isAuthorizedToUpdateTags(
              context, targetUrn, input.getSubResource(), tagUrns)) {
            throw new AuthorizationException(
                "Unauthorized to perform this action. Please contact your Orrery administrator.");
          }

          LabelUtils.validateResourceAndLabel(
              context.getOperationContext(),
              tagUrns,
              targetUrn,
              input.getSubResource(),
              input.getSubResourceType(),
              Constants.TAG_ENTITY_NAME,
              _entityService,
              false);
          try {
            log.info("Adding Tags. input: {}", input.toString());
            Urn actor = CorpuserUrn.createFromString(context.getActorUrn());
            LabelUtils.addTagsToResources(
                context.getOperationContext(),
                tagUrns,
                ImmutableList.of(
                    new ResourceRefInput(
                        input.getResourceUrn(),
                        input.getSubResourceType(),
                        input.getSubResource())),
                actor,
                _entityService);
            return true;
          } catch (Exception e) {
            log.error(
                "Failed to perform update against input {}, {}", input.toString(), e.getMessage());
            throw new RuntimeException(
                String.format("Failed to perform update against input %s", input.toString()), e);
          }
        },
        this.getClass().getSimpleName(),
        "get");
  }
}
