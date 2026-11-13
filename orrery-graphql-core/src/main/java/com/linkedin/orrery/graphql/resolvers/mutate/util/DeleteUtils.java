package com.linkedin.orrery.graphql.resolvers.mutate.util;

import static com.linkedin.orrery.graphql.resolvers.mutate.MutationUtils.*;
import static com.linkedin.metadata.authorization.ApiOperation.DELETE;

import com.orrery.authorization.AuthUtil;
import com.linkedin.common.Status;
import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.metadata.Constants;
import com.linkedin.metadata.entity.EntityService;
import com.linkedin.metadata.entity.EntityUtils;
import com.linkedin.mxe.MetadataChangeProposal;
import io.orreryproject.metadata.context.OperationContext;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DeleteUtils {

  private DeleteUtils() {}

  public static boolean isAuthorizedToDeleteEntity(@Nonnull QueryContext context, Urn entityUrn) {
    return AuthUtil.isAuthorizedEntityUrns(
        context.getOperationContext(), DELETE, List.of(entityUrn));
  }

  public static void updateStatusForResources(
      @Nonnull OperationContext opContext,
      boolean removed,
      List<String> urnStrs,
      Urn actor,
      EntityService entityService) {
    final List<MetadataChangeProposal> changes = new ArrayList<>();
    for (String urnStr : urnStrs) {
      changes.add(buildSoftDeleteProposal(opContext, removed, urnStr, actor, entityService));
    }
    EntityUtils.ingestChangeProposals(opContext, changes, entityService, actor, false);
  }

  private static MetadataChangeProposal buildSoftDeleteProposal(
      @Nonnull OperationContext opContext,
      boolean removed,
      String urnStr,
      Urn actor,
      EntityService entityService) {
    Status status =
        (Status)
            EntityUtils.getAspectFromEntity(
                opContext, urnStr, Constants.STATUS_ASPECT_NAME, entityService, new Status());
    status.setRemoved(removed);
    return buildMetadataChangeProposalWithUrn(
        UrnUtils.getUrn(urnStr), Constants.STATUS_ASPECT_NAME, status);
  }
}
