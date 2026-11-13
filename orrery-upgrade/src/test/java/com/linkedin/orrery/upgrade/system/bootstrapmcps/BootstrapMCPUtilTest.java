package com.linkedin.orrery.upgrade.system.bootstrapmcps;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.linkedin.common.AuditStamp;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.orrery.upgrade.system.bootstrapmcps.model.BootstrapMCPConfigFile;
import com.linkedin.events.metadata.ChangeType;
import com.linkedin.ingestion.OrreryIngestionSourceInfo;
import com.linkedin.metadata.aspect.batch.AspectsBatch;
import com.linkedin.metadata.aspect.batch.MCPItem;
import com.linkedin.metadata.utils.AuditStampUtils;
import io.orreryproject.metadata.context.OperationContext;
import io.orreryproject.test.metadata.context.TestOperationContexts;
import java.io.IOException;
import java.util.List;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.testng.SystemStub;
import uk.org.webcompere.systemstubs.testng.SystemStubsListener;

@Listeners(SystemStubsListener.class)
public class BootstrapMCPUtilTest {
  static final OperationContext OP_CONTEXT =
      TestOperationContexts.systemContextNoSearchAuthorization();
  private static final String ORRERY_TEST_VALUES_ENV = "ORRERY_TEST_VALUES_ENV";
  private static final String ORRERY_TEST_REVISION_ENV = "ORRERY_TEST_REVISION_ENV";
  private static final AuditStamp TEST_AUDIT_STAMP = AuditStampUtils.createDefaultAuditStamp();

  @SystemStub private EnvironmentVariables environmentVariables;

  @BeforeMethod
  private void resetEnvironment() {
    environmentVariables.remove(ORRERY_TEST_VALUES_ENV);
    environmentVariables.remove(ORRERY_TEST_REVISION_ENV);
  }

  @Test
  public void testResolveYamlConf() throws IOException {
    BootstrapMCPConfigFile initConfig =
        BootstrapMCPUtil.resolveYamlConf(
            OP_CONTEXT, "bootstrapmcp/test.yaml", BootstrapMCPConfigFile.class);
    assertEquals(initConfig.getBootstrap().getTemplates().size(), 1);

    BootstrapMCPConfigFile.MCPTemplate template = initConfig.getBootstrap().getTemplates().get(0);
    assertEquals(template.getName(), "orrery-test");
    assertEquals(template.getVersion(), "v10");
    assertFalse(template.isForce());
    assertFalse(template.isBlocking());
    assertTrue(template.isAsync());
    assertFalse(template.isOptional());
    assertEquals(template.getMcps_location(), "bootstrapmcp/orrery-test-mcp.yaml");
    assertEquals(template.getValues_env(), "ORRERY_TEST_VALUES_ENV");
  }

  @Test
  public void testResolveYamlConfOverride() throws IOException {
    environmentVariables.set(ORRERY_TEST_REVISION_ENV, "{\"version\":\"2024110600\"}");

    BootstrapMCPConfigFile initConfig =
        BootstrapMCPUtil.resolveYamlConf(
            OP_CONTEXT, "bootstrapmcp/test.yaml", BootstrapMCPConfigFile.class);
    assertEquals(initConfig.getBootstrap().getTemplates().size(), 1);

    BootstrapMCPConfigFile.MCPTemplate template =
        initConfig.getBootstrap().getTemplates().get(0).withOverride(new ObjectMapper());
    assertEquals(template.getName(), "orrery-test");
    assertEquals(template.getVersion(), "2024110600");
    assertFalse(template.isForce());
    assertFalse(template.isBlocking());
    assertTrue(template.isAsync());
    assertFalse(template.isOptional());
    assertEquals(template.getMcps_location(), "bootstrapmcp/orrery-test-mcp.yaml");
    assertEquals(template.getValues_env(), "ORRERY_TEST_VALUES_ENV");
  }

  @Test
  public void testResolveMCPTemplateDefaults() throws IOException {
    BootstrapMCPConfigFile.MCPTemplate template =
        BootstrapMCPUtil.resolveYamlConf(
                OP_CONTEXT, "bootstrapmcp/test.yaml", BootstrapMCPConfigFile.class)
            .getBootstrap()
            .getTemplates()
            .get(0);

    List<ObjectNode> mcpObjectNodes =
        BootstrapMCPUtil.resolveMCPTemplate(OP_CONTEXT, template, TEST_AUDIT_STAMP);
    assertEquals(mcpObjectNodes.size(), 1);

    ObjectNode mcp = mcpObjectNodes.get(0);
    assertEquals(mcp.get("entityType").asText(), "orreryIngestionSource");
    assertEquals(mcp.get("entityUrn").asText(), "urn:li:orreryIngestionSource:orrery-test");
    assertEquals(mcp.get("aspectName").asText(), "orreryIngestionSourceInfo");
    assertEquals(mcp.get("changeType").asText(), "UPSERT");

    ObjectNode aspect = (ObjectNode) mcp.get("aspect");
    assertEquals(aspect.get("type").asText(), "orrery-gc");
    assertEquals(aspect.get("name").asText(), "orrery-test");

    ObjectNode schedule = (ObjectNode) aspect.get("schedule");
    assertEquals(schedule.get("timezone").asText(), "UTC");
    assertEquals(schedule.get("interval").asText(), "0 0 * * *");

    ObjectNode config = (ObjectNode) aspect.get("config");
    assertTrue(config.get("extraArgs").isObject());
    assertTrue(config.get("debugMode").isBoolean());
    assertEquals(config.get("executorId").asText(), "default");

    ObjectNode recipe = (ObjectNode) config.get("recipe");
    ObjectNode source = (ObjectNode) recipe.get("source");
    assertEquals(source.get("type").asText(), "orrery-gc");

    ObjectNode sourceConfig = (ObjectNode) source.get("config");
    assertFalse(sourceConfig.get("cleanup_expired_tokens").asBoolean());
    assertTrue(sourceConfig.get("truncate_indices").asBoolean());

    ObjectNode dataprocessCleanup = (ObjectNode) sourceConfig.get("dataprocess_cleanup");
    assertEquals(dataprocessCleanup.get("retention_days").asInt(), 10);
    assertTrue(dataprocessCleanup.get("delete_empty_data_jobs").asBoolean());
    assertTrue(dataprocessCleanup.get("delete_empty_data_flows").asBoolean());
    assertFalse(dataprocessCleanup.get("hard_delete_entities").asBoolean());
    assertEquals(dataprocessCleanup.get("keep_last_n").asInt(), 5);

    ObjectNode softDeletedEntitiesCleanup =
        (ObjectNode) sourceConfig.get("soft_deleted_entities_cleanup");
    assertEquals(softDeletedEntitiesCleanup.get("retention_days").asInt(), 10);

    assertTrue(mcp.get("headers").isObject());
  }

  @Test
  public void testResolveMCPTemplateOverride() throws IOException {
    environmentVariables.set(
        "ORRERY_TEST_VALUES_ENV",
        "{\n"
            + "    \"ingestion\": {\n"
            + "      \"name\": \"name-override\"\n"
            + "    },\n"
            + "    \"schedule\": {\n"
            + "      \"timezone\": \"America/Chicago\",\n"
            + "      \"interval\": \"9 9 * * *\"\n"
            + "    },\n"
            + "    \"cleanup_expired_tokens\": true,\n"
            + "    \"truncate_indices\": false,\n"
            + "    \"dataprocess_cleanup\": {\n"
            + "      \"retention_days\": 99,\n"
            + "      \"delete_empty_data_jobs\": false,\n"
            + "      \"delete_empty_data_flows\": false,\n"
            + "      \"hard_delete_entities\": true,\n"
            + "      \"keep_last_n\": 50\n"
            + "    },\n"
            + "    \"soft_deleted_entities_cleanup\": {\n"
            + "      \"retention_days\": 100\n"
            + "    }\n"
            + "}");

    BootstrapMCPConfigFile.MCPTemplate template =
        BootstrapMCPUtil.resolveYamlConf(
                OP_CONTEXT, "bootstrapmcp/test.yaml", BootstrapMCPConfigFile.class)
            .getBootstrap()
            .getTemplates()
            .get(0);

    List<ObjectNode> mcpObjectNodes =
        BootstrapMCPUtil.resolveMCPTemplate(OP_CONTEXT, template, TEST_AUDIT_STAMP);
    assertEquals(mcpObjectNodes.size(), 1);

    ObjectNode mcp = mcpObjectNodes.get(0);
    assertEquals(mcp.get("entityType").asText(), "orreryIngestionSource");
    assertEquals(mcp.get("entityUrn").asText(), "urn:li:orreryIngestionSource:orrery-test");
    assertEquals(mcp.get("aspectName").asText(), "orreryIngestionSourceInfo");
    assertEquals(mcp.get("changeType").asText(), "UPSERT");

    ObjectNode aspect = (ObjectNode) mcp.get("aspect");
    assertEquals(aspect.get("type").asText(), "orrery-gc");
    assertEquals(aspect.get("name").asText(), "name-override");

    ObjectNode schedule = (ObjectNode) aspect.get("schedule");
    assertEquals(schedule.get("timezone").asText(), "America/Chicago");
    assertEquals(schedule.get("interval").asText(), "9 9 * * *");

    ObjectNode config = (ObjectNode) aspect.get("config");
    assertTrue(config.get("extraArgs").isObject());
    assertTrue(config.get("debugMode").isBoolean());
    assertEquals(config.get("executorId").asText(), "default");

    ObjectNode recipe = (ObjectNode) config.get("recipe");
    ObjectNode source = (ObjectNode) recipe.get("source");
    assertEquals(source.get("type").asText(), "orrery-gc");

    ObjectNode sourceConfig = (ObjectNode) source.get("config");
    assertTrue(sourceConfig.get("cleanup_expired_tokens").asBoolean());
    assertFalse(sourceConfig.get("truncate_indices").asBoolean());

    ObjectNode dataprocessCleanup = (ObjectNode) sourceConfig.get("dataprocess_cleanup");
    assertEquals(dataprocessCleanup.get("retention_days").asInt(), 99);
    assertFalse(dataprocessCleanup.get("delete_empty_data_jobs").asBoolean());
    assertFalse(dataprocessCleanup.get("delete_empty_data_flows").asBoolean());
    assertTrue(dataprocessCleanup.get("hard_delete_entities").asBoolean());
    assertEquals(dataprocessCleanup.get("keep_last_n").asInt(), 50);

    ObjectNode softDeletedEntitiesCleanup =
        (ObjectNode) sourceConfig.get("soft_deleted_entities_cleanup");
    assertEquals(softDeletedEntitiesCleanup.get("retention_days").asInt(), 100);

    assertTrue(mcp.get("headers").isObject());
  }

  @Test
  public void testMCPBatch() throws IOException {
    BootstrapMCPConfigFile.MCPTemplate template =
        BootstrapMCPUtil.resolveYamlConf(
                OP_CONTEXT, "bootstrapmcp/test.yaml", BootstrapMCPConfigFile.class)
            .getBootstrap()
            .getTemplates()
            .get(0);

    AspectsBatch batch = BootstrapMCPUtil.generateAspectBatch(OP_CONTEXT, template, "testMCPBatch");
    assertEquals(batch.getMCPItems().size(), 1);

    MCPItem item = batch.getMCPItems().get(0);
    assertEquals(item.getUrn(), UrnUtils.getUrn("urn:li:orreryIngestionSource:orrery-test"));
    assertEquals(item.getAspectName(), "orreryIngestionSourceInfo");
    assertEquals(item.getChangeType(), ChangeType.UPSERT);
    assertNotNull(item.getSystemMetadata());

    OrreryIngestionSourceInfo ingestionSource = item.getAspect(OrreryIngestionSourceInfo.class);

    assertEquals(ingestionSource.getName(), "orrery-test");
    assertEquals(ingestionSource.getType(), "orrery-gc");

    assertFalse(ingestionSource.getConfig().isDebugMode());
    assertEquals(ingestionSource.getConfig().getExecutorId(), "default");

    assertEquals(ingestionSource.getSchedule().getTimezone(), "UTC");
    assertEquals(ingestionSource.getSchedule().getInterval(), "0 0 * * *");

    assertEquals(
        OP_CONTEXT.getObjectMapper().readTree(ingestionSource.getConfig().getRecipe()),
        OP_CONTEXT
            .getObjectMapper()
            .readTree(
                "{\"source\":{\"type\":\"orrery-gc\",\"config\":{\"cleanup_expired_tokens\":false,\"truncate_indices\":true,\"dataprocess_cleanup\":{\"retention_days\":10,\"delete_empty_data_jobs\":true,\"delete_empty_data_flows\":true,\"hard_delete_entities\":false,\"keep_last_n\":5},\"soft_deleted_entities_cleanup\":{\"retention_days\":10},\"execution_request_cleanup\":{\"keep_history_min_count\":10,\"keep_history_max_count\":1000,\"keep_history_max_days\":30,\"batch_read_size\":100,\"enabled\":false}}}}"));
  }

  @Test
  public void testSearchTemplatesKeepRendererPlaceholders() throws IOException {
    BootstrapMCPConfigFile.MCPTemplate template =
        BootstrapMCPConfigFile.MCPTemplate.builder()
            .name("search-templates")
            .version("v1")
            .mcps_location("bootstrap_mcps/search-templates.yaml")
            .build();
    List<ObjectNode> mcps =
        BootstrapMCPUtil.resolveMCPTemplate(OP_CONTEXT, template, TEST_AUDIT_STAMP);
    String rendered = mcps.toString();
    assertTrue(rendered.contains("{{alphanumericSplit}}"));
    assertTrue(rendered.contains("{{fuzzy}}"));
    assertTrue(rendered.contains("{{unquoted}}"));
    assertTrue(rendered.contains("{{&defaultQueryFieldsJson}}"));
    assertFalse(rendered.contains("keyword_weight"));
    assertFalse(rendered.contains("<%"));
    assertFalse(rendered.contains("{{="));
    assertTrue(rendered.contains("urn:li:searchTemplate:search"));
    assertTrue(rendered.contains("urn:li:searchTemplate:searchLight"));
    assertTrue(rendered.contains("urn:li:searchTemplate:structured"));
    assertTrue(rendered.contains("urn:li:searchTemplate:fulltext"));
    assertTrue(rendered.contains("urn:li:searchTemplate:light"));
    assertFalse(rendered.contains("lightQuery:"));
    assertEquals(mcps.get(0).get("changeType").asText(), "UPSERT");
  }

  @Test
  public void testLifecycleStagesDoNotOverwriteExistingStages() throws IOException {
    BootstrapMCPConfigFile.MCPTemplate template =
        BootstrapMCPConfigFile.MCPTemplate.builder()
            .name("lifecycle-stages")
            .version("v3")
            .mcps_location("bootstrap_mcps/lifecycle-stages.yaml")
            .build();

    AspectsBatch batch =
        BootstrapMCPUtil.generateAspectBatch(
            OP_CONTEXT, template, "testLifecycleStagesDoNotOverwriteExistingStages");

    assertFalse(batch.getMCPItems().isEmpty());
    for (MCPItem item : batch.getMCPItems()) {
      assertEquals(item.getChangeType(), ChangeType.CREATE);
      assertEquals(item.getHeader("If-None-Match").orElse(null), "*");
    }
  }
}
