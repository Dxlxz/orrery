package io.orreryproject.openlineage;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import com.linkedin.common.FabricType;
import io.orreryproject.openlineage.config.OrreryOpenlineageConfig;
import io.orreryproject.openlineage.converter.OpenLineageToOrrery;
import io.orreryproject.openlineage.dataset.OrreryJob;
import io.openlineage.client.OpenLineage;
import java.net.URI;
import java.time.ZonedDateTime;
import java.util.UUID;
import org.testng.annotations.Test;

/** Tests for OpenLineage configuration including orchestrator and fabric type */
public class OpenLineageConfigTest {

  private OpenLineage.RunEvent createTestEvent(URI producerUri) {
    OpenLineage openLineage = new OpenLineage(producerUri);
    return openLineage
        .newRunEventBuilder()
        .eventTime(ZonedDateTime.now())
        .eventType(OpenLineage.RunEvent.EventType.START)
        .run(openLineage.newRunBuilder().runId(UUID.randomUUID()).build())
        .job(
            openLineage
                .newJobBuilder()
                .namespace("test-namespace")
                .name("test-job")
                .facets(openLineage.newJobFacetsBuilder().build())
                .build())
        .inputs(java.util.Collections.emptyList())
        .outputs(java.util.Collections.emptyList())
        .build();
  }

  @Test
  public void testOrchestratorOverride() throws Exception {
    // Create config with orchestrator override
    OrreryOpenlineageConfig config =
        OrreryOpenlineageConfig.builder()
            .fabricType(FabricType.PROD)
            .orchestrator("custom-orchestrator")
            .build();

    OpenLineage.RunEvent runEvent =
        createTestEvent(URI.create("https://github.com/OpenLineage/OpenLineage/"));

    OrreryJob orreryJob = OpenLineageToOrrery.convertRunEventToJob(runEvent, config);

    assertNotNull(orreryJob);
    assertEquals(
        orreryJob.getFlowUrn().getOrchestratorEntity(),
        "custom-orchestrator",
        "Orchestrator should be overridden to custom-orchestrator");
  }

  @Test
  public void testOrchestratorFromProducerUrl() throws Exception {
    // Test with an Airflow producer URL and no override
    OrreryOpenlineageConfig config =
        OrreryOpenlineageConfig.builder().fabricType(FabricType.PROD).build();

    URI producerUri = URI.create("https://github.com/apache/airflow/");
    OpenLineage.RunEvent runEvent = createTestEvent(producerUri);

    OrreryJob orreryJob = OpenLineageToOrrery.convertRunEventToJob(runEvent, config);

    assertNotNull(orreryJob);
    assertEquals(
        orreryJob.getFlowUrn().getOrchestratorEntity(),
        "airflow",
        "Orchestrator should be derived from Airflow producer URL");
  }

  @Test
  public void testOrchestratorOverrideTakesPrecedence() throws Exception {
    // Even if producer URL suggests Airflow, override should win
    OrreryOpenlineageConfig config =
        OrreryOpenlineageConfig.builder()
            .fabricType(FabricType.DEV)
            .orchestrator("my-platform")
            .build();

    URI producerUri = URI.create("https://github.com/apache/airflow/");
    OpenLineage.RunEvent runEvent = createTestEvent(producerUri);

    OrreryJob orreryJob = OpenLineageToOrrery.convertRunEventToJob(runEvent, config);

    assertNotNull(orreryJob);
    assertEquals(
        orreryJob.getFlowUrn().getOrchestratorEntity(),
        "my-platform",
        "Orchestrator override should take precedence over producer URL");
  }

  @Test
  public void testFabricTypeConfiguration() throws Exception {
    // Test that fabric type can be configured (fabric applies to datasets, not flows)
    OrreryOpenlineageConfig config =
        OrreryOpenlineageConfig.builder()
            .fabricType(FabricType.QA)
            .orchestrator("test-orchestrator")
            .build();

    OpenLineage.RunEvent runEvent =
        createTestEvent(URI.create("https://github.com/OpenLineage/OpenLineage/"));

    OrreryJob orreryJob = OpenLineageToOrrery.convertRunEventToJob(runEvent, config);

    assertNotNull(orreryJob);
    // Fabric type is applied to datasets not flows, which is tested in existing tests
    assertEquals(config.getFabricType(), FabricType.QA, "Config should have QA fabric type");
  }

  @Test
  public void testDefaultFabricTypeIsProd() throws Exception {
    // Test that default fabric type is PROD when not specified
    // Need to set orchestrator since no producer URL pattern will match
    OrreryOpenlineageConfig config =
        OrreryOpenlineageConfig.builder().orchestrator("test").build();

    OpenLineage.RunEvent runEvent =
        createTestEvent(URI.create("https://github.com/OpenLineage/OpenLineage/spark/"));

    OrreryJob orreryJob = OpenLineageToOrrery.convertRunEventToJob(runEvent, config);

    assertNotNull(orreryJob);
    // Fabric type default is tested via the config
    assertEquals(config.getFabricType(), FabricType.PROD, "Default fabric should be PROD");
  }
}
