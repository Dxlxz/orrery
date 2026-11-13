package io.orreryproject.openapi.openlineage.config;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import com.linkedin.common.FabricType;
import io.orreryproject.openapi.openlineage.mapping.RunEventMapper;
import io.orreryproject.openlineage.config.OrreryOpenlineageConfig;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.Test;

/** Tests for OpenLineageServletConfig parsing and configuration */
public class OpenLineageServletConfigTest extends AbstractTestNGSpringContextTests {

  /** Test configuration with valid environment */
  @SpringBootTest(classes = {OpenLineageServletConfig.class, TestConfigDev.class})
  @TestPropertySource(properties = {"orrery.openlineage.env=DEV"})
  public static class ValidEnvTest extends AbstractTestNGSpringContextTests {

    @Autowired private RunEventMapper.MappingConfig mappingConfig;

    @Test
    public void testValidEnv() {
      assertNotNull(mappingConfig);
      assertNotNull(mappingConfig.getOrreryConfig());
      assertEquals(mappingConfig.getOrreryConfig().getFabricType(), FabricType.DEV);
    }
  }

  /** Test configuration with invalid environment (should default to PROD with warning) */
  @SpringBootTest(classes = {OpenLineageServletConfig.class, TestConfigInvalid.class})
  @TestPropertySource(properties = {"orrery.openlineage.env=INVALID_ENV"})
  public static class InvalidEnvTest extends AbstractTestNGSpringContextTests {

    @Autowired private RunEventMapper.MappingConfig mappingConfig;

    @Test
    public void testInvalidEnv() {
      assertNotNull(mappingConfig);
      assertNotNull(mappingConfig.getOrreryConfig());
      // Should default to PROD when invalid value is provided
      assertEquals(mappingConfig.getOrreryConfig().getFabricType(), FabricType.PROD);
    }
  }

  /** Test configuration with empty environment (should default to PROD) */
  @SpringBootTest(classes = {OpenLineageServletConfig.class, TestConfigEmpty.class})
  public static class EmptyEnvTest extends AbstractTestNGSpringContextTests {

    @Autowired private RunEventMapper.MappingConfig mappingConfig;

    @Test
    public void testEmptyEnv() {
      assertNotNull(mappingConfig);
      assertNotNull(mappingConfig.getOrreryConfig());
      // Should default to PROD when no value is provided
      assertEquals(mappingConfig.getOrreryConfig().getFabricType(), FabricType.PROD);
    }
  }

  /** Test orchestrator configuration */
  @SpringBootTest(classes = {OpenLineageServletConfig.class, TestConfigOrchestrator.class})
  @TestPropertySource(
      properties = {
        "orrery.openlineage.orchestrator=my-custom-orchestrator",
        "orrery.openlineage.env=STG"
      })
  public static class OrchestratorTest extends AbstractTestNGSpringContextTests {

    @Autowired private RunEventMapper.MappingConfig mappingConfig;

    @Test
    public void testOrchestratorConfig() {
      assertNotNull(mappingConfig);
      assertNotNull(mappingConfig.getOrreryConfig());
      assertEquals(mappingConfig.getOrreryConfig().getOrchestrator(), "my-custom-orchestrator");
      assertEquals(mappingConfig.getOrreryConfig().getFabricType(), FabricType.STG);
    }
  }

  /** Test that null orchestrator is handled correctly */
  @SpringBootTest(classes = {OpenLineageServletConfig.class, TestConfigNoOrchestrator.class})
  public static class NoOrchestratorTest extends AbstractTestNGSpringContextTests {

    @Autowired private RunEventMapper.MappingConfig mappingConfig;

    @Test
    public void testNoOrchestrator() {
      assertNotNull(mappingConfig);
      assertNotNull(mappingConfig.getOrreryConfig());
      assertNull(mappingConfig.getOrreryConfig().getOrchestrator());
      assertEquals(mappingConfig.getOrreryConfig().getFabricType(), FabricType.PROD);
    }
  }

  /** Test all config properties together */
  @SpringBootTest(classes = {OpenLineageServletConfig.class, TestConfigComplete.class})
  @TestPropertySource(
      properties = {
        "orrery.openlineage.env=TEST",
        "orrery.openlineage.orchestrator=airflow",
        "orrery.openlineage.platform-instance=us-west-2",
        "orrery.openlineage.materialize-dataset=false",
        "orrery.openlineage.include-schema-metadata=false",
        "orrery.openlineage.capture-column-level-lineage=false"
      })
  public static class CompleteConfigTest extends AbstractTestNGSpringContextTests {

    @Autowired private RunEventMapper.MappingConfig mappingConfig;

    @Test
    public void testCompleteConfig() {
      assertNotNull(mappingConfig);
      OrreryOpenlineageConfig config = mappingConfig.getOrreryConfig();
      assertNotNull(config);

      // Verify all configured values
      assertEquals(config.getFabricType(), FabricType.TEST);
      assertEquals(config.getOrchestrator(), "airflow");
      assertEquals(config.getPlatformInstance(), "us-west-2");
      assertEquals(config.isMaterializeDataset(), false);
      assertEquals(config.isIncludeSchemaMetadata(), false);
      assertEquals(config.isCaptureColumnLevelLineage(), false);
    }
  }

  /** Test case-insensitive env parsing */
  @SpringBootTest(classes = {OpenLineageServletConfig.class, TestConfigCaseInsensitive.class})
  @TestPropertySource(
      properties = {
        "orrery.openlineage.env=stg" // lowercase
      })
  public static class CaseInsensitiveTest extends AbstractTestNGSpringContextTests {

    @Autowired private RunEventMapper.MappingConfig mappingConfig;

    @Test
    public void testCaseInsensitiveEnv() {
      assertNotNull(mappingConfig);
      assertNotNull(mappingConfig.getOrreryConfig());
      // Should handle lowercase and convert to STG
      assertEquals(mappingConfig.getOrreryConfig().getFabricType(), FabricType.STG);
    }
  }

  /** Test that env sets both DataFlow cluster and Dataset fabricType by default */
  @SpringBootTest(classes = {OpenLineageServletConfig.class, TestConfigEnvSetsCluster.class})
  @TestPropertySource(properties = {"orrery.openlineage.env=DEV"})
  public static class EnvSetsClusterTest extends AbstractTestNGSpringContextTests {

    @Autowired private RunEventMapper.MappingConfig mappingConfig;

    @Test
    public void testEnvSetsCluster() {
      assertNotNull(mappingConfig);
      OrreryOpenlineageConfig config = mappingConfig.getOrreryConfig();
      assertNotNull(config);

      // Env should set Dataset fabricType
      assertEquals(config.getFabricType(), FabricType.DEV);

      // Env should also set DataFlow cluster (via platformInstance)
      assertEquals(
          config.getPlatformInstance(), "dev", "env should default DataFlow cluster to 'dev'");
    }
  }

  /** Test that platformInstance overrides env for DataFlow cluster */
  @SpringBootTest(
      classes = {OpenLineageServletConfig.class, TestConfigPlatformInstanceOverride.class})
  @TestPropertySource(
      properties = {
        "orrery.openlineage.env=PROD",
        "orrery.openlineage.platform-instance=prod-us-west-2"
      })
  public static class PlatformInstanceOverrideTest extends AbstractTestNGSpringContextTests {

    @Autowired private RunEventMapper.MappingConfig mappingConfig;

    @Test
    public void testPlatformInstanceOverride() {
      assertNotNull(mappingConfig);
      OrreryOpenlineageConfig config = mappingConfig.getOrreryConfig();
      assertNotNull(config);

      // Dataset should use PROD
      assertEquals(config.getFabricType(), FabricType.PROD);

      // DataFlow cluster should use the override
      assertEquals(
          config.getPlatformInstance(),
          "prod-us-west-2",
          "platformInstance should override env for DataFlow cluster");
    }
  }

  /** Test that commonDatasetEnv overrides env for Dataset fabricType */
  @SpringBootTest(classes = {OpenLineageServletConfig.class, TestConfigCommonDatasetEnv.class})
  @TestPropertySource(
      properties = {"orrery.openlineage.env=PROD", "orrery.openlineage.common-dataset-env=DEV"})
  public static class CommonDatasetEnvTest extends AbstractTestNGSpringContextTests {

    @Autowired private RunEventMapper.MappingConfig mappingConfig;

    @Test
    public void testCommonDatasetEnv() {
      assertNotNull(mappingConfig);
      OrreryOpenlineageConfig config = mappingConfig.getOrreryConfig();
      assertNotNull(config);

      // Dataset should use DEV (override)
      assertEquals(config.getFabricType(), FabricType.DEV);

      // DataFlow cluster should use prod (from env)
      assertEquals(config.getPlatformInstance(), "prod", "DataFlow cluster should use env");

      // Config should have commonDatasetEnv set
      assertEquals(config.getCommonDatasetEnv(), "DEV");
    }
  }

  // Test configuration classes - each needs to provide OrreryOpenlineageProperties bean
  @Configuration
  static class TestConfigDev {
    @Bean
    public OrreryOpenlineageProperties orreryOpenlineageProperties() {
      OrreryOpenlineageProperties props = new OrreryOpenlineageProperties();
      props.setEnv("DEV");
      return props;
    }
  }

  @Configuration
  static class TestConfigInvalid {
    @Bean
    public OrreryOpenlineageProperties orreryOpenlineageProperties() {
      OrreryOpenlineageProperties props = new OrreryOpenlineageProperties();
      props.setEnv("INVALID_ENV");
      return props;
    }
  }

  @Configuration
  static class TestConfigEmpty {
    @Bean
    public OrreryOpenlineageProperties orreryOpenlineageProperties() {
      return new OrreryOpenlineageProperties();
    }
  }

  @Configuration
  static class TestConfigOrchestrator {
    @Bean
    public OrreryOpenlineageProperties orreryOpenlineageProperties() {
      OrreryOpenlineageProperties props = new OrreryOpenlineageProperties();
      props.setEnv("STG");
      props.setOrchestrator("my-custom-orchestrator");
      return props;
    }
  }

  @Configuration
  static class TestConfigNoOrchestrator {
    @Bean
    public OrreryOpenlineageProperties orreryOpenlineageProperties() {
      OrreryOpenlineageProperties props = new OrreryOpenlineageProperties();
      props.setEnv("PROD");
      return props;
    }
  }

  @Configuration
  static class TestConfigComplete {
    @Bean
    public OrreryOpenlineageProperties orreryOpenlineageProperties() {
      OrreryOpenlineageProperties props = new OrreryOpenlineageProperties();
      props.setEnv("TEST");
      props.setOrchestrator("airflow");
      props.setPlatformInstance("us-west-2");
      props.setMaterializeDataset(false);
      props.setIncludeSchemaMetadata(false);
      props.setCaptureColumnLevelLineage(false);
      return props;
    }
  }

  @Configuration
  static class TestConfigCaseInsensitive {
    @Bean
    public OrreryOpenlineageProperties orreryOpenlineageProperties() {
      OrreryOpenlineageProperties props = new OrreryOpenlineageProperties();
      props.setEnv("stg"); // lowercase
      return props;
    }
  }

  @Configuration
  static class TestConfigEnvSetsCluster {
    @Bean
    public OrreryOpenlineageProperties orreryOpenlineageProperties() {
      OrreryOpenlineageProperties props = new OrreryOpenlineageProperties();
      props.setEnv("DEV");
      return props;
    }
  }

  @Configuration
  static class TestConfigPlatformInstanceOverride {
    @Bean
    public OrreryOpenlineageProperties orreryOpenlineageProperties() {
      OrreryOpenlineageProperties props = new OrreryOpenlineageProperties();
      props.setEnv("PROD");
      props.setPlatformInstance("prod-us-west-2");
      return props;
    }
  }

  @Configuration
  static class TestConfigCommonDatasetEnv {
    @Bean
    public OrreryOpenlineageProperties orreryOpenlineageProperties() {
      OrreryOpenlineageProperties props = new OrreryOpenlineageProperties();
      props.setEnv("PROD");
      props.setCommonDatasetEnv("DEV");
      return props;
    }
  }

  /**
   * Unlike the cases above, which hand-build the properties bean and so never exercise binding,
   * this one enables real {@code @ConfigurationProperties} binding. It covers both the {@code
   * .domains(...)} wiring in {@link OpenLineageServletConfig#mappingConfig()} and {@code
   * ORRERY_OPENLINEAGE_DOMAINS} parsing as a comma-separated list.
   */
  @SpringBootTest(classes = {OpenLineageServletConfig.class, TestConfigBoundProperties.class})
  @TestPropertySource(
      properties = {
        "orrery.openlineage.env=PROD",
        "orrery.openlineage.domains=urn:li:domain:finance,urn:li:domain:reporting"
      })
  public static class BoundDomainsTest extends AbstractTestNGSpringContextTests {

    @Autowired private RunEventMapper.MappingConfig mappingConfig;

    @Test
    public void testCommaSeparatedDomainsBindAndReachConverterConfig() {
      assertEquals(
          mappingConfig.getOrreryConfig().getDomains(),
          List.of("urn:li:domain:finance", "urn:li:domain:reporting"));
    }
  }

  @Configuration
  @EnableConfigurationProperties(OrreryOpenlineageProperties.class)
  static class TestConfigBoundProperties {}
}
