package orrery.spark;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.linkedin.common.FabricType;
import com.linkedin.common.urn.DataFlowUrn;
import com.linkedin.common.urn.DataJobUrn;
import com.linkedin.common.urn.DataPlatformUrn;
import com.linkedin.common.urn.DatasetUrn;
import com.linkedin.common.urn.Urn;
import com.linkedin.datajob.DataFlowInfo;
import com.linkedin.datajob.DataJobInfo;
import com.linkedin.dataprocess.DataProcessInstanceRelationships;
import com.linkedin.mxe.MetadataChangeProposal;
import io.orreryproject.openlineage.config.OrreryOpenlineageConfig;
import io.orreryproject.openlineage.dataset.OrreryDataset;
import io.orreryproject.openlineage.dataset.OrreryJob;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Tests for OrreryJob.toMcps() — verifying that the dataJobInputOutput aspect is correctly
 * generated (or skipped) based on inSet/outSet contents. These tests cover the customer-reported
 * issue where early coalesced emissions with empty edges clobbered later emissions that had output
 * dataset edges.
 */
class OrreryJobMcpGenerationTest {

  private static OrreryJob createMinimalJob() throws URISyntaxException {
    DataFlowUrn flowUrn = new DataFlowUrn("spark", "test-pipeline", "default");
    DataJobUrn jobUrn = new DataJobUrn(flowUrn, "test-pipeline");

    DataFlowInfo flowInfo = new DataFlowInfo();
    flowInfo.setName("test-pipeline");

    DataJobInfo jobInfo = new DataJobInfo();
    jobInfo.setName("test-pipeline");
    jobInfo.setFlowUrn(flowUrn);
    jobInfo.setType(DataJobInfo.Type.create("spark"));

    DataProcessInstanceRelationships dpiRelationships = new DataProcessInstanceRelationships();
    dpiRelationships.setParentTemplate(jobUrn);
    dpiRelationships.setUpstreamInstances(new com.linkedin.common.UrnArray());

    OrreryJob job = OrreryJob.builder().build();
    job.setFlowUrn(flowUrn);
    job.setJobUrn(jobUrn);
    job.setDataFlowInfo(flowInfo);
    job.setJobInfo(jobInfo);
    job.setEventTime(System.currentTimeMillis());
    job.setDataProcessInstanceUrn(Urn.createFromString("urn:li:dataProcessInstance:test-run-123"));
    job.setDataProcessInstanceRelationships(dpiRelationships);
    return job;
  }

  private static OrreryDataset createDataset(String name) throws URISyntaxException {
    DatasetUrn urn = new DatasetUrn(new DataPlatformUrn("s3"), name, FabricType.PROD);
    return OrreryDataset.builder().urn(urn).build();
  }

  private static OrreryOpenlineageConfig createConfig(boolean usePatch) {
    return OrreryOpenlineageConfig.builder()
        .fabricType(FabricType.PROD)
        .usePatch(usePatch)
        .materializeDataset(false)
        .includeSchemaMetadata(false)
        .build();
  }

  private static boolean hasDataJobInputOutputMcp(List<MetadataChangeProposal> mcps) {
    return mcps.stream().anyMatch(mcp -> "dataJobInputOutput".equals(mcp.getAspectName()));
  }

  /**
   * The customer clobbering scenario (PATCH mode): a coalesced early event (e.g., START) has empty
   * inSet/outSet. Because the PATCH branch requires a non-empty set, an empty emission would fall
   * through to the UPSERT branch and write an empty aspect that a later PATCH cannot override —
   * losing edges. So in PATCH mode an empty emission must be skipped entirely.
   */
  @Test
  void testEmptyEdgesSkipsDataJobInputOutputMcpInPatchMode()
      throws URISyntaxException, IOException {
    OrreryJob job = createMinimalJob();

    assertTrue(job.getInSet().isEmpty());
    assertTrue(job.getOutSet().isEmpty());

    List<MetadataChangeProposal> mcps = job.toMcps(createConfig(true));

    assertFalse(
        hasDataJobInputOutputMcp(mcps),
        "Empty inSet/outSet in PATCH mode should NOT produce a dataJobInputOutput MCP");
  }

  /**
   * In UPSERT mode there is no follow-up PATCH, so emitting the empty aspect is the only way to
   * clear edges a job legitimately no longer has. An empty emission must therefore be emitted (not
   * skipped), otherwise stale edges from a previous run would stick permanently.
   */
  @Test
  void testEmptyEdgesInUpsertModeEmitsToClearEdges() throws URISyntaxException, IOException {
    OrreryJob job = createMinimalJob();

    assertTrue(job.getInSet().isEmpty());
    assertTrue(job.getOutSet().isEmpty());

    List<MetadataChangeProposal> mcps = job.toMcps(createConfig(false));

    assertTrue(
        hasDataJobInputOutputMcp(mcps),
        "Empty inSet/outSet in UPSERT mode SHOULD produce a dataJobInputOutput MCP to clear edges");
  }

  /**
   * Simulates the final emission: both inputs and outputs populated. The dataJobInputOutput aspect
   * MUST be emitted with both inputDatasetEdges and outputDatasetEdges.
   */
  @Test
  void testPopulatedOutSetProducesDataJobInputOutputMcp() throws URISyntaxException, IOException {
    OrreryJob job = createMinimalJob();
    job.getInSet().add(createDataset("bucket/input/data"));
    job.getOutSet().add(createDataset("bucket/output/data"));

    List<MetadataChangeProposal> mcps = job.toMcps(createConfig(false));

    assertTrue(
        hasDataJobInputOutputMcp(mcps),
        "Populated inSet/outSet SHOULD produce a dataJobInputOutput MCP");
  }

  /** Verifies the PATCH path includes both input and output edge operations. */
  @Test
  void testPatchPathIncludesOutputEdges() throws URISyntaxException, IOException {
    OrreryJob job = createMinimalJob();
    job.getInSet().add(createDataset("bucket/input/data"));
    job.getOutSet().add(createDataset("bucket/output/data"));

    List<MetadataChangeProposal> mcps = job.toMcps(createConfig(true));

    assertTrue(
        hasDataJobInputOutputMcp(mcps),
        "PATCH path with populated sets SHOULD produce a dataJobInputOutput MCP");

    MetadataChangeProposal patchMcp =
        mcps.stream()
            .filter(mcp -> "dataJobInputOutput".equals(mcp.getAspectName()))
            .findFirst()
            .orElseThrow(() -> new AssertionError("Expected dataJobInputOutput MCP"));
    String patchContent = patchMcp.getAspect().getValue().asString(Charset.defaultCharset());
    assertTrue(
        patchContent.contains("outputDatasetEdges"),
        "Patch MCP should contain outputDatasetEdges operations");
    assertTrue(
        patchContent.contains("inputDatasetEdges"),
        "Patch MCP should contain inputDatasetEdges operations");
  }

  /**
   * Simulates an intermediate coalesced emission where reads have been captured but the write
   * hasn't happened yet. The dataJobInputOutput should still be emitted (with input edges only).
   */
  @Test
  void testInputOnlyProducesDataJobInputOutputMcp() throws URISyntaxException, IOException {
    OrreryJob job = createMinimalJob();
    job.getInSet().add(createDataset("bucket/input/data"));

    List<MetadataChangeProposal> mcps = job.toMcps(createConfig(false));

    assertTrue(
        hasDataJobInputOutputMcp(mcps), "Input-only job SHOULD produce a dataJobInputOutput MCP");
  }
}
