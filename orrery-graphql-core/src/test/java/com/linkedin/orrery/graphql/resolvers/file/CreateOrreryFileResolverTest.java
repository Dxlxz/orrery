package com.linkedin.orrery.graphql.resolvers.file;

import static com.linkedin.orrery.graphql.TestUtils.getMockAllowContext;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import com.linkedin.common.AuditStamp;
import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.CreateOrreryFileInput;
import com.linkedin.orrery.graphql.generated.CreateOrreryFileResponse;
import com.linkedin.orrery.graphql.generated.UploadDownloadScenario;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.EnvelopedAspect;
import com.linkedin.entity.EnvelopedAspectMap;
import com.linkedin.file.BucketStorageLocation;
import com.linkedin.file.OrreryFileInfo;
import com.linkedin.file.FileUploadScenario;
import com.linkedin.metadata.config.ObjectStorageConfiguration;
import com.linkedin.metadata.service.OrreryFileService;
import com.linkedin.metadata.utils.objectstorage.ObjectStorageClient;
import graphql.schema.DataFetchingEnvironment;
import java.util.concurrent.CompletionException;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.Test;

public class CreateOrreryFileResolverTest {
  private static final String TEST_FILE_ID = "test-file-123";
  private static final String TEST_FILE_URN = "urn:li:orreryFile:test-file-123";
  private static final String TEST_STORAGE_BUCKET = "my-bucket";
  private static final String TEST_STORAGE_KEY = "path/to/file.png";
  private static final String TEST_FILE_NAME = "file.png";
  private static final String TEST_MIME_TYPE = "image/png";
  private static final Long TEST_SIZE = 1024L;

  private static ObjectStorageConfiguration storageConfig(String assetPathPrefix) {
    ObjectStorageConfiguration config = new ObjectStorageConfiguration();
    config.setAssetPathPrefix(assetPathPrefix);
    return config;
  }

  @Test
  public void testStorageKeyBuiltFromConfiguredPrefixNotClientInput() throws Exception {
    OrreryFileService mockService = mock(OrreryFileService.class);
    ObjectStorageClient mockObjectStorageClient = mock(ObjectStorageClient.class);
    when(mockObjectStorageClient.isConfigured()).thenReturn(true);
    when(mockObjectStorageClient.storageBucket()).thenReturn(TEST_STORAGE_BUCKET);

    CreateOrreryFileResolver resolver =
        new CreateOrreryFileResolver(
            mockService, mockObjectStorageClient, storageConfig("team/documentation"));

    QueryContext mockContext = getMockAllowContext();
    DataFetchingEnvironment mockEnv = mock(DataFetchingEnvironment.class);
    CreateOrreryFileInput input = new CreateOrreryFileInput();
    input.setId(TEST_FILE_ID);
    input.setStorageKey("product_assets/some-other-object");
    input.setOriginalFileName(TEST_FILE_NAME);
    input.setMimeType(TEST_MIME_TYPE);
    input.setSizeInBytes(TEST_SIZE);
    input.setScenario(UploadDownloadScenario.ASSET_DOCUMENTATION);
    when(mockEnv.getArgument(eq("input"))).thenReturn(input);
    when(mockEnv.getContext()).thenReturn(mockContext);
    when(mockService.createOrreryFile(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenThrow(new RuntimeException("stop after capture"));

    assertThrows(CompletionException.class, () -> resolver.get(mockEnv).join());

    ArgumentCaptor<BucketStorageLocation> location =
        ArgumentCaptor.forClass(BucketStorageLocation.class);
    verify(mockService)
        .createOrreryFile(
            any(), any(), location.capture(), any(), any(), any(), any(), any(), any(), any());
    assertEquals(location.getValue().getStorageBucket(), TEST_STORAGE_BUCKET);
    assertEquals(location.getValue().getStorageKey(), "team/documentation/test-file-123__file.png");
  }

  @Test
  public void testGetSuccess() throws Exception {
    OrreryFileService mockService = mock(OrreryFileService.class);
    ObjectStorageClient mockObjectStorageClient = mock(ObjectStorageClient.class);
    when(mockObjectStorageClient.isConfigured()).thenReturn(true);
    when(mockObjectStorageClient.storageBucket()).thenReturn(TEST_STORAGE_BUCKET);

    CreateOrreryFileResolver resolver =
        new CreateOrreryFileResolver(
            mockService, mockObjectStorageClient, storageConfig("product_assets"));

    QueryContext mockContext = getMockAllowContext();
    DataFetchingEnvironment mockEnv = mock(DataFetchingEnvironment.class);
    CreateOrreryFileInput input = new CreateOrreryFileInput();
    input.setId(TEST_FILE_ID);
    input.setStorageKey(TEST_STORAGE_KEY);
    input.setOriginalFileName(TEST_FILE_NAME);
    input.setMimeType(TEST_MIME_TYPE);
    input.setSizeInBytes(TEST_SIZE);
    input.setScenario(UploadDownloadScenario.ASSET_DOCUMENTATION);

    Urn fileUrn = UrnUtils.getUrn(TEST_FILE_URN);
    when(mockService.createOrreryFile(
            any(),
            eq(TEST_FILE_ID),
            any(),
            eq(TEST_FILE_NAME),
            eq(TEST_MIME_TYPE),
            eq(TEST_SIZE),
            any(),
            eq(null),
            eq(null),
            eq(null)))
        .thenReturn(fileUrn);

    // Mock EntityResponse with a valid aspect map
    com.linkedin.file.BucketStorageLocation location =
        new com.linkedin.file.BucketStorageLocation();
    location.setStorageBucket(TEST_STORAGE_BUCKET);
    location.setStorageKey(TEST_STORAGE_KEY);

    EntityResponse mockResponse = mock(EntityResponse.class);
    OrreryFileInfo fileInfo = new OrreryFileInfo();
    fileInfo.setBucketStorageLocation(location);
    fileInfo.setOriginalFileName(TEST_FILE_NAME);
    fileInfo.setMimeType(TEST_MIME_TYPE);
    fileInfo.setSizeInBytes(TEST_SIZE);
    fileInfo.setScenario(FileUploadScenario.ASSET_DOCUMENTATION);

    AuditStamp auditStamp = new AuditStamp();
    auditStamp.setTime(System.currentTimeMillis());
    auditStamp.setActor(UrnUtils.getUrn("urn:li:corpuser:test-user"));
    fileInfo.setCreated(auditStamp);

    EnvelopedAspectMap aspectMap = new EnvelopedAspectMap();
    EnvelopedAspect aspect = new EnvelopedAspect();
    aspect.setValue(new com.linkedin.entity.Aspect(fileInfo.data()));
    aspectMap.put(com.linkedin.metadata.Constants.ORRERY_FILE_INFO_ASPECT_NAME, aspect);
    when(mockResponse.getAspects()).thenReturn(aspectMap);
    when(mockResponse.getUrn()).thenReturn(fileUrn);
    when(mockService.getOrreryFileEntityResponse(any(), eq(fileUrn))).thenReturn(mockResponse);

    when(mockEnv.getArgument(eq("input"))).thenReturn(input);
    when(mockEnv.getContext()).thenReturn(mockContext);

    CreateOrreryFileResponse response = resolver.get(mockEnv).join();

    assertNotNull(response);
    assertNotNull(response.getFile());
    assertEquals(response.getFile().getUrn(), TEST_FILE_URN);

    verify(mockService, times(1))
        .createOrreryFile(
            any(),
            eq(TEST_FILE_ID),
            any(),
            eq(TEST_FILE_NAME),
            eq(TEST_MIME_TYPE),
            eq(TEST_SIZE),
            any(),
            eq(null),
            eq(null),
            eq(null));
    verify(mockService, times(1)).getOrreryFileEntityResponse(any(), eq(fileUrn));
  }

  @Test
  public void testGetSuccessWithReferences() throws Exception {
    OrreryFileService mockService = mock(OrreryFileService.class);
    ObjectStorageClient mockObjectStorageClient = mock(ObjectStorageClient.class);
    when(mockObjectStorageClient.isConfigured()).thenReturn(true);
    when(mockObjectStorageClient.storageBucket()).thenReturn(TEST_STORAGE_BUCKET);

    CreateOrreryFileResolver resolver =
        new CreateOrreryFileResolver(
            mockService, mockObjectStorageClient, storageConfig("product_assets"));

    QueryContext mockContext = getMockAllowContext();
    DataFetchingEnvironment mockEnv = mock(DataFetchingEnvironment.class);
    CreateOrreryFileInput input = new CreateOrreryFileInput();
    input.setId(TEST_FILE_ID);
    input.setStorageKey(TEST_STORAGE_KEY);
    input.setOriginalFileName(TEST_FILE_NAME);
    input.setMimeType(TEST_MIME_TYPE);
    input.setSizeInBytes(TEST_SIZE);
    input.setScenario(UploadDownloadScenario.ASSET_DOCUMENTATION);
    input.setReferencedByAsset("urn:li:dataset:(urn:li:dataPlatform:test,test,PROD)");
    input.setSchemaField(
        "urn:li:schemaField:(urn:li:dataset:(urn:li:dataPlatform:test,test,PROD),testField)");
    input.setContentHash("abc123hash");

    Urn fileUrn = UrnUtils.getUrn(TEST_FILE_URN);
    Urn assetUrn = UrnUtils.getUrn("urn:li:dataset:(urn:li:dataPlatform:test,test,PROD)");
    Urn schemaFieldUrn =
        UrnUtils.getUrn(
            "urn:li:schemaField:(urn:li:dataset:(urn:li:dataPlatform:test,test,PROD),testField)");

    when(mockService.createOrreryFile(
            any(),
            eq(TEST_FILE_ID),
            any(),
            eq(TEST_FILE_NAME),
            eq(TEST_MIME_TYPE),
            eq(TEST_SIZE),
            any(),
            eq(assetUrn),
            eq(schemaFieldUrn),
            eq("abc123hash")))
        .thenReturn(fileUrn);

    // Mock EntityResponse
    com.linkedin.file.BucketStorageLocation location =
        new com.linkedin.file.BucketStorageLocation();
    location.setStorageBucket(TEST_STORAGE_BUCKET);
    location.setStorageKey(TEST_STORAGE_KEY);

    EntityResponse mockResponse = mock(EntityResponse.class);
    OrreryFileInfo fileInfo = new OrreryFileInfo();
    fileInfo.setBucketStorageLocation(location);
    fileInfo.setOriginalFileName(TEST_FILE_NAME);
    fileInfo.setMimeType(TEST_MIME_TYPE);
    fileInfo.setSizeInBytes(TEST_SIZE);
    fileInfo.setScenario(FileUploadScenario.ASSET_DOCUMENTATION);
    fileInfo.setReferencedByAsset(assetUrn);
    fileInfo.setSchemaField(schemaFieldUrn);
    fileInfo.setContentHash("abc123hash");

    AuditStamp auditStamp = new AuditStamp();
    auditStamp.setTime(System.currentTimeMillis());
    auditStamp.setActor(UrnUtils.getUrn("urn:li:corpuser:test-user"));
    fileInfo.setCreated(auditStamp);

    EnvelopedAspectMap aspectMap = new EnvelopedAspectMap();
    EnvelopedAspect aspect = new EnvelopedAspect();
    aspect.setValue(new com.linkedin.entity.Aspect(fileInfo.data()));
    aspectMap.put(com.linkedin.metadata.Constants.ORRERY_FILE_INFO_ASPECT_NAME, aspect);
    when(mockResponse.getAspects()).thenReturn(aspectMap);
    when(mockResponse.getUrn()).thenReturn(fileUrn);
    when(mockService.getOrreryFileEntityResponse(any(), eq(fileUrn))).thenReturn(mockResponse);

    when(mockEnv.getArgument(eq("input"))).thenReturn(input);
    when(mockEnv.getContext()).thenReturn(mockContext);

    CreateOrreryFileResponse response = resolver.get(mockEnv).join();

    assertNotNull(response);
    assertNotNull(response.getFile());
    assertEquals(response.getFile().getUrn(), TEST_FILE_URN);

    verify(mockService, times(1))
        .createOrreryFile(
            any(),
            eq(TEST_FILE_ID),
            any(),
            eq(TEST_FILE_NAME),
            eq(TEST_MIME_TYPE),
            eq(TEST_SIZE),
            any(),
            eq(assetUrn),
            eq(schemaFieldUrn),
            eq("abc123hash"));
    verify(mockService, times(1)).getOrreryFileEntityResponse(any(), eq(fileUrn));
  }

  @Test
  public void testGetThrowsException() throws Exception {
    OrreryFileService mockService = mock(OrreryFileService.class);
    ObjectStorageClient mockObjectStorageClient = mock(ObjectStorageClient.class);
    when(mockObjectStorageClient.isConfigured()).thenReturn(true);
    when(mockObjectStorageClient.storageBucket()).thenReturn(TEST_STORAGE_BUCKET);

    CreateOrreryFileResolver resolver =
        new CreateOrreryFileResolver(
            mockService, mockObjectStorageClient, storageConfig("product_assets"));

    QueryContext mockContext = getMockAllowContext();
    DataFetchingEnvironment mockEnv = mock(DataFetchingEnvironment.class);
    CreateOrreryFileInput input = new CreateOrreryFileInput();
    input.setId(TEST_FILE_ID);
    input.setStorageKey(TEST_STORAGE_KEY);
    input.setOriginalFileName(TEST_FILE_NAME);
    input.setMimeType(TEST_MIME_TYPE);
    input.setSizeInBytes(TEST_SIZE);
    input.setScenario(UploadDownloadScenario.ASSET_DOCUMENTATION);

    when(mockService.createOrreryFile(
            any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
        .thenThrow(new RuntimeException("File already exists"));
    when(mockEnv.getArgument(eq("input"))).thenReturn(input);
    when(mockEnv.getContext()).thenReturn(mockContext);

    assertThrows(CompletionException.class, () -> resolver.get(mockEnv).join());

    verify(mockService, times(1))
        .createOrreryFile(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  public void testGetWithNullOptionalFields() throws Exception {
    OrreryFileService mockService = mock(OrreryFileService.class);
    ObjectStorageClient mockObjectStorageClient = mock(ObjectStorageClient.class);
    when(mockObjectStorageClient.isConfigured()).thenReturn(true);
    when(mockObjectStorageClient.storageBucket()).thenReturn(TEST_STORAGE_BUCKET);

    CreateOrreryFileResolver resolver =
        new CreateOrreryFileResolver(
            mockService, mockObjectStorageClient, storageConfig("product_assets"));

    QueryContext mockContext = getMockAllowContext();
    DataFetchingEnvironment mockEnv = mock(DataFetchingEnvironment.class);
    CreateOrreryFileInput input = new CreateOrreryFileInput();
    input.setId(TEST_FILE_ID);
    input.setStorageKey(TEST_STORAGE_KEY);
    input.setOriginalFileName(TEST_FILE_NAME);
    input.setMimeType(TEST_MIME_TYPE);
    input.setSizeInBytes(TEST_SIZE);
    input.setScenario(UploadDownloadScenario.ASSET_DOCUMENTATION);
    // Don't set optional fields

    Urn fileUrn = UrnUtils.getUrn(TEST_FILE_URN);
    when(mockService.createOrreryFile(
            any(),
            eq(TEST_FILE_ID),
            any(),
            eq(TEST_FILE_NAME),
            eq(TEST_MIME_TYPE),
            eq(TEST_SIZE),
            any(),
            eq(null),
            eq(null),
            eq(null)))
        .thenReturn(fileUrn);

    // Mock EntityResponse
    com.linkedin.file.BucketStorageLocation location =
        new com.linkedin.file.BucketStorageLocation();
    location.setStorageBucket(TEST_STORAGE_BUCKET);
    location.setStorageKey(TEST_STORAGE_KEY);

    EntityResponse mockResponse = mock(EntityResponse.class);
    OrreryFileInfo fileInfo = new OrreryFileInfo();
    fileInfo.setBucketStorageLocation(location);
    fileInfo.setOriginalFileName(TEST_FILE_NAME);
    fileInfo.setMimeType(TEST_MIME_TYPE);
    fileInfo.setSizeInBytes(TEST_SIZE);
    fileInfo.setScenario(FileUploadScenario.ASSET_DOCUMENTATION);

    AuditStamp auditStamp = new AuditStamp();
    auditStamp.setTime(System.currentTimeMillis());
    auditStamp.setActor(UrnUtils.getUrn("urn:li:corpuser:test-user"));
    fileInfo.setCreated(auditStamp);

    EnvelopedAspectMap aspectMap = new EnvelopedAspectMap();
    EnvelopedAspect aspect = new EnvelopedAspect();
    aspect.setValue(new com.linkedin.entity.Aspect(fileInfo.data()));
    aspectMap.put(com.linkedin.metadata.Constants.ORRERY_FILE_INFO_ASPECT_NAME, aspect);
    when(mockResponse.getAspects()).thenReturn(aspectMap);
    when(mockResponse.getUrn()).thenReturn(fileUrn);
    when(mockService.getOrreryFileEntityResponse(any(), eq(fileUrn))).thenReturn(mockResponse);

    when(mockEnv.getArgument(eq("input"))).thenReturn(input);
    when(mockEnv.getContext()).thenReturn(mockContext);

    CreateOrreryFileResponse response = resolver.get(mockEnv).join();

    assertNotNull(response);
    assertNotNull(response.getFile());

    verify(mockService, times(1))
        .createOrreryFile(
            any(), any(), any(), any(), any(), any(), any(), eq(null), eq(null), eq(null));
  }

  @Test
  public void testGetFailsWhenObjectStorageClientIsNull() throws Exception {
    OrreryFileService mockService = mock(OrreryFileService.class);
    CreateOrreryFileResolver resolver =
        new CreateOrreryFileResolver(mockService, null, storageConfig("product_assets"));

    QueryContext mockContext = getMockAllowContext();
    DataFetchingEnvironment mockEnv = mock(DataFetchingEnvironment.class);
    CreateOrreryFileInput input = new CreateOrreryFileInput();
    input.setId(TEST_FILE_ID);
    input.setStorageKey(TEST_STORAGE_KEY);
    input.setOriginalFileName(TEST_FILE_NAME);
    input.setMimeType(TEST_MIME_TYPE);
    input.setSizeInBytes(TEST_SIZE);
    input.setScenario(UploadDownloadScenario.ASSET_DOCUMENTATION);

    when(mockEnv.getArgument(eq("input"))).thenReturn(input);
    when(mockEnv.getContext()).thenReturn(mockContext);

    assertThrows(IllegalArgumentException.class, () -> resolver.get(mockEnv).join());
    verify(mockService, never())
        .createOrreryFile(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  public void testGetFailsWhenObjectStorageNotConfigured() throws Exception {
    OrreryFileService mockService = mock(OrreryFileService.class);
    ObjectStorageClient mockObjectStorageClient = mock(ObjectStorageClient.class);
    when(mockObjectStorageClient.isConfigured()).thenReturn(false);

    CreateOrreryFileResolver resolver =
        new CreateOrreryFileResolver(
            mockService, mockObjectStorageClient, storageConfig("product_assets"));

    QueryContext mockContext = getMockAllowContext();
    DataFetchingEnvironment mockEnv = mock(DataFetchingEnvironment.class);
    CreateOrreryFileInput input = new CreateOrreryFileInput();
    input.setId(TEST_FILE_ID);
    input.setStorageKey(TEST_STORAGE_KEY);
    input.setOriginalFileName(TEST_FILE_NAME);
    input.setMimeType(TEST_MIME_TYPE);
    input.setSizeInBytes(TEST_SIZE);
    input.setScenario(UploadDownloadScenario.ASSET_DOCUMENTATION);

    when(mockEnv.getArgument(eq("input"))).thenReturn(input);
    when(mockEnv.getContext()).thenReturn(mockContext);

    assertThrows(IllegalArgumentException.class, () -> resolver.get(mockEnv).join());
    verify(mockService, never())
        .createOrreryFile(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
  }

  @Test
  public void testGetFailsWhenStorageBucketMissing() throws Exception {
    OrreryFileService mockService = mock(OrreryFileService.class);
    ObjectStorageClient mockObjectStorageClient = mock(ObjectStorageClient.class);
    when(mockObjectStorageClient.isConfigured()).thenReturn(true);
    when(mockObjectStorageClient.storageBucket()).thenReturn("");

    CreateOrreryFileResolver resolver =
        new CreateOrreryFileResolver(
            mockService, mockObjectStorageClient, storageConfig("product_assets"));

    QueryContext mockContext = getMockAllowContext();
    DataFetchingEnvironment mockEnv = mock(DataFetchingEnvironment.class);
    CreateOrreryFileInput input = new CreateOrreryFileInput();
    input.setId(TEST_FILE_ID);
    input.setStorageKey(TEST_STORAGE_KEY);
    input.setOriginalFileName(TEST_FILE_NAME);
    input.setMimeType(TEST_MIME_TYPE);
    input.setSizeInBytes(TEST_SIZE);
    input.setScenario(UploadDownloadScenario.ASSET_DOCUMENTATION);

    when(mockEnv.getArgument(eq("input"))).thenReturn(input);
    when(mockEnv.getContext()).thenReturn(mockContext);

    assertThrows(IllegalArgumentException.class, () -> resolver.get(mockEnv).join());
    verify(mockService, never())
        .createOrreryFile(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
  }
}
