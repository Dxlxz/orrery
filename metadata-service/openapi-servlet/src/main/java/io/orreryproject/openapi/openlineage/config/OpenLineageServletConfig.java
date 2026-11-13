package io.orreryproject.openapi.openlineage.config;

import com.linkedin.common.FabricType;
import io.orreryproject.openapi.openlineage.mapping.RunEventMapper;
import io.orreryproject.openlineage.config.OrreryOpenlineageConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class OpenLineageServletConfig {

  private final OrreryOpenlineageProperties properties;

  public OpenLineageServletConfig(OrreryOpenlineageProperties properties) {
    this.properties = properties;
  }

  @Bean
  public RunEventMapper.MappingConfig mappingConfig() {
    // Parse FabricType from string property
    // Use commonDatasetEnv if specified, otherwise fall back to env
    String envValue =
        properties.getCommonDatasetEnv() != null
            ? properties.getCommonDatasetEnv()
            : properties.getEnv();

    FabricType fabricType = FabricType.PROD; // default
    if (envValue != null && !envValue.isEmpty()) {
      try {
        fabricType = FabricType.valueOf(envValue.toUpperCase());
      } catch (IllegalArgumentException e) {
        log.warn(
            "Invalid env value '{}'. Using default PROD. Valid values: PROD, DEV, TEST, QA, UAT, EI, PRE, STG, NON_PROD, CORP, RVW, PRD, TST, SIT, SBX, SANDBOX, CERT",
            envValue);
      }
    }

    // Use platformInstance if specified, otherwise use env as the cluster
    String platformInstance = properties.getPlatformInstance();
    if (platformInstance == null && properties.getEnv() != null && !properties.getEnv().isEmpty()) {
      // Default: use env as the DataFlow cluster
      platformInstance = properties.getEnv().toLowerCase();
      log.debug(
          "Using env '{}' as DataFlow cluster (platformInstance not specified)", platformInstance);
    }

    OrreryOpenlineageConfig orreryOpenlineageConfig =
        OrreryOpenlineageConfig.builder()
            .platformInstance(platformInstance)
            .commonDatasetPlatformInstance(properties.getCommonDatasetPlatformInstance())
            .commonDatasetEnv(properties.getCommonDatasetEnv())
            .platform(properties.getPlatform())
            .filePartitionRegexpPattern(properties.getFilePartitionRegexpPattern())
            .materializeDataset(properties.isMaterializeDataset())
            .includeSchemaMetadata(properties.isIncludeSchemaMetadata())
            .captureColumnLevelLineage(properties.isCaptureColumnLevelLineage())
            .usePatch(properties.isUsePatch())
            .fabricType(fabricType)
            .orchestrator(properties.getOrchestrator())
            .domains(properties.getDomains())
            .parentJobUrn(null)
            .build();
    log.info("Starting OpenLineage Endpoint with config: {}", orreryOpenlineageConfig);
    return RunEventMapper.MappingConfig.builder().orreryConfig(orreryOpenlineageConfig).build();
  }
}
