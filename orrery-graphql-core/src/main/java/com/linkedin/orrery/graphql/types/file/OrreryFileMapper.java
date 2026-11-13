package com.linkedin.orrery.graphql.types.file;

import static com.linkedin.orrery.graphql.authorization.AuthorizationUtils.canView;
import static com.linkedin.metadata.Constants.ORRERY_FILE_INFO_ASPECT_NAME;

import com.linkedin.common.urn.Urn;
import com.linkedin.data.DataMap;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.authorization.AuthorizationUtils;
import com.linkedin.orrery.graphql.generated.OrreryFile;
import com.linkedin.orrery.graphql.generated.Entity;
import com.linkedin.orrery.graphql.generated.EntityType;
import com.linkedin.orrery.graphql.generated.SchemaFieldEntity;
import com.linkedin.orrery.graphql.types.common.mappers.UrnToEntityMapper;
import com.linkedin.orrery.graphql.types.common.mappers.util.MappingHelper;
import com.linkedin.orrery.graphql.types.mappers.MapperUtils;
import com.linkedin.orrery.graphql.types.mappers.ModelMapper;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.EnvelopedAspectMap;
import com.linkedin.file.OrreryFileInfo;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrreryFileMapper implements ModelMapper<EntityResponse, OrreryFile> {

  public static final OrreryFileMapper INSTANCE = new OrreryFileMapper();

  public static OrreryFile map(
      @Nullable final QueryContext context, @Nonnull final EntityResponse entityResponse) {
    return INSTANCE.apply(context, entityResponse);
  }

  @Override
  public OrreryFile apply(
      @Nullable final QueryContext context, @Nonnull final EntityResponse entityResponse) {
    final OrreryFile result = new OrreryFile();
    Urn entityUrn = entityResponse.getUrn();

    result.setUrn(entityResponse.getUrn().toString());
    result.setType(EntityType.ORRERY_FILE);

    EnvelopedAspectMap aspectMap = entityResponse.getAspects();

    // Handle getting deleted file by broken reference (check if required aspect was fetched)
    if (aspectMap.get(ORRERY_FILE_INFO_ASPECT_NAME) == null) {
      log.warn("OrreryFile {} doesn't have required aspects", entityUrn);
      return null;
    }

    MappingHelper<OrreryFile> mappingHelper = new MappingHelper<>(aspectMap, result);
    mappingHelper.mapToResult(
        ORRERY_FILE_INFO_ASPECT_NAME, (file, dataMap) -> mapFileInfo(file, dataMap));

    if (context != null && !canView(context.getOperationContext(), entityUrn)) {
      return AuthorizationUtils.restrictEntity(result, OrreryFile.class);
    } else {
      return result;
    }
  }

  private void mapFileInfo(@Nonnull OrreryFile file, @Nonnull DataMap dataMap) {
    OrreryFileInfo gmsFileInfo = new OrreryFileInfo(dataMap);
    com.linkedin.orrery.graphql.generated.OrreryFileInfo graphqlFileInfo =
        new com.linkedin.orrery.graphql.generated.OrreryFileInfo();

    // Map bucket storage location
    if (gmsFileInfo.getBucketStorageLocation() != null) {
      com.linkedin.file.BucketStorageLocation gmsLocation = gmsFileInfo.getBucketStorageLocation();
      com.linkedin.orrery.graphql.generated.BucketStorageLocation graphqlLocation =
          new com.linkedin.orrery.graphql.generated.BucketStorageLocation();
      graphqlLocation.setStorageBucket(gmsLocation.getStorageBucket());
      graphqlLocation.setStorageKey(gmsLocation.getStorageKey());
      graphqlFileInfo.setBucketStorageLocation(graphqlLocation);
    }

    graphqlFileInfo.setOriginalFileName(gmsFileInfo.getOriginalFileName());
    graphqlFileInfo.setMimeType(gmsFileInfo.getMimeType());
    graphqlFileInfo.setSizeInBytes(gmsFileInfo.getSizeInBytes());
    graphqlFileInfo.setScenario(
        com.linkedin.orrery.graphql.generated.UploadDownloadScenario.valueOf(
            gmsFileInfo.getScenario().toString()));

    if (gmsFileInfo.getReferencedByAsset() != null) {
      Urn assetUrn = gmsFileInfo.getReferencedByAsset();
      Entity partialEntity = UrnToEntityMapper.map(null, assetUrn);
      graphqlFileInfo.setReferencedByAsset(partialEntity);
    }

    if (gmsFileInfo.getSchemaField() != null) {
      Urn schemaFieldUrn = gmsFileInfo.getSchemaField();
      SchemaFieldEntity partialSchemaField = new SchemaFieldEntity();
      partialSchemaField.setUrn(schemaFieldUrn.toString());
      partialSchemaField.setType(EntityType.SCHEMA_FIELD);
      graphqlFileInfo.setSchemaField(partialSchemaField);
    }

    if (gmsFileInfo.hasCreated()) {
      graphqlFileInfo.setCreated(MapperUtils.createResolvedAuditStamp(gmsFileInfo.getCreated()));
    }

    file.setInfo(graphqlFileInfo);
  }
}
