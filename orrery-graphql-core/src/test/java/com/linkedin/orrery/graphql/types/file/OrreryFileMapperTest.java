package com.linkedin.orrery.graphql.types.file;

import static org.testng.Assert.*;

import com.linkedin.common.AuditStamp;
import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.orrery.graphql.generated.OrreryFile;
import com.linkedin.orrery.graphql.generated.EntityType;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.EnvelopedAspect;
import com.linkedin.entity.EnvelopedAspectMap;
import com.linkedin.file.OrreryFileInfo;
import com.linkedin.file.FileUploadScenario;
import org.testng.annotations.Test;

public class OrreryFileMapperTest {

  @Test
  public void testOrreryFileMapper() throws Exception {
    // Create test data
    Urn fileUrn = UrnUtils.getUrn("urn:li:orreryFile:test-file-123");

    // Create BucketStorageLocation
    com.linkedin.file.BucketStorageLocation location =
        new com.linkedin.file.BucketStorageLocation();
    location.setStorageBucket("my-bucket");
    location.setStorageKey("path/to/file.png");

    // Create GMS file info
    OrreryFileInfo gmsFileInfo = new OrreryFileInfo();
    gmsFileInfo.setBucketStorageLocation(location);
    gmsFileInfo.setOriginalFileName("file.png");
    gmsFileInfo.setMimeType("image/png");
    gmsFileInfo.setSizeInBytes(1024L);
    gmsFileInfo.setScenario(FileUploadScenario.ASSET_DOCUMENTATION);

    // Create audit stamp
    AuditStamp created = new AuditStamp();
    created.setTime(System.currentTimeMillis());
    created.setActor(UrnUtils.getUrn("urn:li:corpuser:test-user"));
    gmsFileInfo.setCreated(created);

    // Create entity response
    EntityResponse entityResponse = new EntityResponse();
    entityResponse.setUrn(fileUrn);

    EnvelopedAspectMap aspectMap = new EnvelopedAspectMap();
    EnvelopedAspect aspect = new EnvelopedAspect();
    aspect.setValue(new com.linkedin.entity.Aspect(gmsFileInfo.data()));
    aspectMap.put("orreryFileInfo", aspect);
    entityResponse.setAspects(aspectMap);

    // Test mapping
    OrreryFile result = OrreryFileMapper.map(null, entityResponse);

    // Verify basic fields
    assertEquals(result.getUrn(), fileUrn.toString());
    assertEquals(result.getType(), EntityType.ORRERY_FILE);

    // Verify info
    assertNotNull(result.getInfo());
    assertNotNull(result.getInfo().getBucketStorageLocation());
    assertEquals(result.getInfo().getBucketStorageLocation().getStorageBucket(), "my-bucket");
    assertEquals(result.getInfo().getBucketStorageLocation().getStorageKey(), "path/to/file.png");
    assertEquals(result.getInfo().getOriginalFileName(), "file.png");
    assertEquals(result.getInfo().getMimeType(), "image/png");
    assertEquals(result.getInfo().getSizeInBytes(), Long.valueOf(1024L));
    assertEquals(
        result.getInfo().getScenario(),
        com.linkedin.orrery.graphql.generated.UploadDownloadScenario.ASSET_DOCUMENTATION);

    // Verify audit stamp
    assertNotNull(result.getInfo().getCreated());
    assertEquals(result.getInfo().getCreated().getTime(), created.getTime());
    assertNotNull(result.getInfo().getCreated().getActor());
    assertEquals(result.getInfo().getCreated().getActor().getUrn(), created.getActor().toString());
  }

  @Test
  public void testOrreryFileMapperWithReferences() throws Exception {
    // Create test data
    Urn fileUrn = UrnUtils.getUrn("urn:li:orreryFile:test-file-123");
    Urn assetUrn = UrnUtils.getUrn("urn:li:dataset:(urn:li:dataPlatform:test,test,PROD)");
    Urn schemaFieldUrn =
        UrnUtils.getUrn(
            "urn:li:schemaField:(urn:li:dataset:(urn:li:dataPlatform:test,test,PROD),testField)");

    // Create BucketStorageLocation
    com.linkedin.file.BucketStorageLocation location =
        new com.linkedin.file.BucketStorageLocation();
    location.setStorageBucket("my-bucket");
    location.setStorageKey("path/to/file.png");

    // Create GMS file info
    OrreryFileInfo gmsFileInfo = new OrreryFileInfo();
    gmsFileInfo.setBucketStorageLocation(location);
    gmsFileInfo.setOriginalFileName("file.png");
    gmsFileInfo.setMimeType("image/png");
    gmsFileInfo.setSizeInBytes(1024L);
    gmsFileInfo.setScenario(FileUploadScenario.ASSET_DOCUMENTATION);
    gmsFileInfo.setReferencedByAsset(assetUrn);
    gmsFileInfo.setSchemaField(schemaFieldUrn);

    // Create audit stamp
    AuditStamp created = new AuditStamp();
    created.setTime(System.currentTimeMillis());
    created.setActor(UrnUtils.getUrn("urn:li:corpuser:test-user"));
    gmsFileInfo.setCreated(created);

    // Create entity response
    EntityResponse entityResponse = new EntityResponse();
    entityResponse.setUrn(fileUrn);

    EnvelopedAspectMap aspectMap = new EnvelopedAspectMap();
    EnvelopedAspect aspect = new EnvelopedAspect();
    aspect.setValue(new com.linkedin.entity.Aspect(gmsFileInfo.data()));
    aspectMap.put("orreryFileInfo", aspect);
    entityResponse.setAspects(aspectMap);

    // Test mapping
    OrreryFile result = OrreryFileMapper.map(null, entityResponse);

    // Verify references
    assertNotNull(result.getInfo().getReferencedByAsset());
    assertEquals(result.getInfo().getReferencedByAsset().getUrn(), assetUrn.toString());
    assertEquals(result.getInfo().getReferencedByAsset().getType(), EntityType.DATASET);

    assertNotNull(result.getInfo().getSchemaField());
    assertEquals(result.getInfo().getSchemaField().getUrn(), schemaFieldUrn.toString());
    assertEquals(result.getInfo().getSchemaField().getType(), EntityType.SCHEMA_FIELD);
  }

  @Test
  public void testOrreryFileMapperWithOptionalFields() throws Exception {
    // Create test data
    Urn fileUrn = UrnUtils.getUrn("urn:li:orreryFile:test-file-123");

    // Create BucketStorageLocation
    com.linkedin.file.BucketStorageLocation location =
        new com.linkedin.file.BucketStorageLocation();
    location.setStorageBucket("my-bucket");
    location.setStorageKey("path/to/file.png");

    // Create GMS file info with optional fields (contentHash)
    OrreryFileInfo gmsFileInfo = new OrreryFileInfo();
    gmsFileInfo.setBucketStorageLocation(location);
    gmsFileInfo.setOriginalFileName("file.png");
    gmsFileInfo.setMimeType("image/png");
    gmsFileInfo.setSizeInBytes(1024L);
    gmsFileInfo.setScenario(FileUploadScenario.ASSET_DOCUMENTATION);
    gmsFileInfo.setContentHash("abc123def456");

    // Create audit stamp
    AuditStamp created = new AuditStamp();
    created.setTime(System.currentTimeMillis());
    created.setActor(UrnUtils.getUrn("urn:li:corpuser:test-user"));
    gmsFileInfo.setCreated(created);

    // Create entity response
    EntityResponse entityResponse = new EntityResponse();
    entityResponse.setUrn(fileUrn);

    EnvelopedAspectMap aspectMap = new EnvelopedAspectMap();
    EnvelopedAspect aspect = new EnvelopedAspect();
    aspect.setValue(new com.linkedin.entity.Aspect(gmsFileInfo.data()));
    aspectMap.put("orreryFileInfo", aspect);
    entityResponse.setAspects(aspectMap);

    // Test mapping
    OrreryFile result = OrreryFileMapper.map(null, entityResponse);

    // Verify basic fields work with optional contentHash
    assertNotNull(result.getInfo());
    assertNotNull(result.getInfo().getBucketStorageLocation());
    assertEquals(result.getInfo().getBucketStorageLocation().getStorageBucket(), "my-bucket");
    assertEquals(result.getInfo().getOriginalFileName(), "file.png");
  }

  @Test
  public void testOrreryFileMapperMissingAspect() {
    // Create test data with no aspects
    Urn fileUrn = UrnUtils.getUrn("urn:li:orreryFile:test-file-123");
    EntityResponse entityResponse = new EntityResponse();
    entityResponse.setUrn(fileUrn);
    entityResponse.setAspects(new EnvelopedAspectMap());

    // Map the file
    OrreryFile result = OrreryFileMapper.map(null, entityResponse);

    // Should return null when required aspect is missing
    assertNull(result);
  }

  @Test
  public void testOrreryFileMapperWithNullReferences() throws Exception {
    // Create test data
    Urn fileUrn = UrnUtils.getUrn("urn:li:orreryFile:test-file-123");

    // Create BucketStorageLocation
    com.linkedin.file.BucketStorageLocation location =
        new com.linkedin.file.BucketStorageLocation();
    location.setStorageBucket("my-bucket");
    location.setStorageKey("path/to/file.png");

    // Create GMS file info without references
    OrreryFileInfo gmsFileInfo = new OrreryFileInfo();
    gmsFileInfo.setBucketStorageLocation(location);
    gmsFileInfo.setOriginalFileName("file.png");
    gmsFileInfo.setMimeType("image/png");
    gmsFileInfo.setSizeInBytes(1024L);
    gmsFileInfo.setScenario(FileUploadScenario.ASSET_DOCUMENTATION);

    // Create audit stamp
    AuditStamp created = new AuditStamp();
    created.setTime(System.currentTimeMillis());
    created.setActor(UrnUtils.getUrn("urn:li:corpuser:test-user"));
    gmsFileInfo.setCreated(created);

    // Create entity response
    EntityResponse entityResponse = new EntityResponse();
    entityResponse.setUrn(fileUrn);

    EnvelopedAspectMap aspectMap = new EnvelopedAspectMap();
    EnvelopedAspect aspect = new EnvelopedAspect();
    aspect.setValue(new com.linkedin.entity.Aspect(gmsFileInfo.data()));
    aspectMap.put("orreryFileInfo", aspect);
    entityResponse.setAspects(aspectMap);

    // Test mapping
    OrreryFile result = OrreryFileMapper.map(null, entityResponse);

    // Verify references are null
    assertNull(result.getInfo().getReferencedByAsset());
    assertNull(result.getInfo().getSchemaField());
  }
}
