package com.linkedin.metadata.service;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import com.linkedin.common.AuditStamp;
import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.EnvelopedAspect;
import com.linkedin.entity.EnvelopedAspectMap;
import com.linkedin.entity.client.EntityClient;
import com.linkedin.file.OrreryFileInfo;
import com.linkedin.file.FileUploadScenario;
import com.linkedin.metadata.Constants;
import com.linkedin.metadata.key.OrreryFileKey;
import com.linkedin.metadata.utils.EntityKeyUtils;
import io.orreryproject.metadata.context.OperationContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class OrreryFileServiceTest {
  private EntityClient mockEntityClient;
  private OrreryFileService service;
  private OperationContext mockOpContext;
  private Urn fileUrn;
  private OrreryFileKey key;

  @BeforeMethod
  public void setup() throws Exception {
    mockEntityClient = mock(EntityClient.class);
    service = new OrreryFileService(mockEntityClient);
    mockOpContext = mock(OperationContext.class);
    key = new OrreryFileKey().setId("test-file-id");
    fileUrn = EntityKeyUtils.convertEntityKeyToUrn(key, Constants.ORRERY_FILE_ENTITY_NAME);
    when(mockOpContext.getAuditStamp()).thenReturn(new AuditStamp());
  }

  @Test
  public void testCreateOrreryFileSuccess() throws Exception {
    when(mockEntityClient.exists(any(), any())).thenReturn(false);
    when(mockEntityClient.batchIngestProposals(any(), anyList(), eq(false))).thenReturn(null);

    com.linkedin.file.BucketStorageLocation location =
        new com.linkedin.file.BucketStorageLocation();
    location.setStorageBucket("my-bucket");
    location.setStorageKey("path/to/file.png");

    Urn urn =
        service.createOrreryFile(
            mockOpContext,
            "test-file-id",
            location,
            "file.png",
            "image/png",
            1024L,
            FileUploadScenario.ASSET_DOCUMENTATION,
            null,
            null,
            null);

    assertNotNull(urn);
    assertEquals(urn.toString(), fileUrn.toString());
    verify(mockEntityClient, times(1)).exists(any(), eq(fileUrn));
    verify(mockEntityClient, times(1)).batchIngestProposals(any(), anyList(), eq(false));
  }

  @Test
  public void testCreateOrreryFileWithReferences() throws Exception {
    when(mockEntityClient.exists(any(), any())).thenReturn(false);
    when(mockEntityClient.batchIngestProposals(any(), anyList(), eq(false))).thenReturn(null);

    Urn assetUrn = UrnUtils.getUrn("urn:li:dataset:(urn:li:dataPlatform:test,test,PROD)");
    Urn schemaFieldUrn =
        UrnUtils.getUrn(
            "urn:li:schemaField:(urn:li:dataset:(urn:li:dataPlatform:test,test,PROD),testField)");

    com.linkedin.file.BucketStorageLocation location =
        new com.linkedin.file.BucketStorageLocation();
    location.setStorageBucket("my-bucket");
    location.setStorageKey("path/to/file.png");

    Urn urn =
        service.createOrreryFile(
            mockOpContext,
            "test-file-id",
            location,
            "file.png",
            "image/png",
            1024L,
            FileUploadScenario.ASSET_DOCUMENTATION,
            assetUrn,
            schemaFieldUrn,
            "abc123hash");

    assertNotNull(urn);
    assertEquals(urn.toString(), fileUrn.toString());
    verify(mockEntityClient, times(1)).exists(any(), eq(fileUrn));
    verify(mockEntityClient, times(1)).batchIngestProposals(any(), anyList(), eq(false));
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void testCreateOrreryFileAlreadyExists() throws Exception {
    when(mockEntityClient.exists(any(), any())).thenReturn(true);

    com.linkedin.file.BucketStorageLocation location =
        new com.linkedin.file.BucketStorageLocation();
    location.setStorageBucket("my-bucket");
    location.setStorageKey("path/to/file.png");

    service.createOrreryFile(
        mockOpContext,
        "test-file-id",
        location,
        "file.png",
        "image/png",
        1024L,
        FileUploadScenario.ASSET_DOCUMENTATION,
        null,
        null,
        null);
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void testCreateOrreryFileFailure() throws Exception {
    when(mockEntityClient.exists(any(), any())).thenReturn(false);
    doThrow(new RuntimeException("fail"))
        .when(mockEntityClient)
        .batchIngestProposals(any(), anyList(), eq(false));

    com.linkedin.file.BucketStorageLocation location =
        new com.linkedin.file.BucketStorageLocation();
    location.setStorageBucket("my-bucket");
    location.setStorageKey("path/to/file.png");

    service.createOrreryFile(
        mockOpContext,
        "test-file-id",
        location,
        "file.png",
        "image/png",
        1024L,
        FileUploadScenario.ASSET_DOCUMENTATION,
        null,
        null,
        null);
  }

  @Test
  public void testFileExistsTrue() throws Exception {
    when(mockEntityClient.exists(any(), eq(fileUrn))).thenReturn(true);

    boolean exists = service.fileExists(mockOpContext, fileUrn);

    assertTrue(exists);
    verify(mockEntityClient, times(1)).exists(any(), eq(fileUrn));
  }

  @Test
  public void testFileExistsFalse() throws Exception {
    when(mockEntityClient.exists(any(), eq(fileUrn))).thenReturn(false);

    boolean exists = service.fileExists(mockOpContext, fileUrn);

    assertFalse(exists);
    verify(mockEntityClient, times(1)).exists(any(), eq(fileUrn));
  }

  @Test
  public void testGetOrreryFileInfoFound() throws Exception {
    com.linkedin.file.BucketStorageLocation location =
        new com.linkedin.file.BucketStorageLocation();
    location.setStorageBucket("my-bucket");
    location.setStorageKey("path/to/file.png");

    OrreryFileInfo fileInfo = new OrreryFileInfo();
    fileInfo.setBucketStorageLocation(location);
    fileInfo.setOriginalFileName("file.png");
    fileInfo.setMimeType("image/png");
    fileInfo.setSizeInBytes(1024L);
    fileInfo.setScenario(FileUploadScenario.ASSET_DOCUMENTATION);
    fileInfo.setCreated(new AuditStamp());

    EntityResponse response = mock(EntityResponse.class);
    EnvelopedAspectMap aspectMap = new EnvelopedAspectMap();
    EnvelopedAspect aspect = new EnvelopedAspect();
    aspect.setValue(new com.linkedin.entity.Aspect(fileInfo.data()));
    aspectMap.put(Constants.ORRERY_FILE_INFO_ASPECT_NAME, aspect);
    when(response.getAspects()).thenReturn(aspectMap);
    when(mockEntityClient.getV2(any(), anyString(), any(), any(), eq(false))).thenReturn(response);

    OrreryFileInfo result = service.getOrreryFileInfo(mockOpContext, fileUrn);

    assertNotNull(result);
    assertEquals(result.getBucketStorageLocation().getStorageBucket(), "my-bucket");
    assertEquals(result.getBucketStorageLocation().getStorageKey(), "path/to/file.png");
    assertEquals(result.getOriginalFileName(), "file.png");
  }

  @Test
  public void testGetOrreryFileInfoNotFound() throws Exception {
    EntityResponse response = mock(EntityResponse.class);
    EnvelopedAspectMap emptyAspectMap = new EnvelopedAspectMap();
    when(response.getAspects()).thenReturn(emptyAspectMap);
    when(mockEntityClient.getV2(any(), anyString(), any(), any(), eq(false))).thenReturn(response);

    OrreryFileInfo result = service.getOrreryFileInfo(mockOpContext, fileUrn);

    assertNull(result);
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void testGetOrreryFileEntityResponseThrows() throws Exception {
    when(mockEntityClient.getV2(any(), anyString(), any(), any(), eq(false)))
        .thenThrow(new RuntimeException("fail"));

    service.getOrreryFileEntityResponse(mockOpContext, fileUrn);
  }

  @Test
  public void testDeleteOrreryFileSuccess() throws Exception {
    doNothing().when(mockEntityClient).deleteEntity(any(), any());

    service.deleteOrreryFile(mockOpContext, fileUrn);

    verify(mockEntityClient, times(1)).deleteEntity(mockOpContext, fileUrn);
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void testDeleteOrreryFileFailure() throws Exception {
    doThrow(new RuntimeException("Test exception"))
        .when(mockEntityClient)
        .deleteEntity(any(), any());

    service.deleteOrreryFile(mockOpContext, fileUrn);
  }

  @Test(expectedExceptions = NullPointerException.class)
  public void testDeleteOrreryFileWithNullUrn() throws Exception {
    service.deleteOrreryFile(mockOpContext, null);
  }
}
