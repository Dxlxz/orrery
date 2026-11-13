package com.linkedin.orrery.graphql.types.entitytype;

import static org.testng.Assert.*;

import com.linkedin.orrery.graphql.generated.EntityType;
import com.linkedin.metadata.Constants;
import com.linkedin.metadata.utils.TolerantHandling;
import com.linkedin.metadata.utils.UnknownDataHandlingsForTests;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class EntityTypeUrnMapperTest {

  @BeforeMethod
  public void tolerateUnknownDataByDefault() {
    UnknownDataHandlingsForTests.use(TolerantHandling.INSTANCE);
  }

  @AfterMethod
  public void resetUnknownDataHandling() {
    UnknownDataHandlingsForTests.reset();
  }

  @Test
  public void testGetName() throws Exception {
    assertEquals(
        EntityTypeUrnMapper.getName("urn:li:entityType:orrery.dataset"),
        Constants.DATASET_ENTITY_NAME);
  }

  @Test
  public void testGetEntityType() throws Exception {
    assertEquals(
        EntityTypeUrnMapper.getEntityType("urn:li:entityType:orrery.dataset"), EntityType.DATASET);
  }

  @Test
  public void testGetEntityTypeOrOther() {
    assertEquals(
        EntityTypeUrnMapper.getEntityTypeOrOther("urn:li:entityType:orrery.dataset"),
        EntityType.DATASET);
    assertEquals(
        EntityTypeUrnMapper.getEntityTypeOrOther("urn:li:entityType:orrery.entityFromNewerBuild"),
        EntityType.OTHER);
  }

  @Test
  public void testGetEntityTypeUrn() throws Exception {
    assertEquals(
        EntityTypeUrnMapper.getEntityTypeUrn(Constants.DATASET_ENTITY_NAME),
        "urn:li:entityType:orrery.dataset");
  }

  @Test
  public void testMetricRoundTrip() throws Exception {
    final String metricUrn = "urn:li:entityType:orrery.metric";
    assertEquals(EntityTypeUrnMapper.getName(metricUrn), Constants.METRIC_ENTITY_NAME);
    assertEquals(EntityTypeUrnMapper.getEntityType(metricUrn), EntityType.METRIC);
    assertEquals(EntityTypeUrnMapper.getEntityTypeUrn(Constants.METRIC_ENTITY_NAME), metricUrn);
  }

  @Test
  public void testSemanticModelRoundTrip() throws Exception {
    final String semanticModelUrn = "urn:li:entityType:orrery.semanticModel";
    assertEquals(
        EntityTypeUrnMapper.getName(semanticModelUrn), Constants.SEMANTIC_MODEL_ENTITY_NAME);
    assertEquals(EntityTypeUrnMapper.getEntityType(semanticModelUrn), EntityType.SEMANTIC_MODEL);
    assertEquals(
        EntityTypeUrnMapper.getEntityTypeUrn(Constants.SEMANTIC_MODEL_ENTITY_NAME),
        semanticModelUrn);
  }
}
