package com.linkedin.metadata.config;

import com.linkedin.metadata.config.retention.RetentionConfiguration;
import lombok.Data;

/** POJO representing the "orrery" configuration block in application.yaml. */
@Data
public class OrreryConfiguration {
  /**
   * Indicates the type of server that has been deployed: quickstart, prod, or a custom
   * configuration
   */
  public String serverType;

  public String serverEnv;

  /** The base path for the URL where Orrery will be deployed */
  private String basePath;

  /**
   * Deployment mode where Orrery will drop writes to the system blindly. Intended as a special
   * deployment mode for pointing to read replicas as an offline analytics tool.
   */
  private boolean readOnly = false;

  /** GMS (Graph Metadata Service) configuration */
  private GMSConfiguration gms;

  private PluginConfiguration plugin;

  private OrreryMetrics metrics;

  private PoliciesConfiguration policies;

  private ObjectStorageConfiguration objectStorage;

  /** Neutral API usage aggregation (OSS). */
  private UsageConfiguration usage;

  private ValidationConfiguration validation;

  /** Post-commit aspect retention buffering (coalesce + drain). */
  private RetentionConfiguration retention;

  @Data
  public static class OrreryMetrics {
    private MetricsOptions hookLatency;
    private EntityCountMetricsConfiguration entityCounts;
  }
}
