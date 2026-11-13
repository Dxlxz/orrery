package com.linkedin.orrery.graphql.resolvers.file;

import static com.linkedin.orrery.graphql.resolvers.ResolverUtils.bindArgument;

import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.concurrency.GraphQLConcurrencyUtils;
import com.linkedin.orrery.graphql.generated.CreateOrreryFileInput;
import com.linkedin.orrery.graphql.generated.CreateOrreryFileResponse;
import com.linkedin.orrery.graphql.generated.OrreryFile;
import com.linkedin.orrery.graphql.types.file.OrreryFileMapper;
import com.linkedin.entity.EntityResponse;
import com.linkedin.metadata.Constants;
import com.linkedin.metadata.config.ObjectStorageConfiguration;
import com.linkedin.metadata.service.OrreryFileService;
import com.linkedin.metadata.utils.objectstorage.ObjectStorageClient;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nullable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class CreateOrreryFileResolver
    implements DataFetcher<CompletableFuture<CreateOrreryFileResponse>> {

  private final OrreryFileService _orreryFileService;
  @Nullable private final ObjectStorageClient _objectStorageClient;
  private final ObjectStorageConfiguration _objectStorageConfiguration;

  @Override
  public CompletableFuture<CreateOrreryFileResponse> get(DataFetchingEnvironment environment)
      throws Exception {
    final QueryContext context = environment.getContext();
    final CreateOrreryFileInput input =
        bindArgument(environment.getArgument("input"), CreateOrreryFileInput.class);

    if (_objectStorageClient == null || !_objectStorageClient.isConfigured()) {
      throw new IllegalArgumentException("Object storage is not configured");
    }
    String storageBucket = _objectStorageClient.storageBucket();
    if (storageBucket == null || storageBucket.isEmpty()) {
      throw new IllegalArgumentException("Object storage bucket is not configured");
    }

    String id = input.getId();
    // Rebuild the key GetPresignedUploadUrlResolver uploaded to rather than trusting the client's
    // storageKey: it ignores a custom asset path prefix, and entity deletion deletes this key.
    String storageKey =
        String.format(
            "%s/%s%s%s",
            _objectStorageConfiguration.getAssetPathPrefix(),
            id,
            Constants.S3_FILE_ID_NAME_SEPARATOR,
            input.getOriginalFileName());

    com.linkedin.file.BucketStorageLocation bucketStorageLocation =
        new com.linkedin.file.BucketStorageLocation();
    bucketStorageLocation.setStorageBucket(storageBucket);
    bucketStorageLocation.setStorageKey(storageKey);

    String originalFileName = input.getOriginalFileName();
    String mimeType = input.getMimeType();
    Long sizeInBytes = input.getSizeInBytes();
    com.linkedin.file.FileUploadScenario scenario =
        com.linkedin.file.FileUploadScenario.valueOf(input.getScenario().toString());
    String contentHash = input.getContentHash();

    Urn referencedByAsset = null;
    if (input.getReferencedByAsset() != null) {
      referencedByAsset = UrnUtils.getUrn(input.getReferencedByAsset());
    }

    Urn schemaField = null;
    if (input.getSchemaField() != null) {
      schemaField = UrnUtils.getUrn(input.getSchemaField());
    }

    final Urn finalReferencedByAsset = referencedByAsset;
    final Urn finalSchemaField = schemaField;
    return GraphQLConcurrencyUtils.supplyAsync(
        () -> {
          try {
            final Urn fileUrn =
                _orreryFileService.createOrreryFile(
                    context.getOperationContext(),
                    id,
                    bucketStorageLocation,
                    originalFileName,
                    mimeType,
                    sizeInBytes,
                    scenario,
                    finalReferencedByAsset,
                    finalSchemaField,
                    contentHash);

            EntityResponse response =
                _orreryFileService.getOrreryFileEntityResponse(
                    context.getOperationContext(), fileUrn);

            OrreryFile file = OrreryFileMapper.map(context, response);

            CreateOrreryFileResponse result = new CreateOrreryFileResponse();
            result.setFile(file);
            return result;
          } catch (Exception e) {
            throw new RuntimeException(
                String.format("Failed to create Orrery file with id %s", id), e);
          }
        },
        this.getClass().getSimpleName(),
        "get");
  }
}
