package com.linkedin.orrery.graphql.resolvers.connection;

import com.linkedin.common.DataPlatformInstance;
import com.linkedin.common.urn.Urn;
import com.linkedin.data.DataMap;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.OrreryConnection;
import com.linkedin.orrery.graphql.generated.OrreryConnectionDetails;
import com.linkedin.orrery.graphql.generated.OrreryJsonConnection;
import com.linkedin.orrery.graphql.generated.DataPlatform;
import com.linkedin.orrery.graphql.generated.EntityType;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.EnvelopedAspect;
import com.linkedin.entity.EnvelopedAspectMap;
import com.linkedin.metadata.Constants;
import io.orreryproject.metadata.context.OperationContext;
import io.orreryproject.metadata.services.SecretService;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class ConnectionMapper {
  /**
   * Maps a GMS encrypted connection details object into the decrypted form returned by the GraphQL
   * API.
   *
   * <p>Returns null if the Entity does not have the required aspects: orreryConnectionDetails or
   * dataPlatformInstance.
   */
  @Nullable
  public static OrreryConnection map(
      @Nonnull final QueryContext context,
      @Nonnull final EntityResponse entityResponse,
      @Nonnull final SecretService secretService) {
    // If the connection does not exist, simply return null
    if (!hasAspects(entityResponse)) {
      return null;
    }

    final OrreryConnection result = new OrreryConnection();
    final Urn entityUrn = entityResponse.getUrn();
    final EnvelopedAspectMap aspects = entityResponse.getAspects();

    result.setUrn(entityUrn.toString());
    result.setType(EntityType.ORRERY_CONNECTION);

    final EnvelopedAspect envelopedAssertionInfo =
        aspects.get(Constants.ORRERY_CONNECTION_DETAILS_ASPECT_NAME);
    if (envelopedAssertionInfo != null) {
      result.setDetails(
          mapConnectionDetails(
              context,
              new com.linkedin.connection.OrreryConnectionDetails(
                  envelopedAssertionInfo.getValue().data()),
              secretService));
    }
    final EnvelopedAspect envelopedPlatformInstance =
        aspects.get(Constants.DATA_PLATFORM_INSTANCE_ASPECT_NAME);
    if (envelopedPlatformInstance != null) {
      final DataMap data = envelopedPlatformInstance.getValue().data();
      result.setPlatform(mapPlatform(new DataPlatformInstance(data)));
    }
    return result;
  }

  private static OrreryConnectionDetails mapConnectionDetails(
      @Nonnull final QueryContext context,
      @Nonnull final com.linkedin.connection.OrreryConnectionDetails gmsDetails,
      @Nonnull final SecretService secretService) {
    final OrreryConnectionDetails result = new OrreryConnectionDetails();
    result.setType(
        com.linkedin.orrery.graphql.generated.OrreryConnectionDetailsType.valueOf(
            gmsDetails.getType().toString()));
    if (gmsDetails.hasJson() && ConnectionUtils.canManageConnections(context)) {
      result.setJson(
          mapJsonConnectionDetails(
              context.getOperationContext(), gmsDetails.getJson(), secretService));
    }
    if (gmsDetails.hasName()) {
      result.setName(gmsDetails.getName());
    }
    return result;
  }

  private static OrreryJsonConnection mapJsonConnectionDetails(
      @Nullable final OperationContext opContext,
      @Nonnull final com.linkedin.connection.OrreryJsonConnection gmsJsonConnection,
      @Nonnull final SecretService secretService) {
    final OrreryJsonConnection result = new OrreryJsonConnection();
    // Browsers should never receive decrypted connection blobs — they contain raw credentials.
    // Skip decryption for human agents; the SecretService guard acts as a backstop.
    if (opContext != null
        && opContext.getRequestContext() != null
        && opContext.getRequestContext().getAgentClass().isHuman()) {
      return result;
    }
    result.setBlob(secretService.decrypt(opContext, gmsJsonConnection.getEncryptedBlob()));
    return result;
  }

  private static DataPlatform mapPlatform(final DataPlatformInstance platformInstance) {
    // Set dummy platform to be resolved.
    final DataPlatform partialPlatform = new DataPlatform();
    partialPlatform.setUrn(platformInstance.getPlatform().toString());
    return partialPlatform;
  }

  private static boolean hasAspects(@Nonnull final EntityResponse response) {
    return response.hasAspects()
        && response.getAspects().containsKey(Constants.ORRERY_CONNECTION_DETAILS_ASPECT_NAME)
        && response.getAspects().containsKey(Constants.DATA_PLATFORM_INSTANCE_ASPECT_NAME);
  }

  private ConnectionMapper() {}
}
