package orrery.spark.conf;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedin.common.FabricType;
import com.linkedin.common.urn.DataJobUrn;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.orreryproject.openlineage.config.OrreryOpenlineageConfig;
import io.orreryproject.openlineage.dataset.ConnectionInstanceDetail;
import io.orreryproject.openlineage.dataset.PathSpec;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.spark.SparkConf;
import org.apache.spark.SparkEnv;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparkConfigParser {
  public static final String PARENT_JOB_KEY = "parent.datajob_urn";
  public static final String EMITTER_TYPE = "emitter";
  public static final String GMS_URL_KEY = "rest.server";
  public static final String GMS_AUTH_TOKEN = "rest.token";
  public static final String FILE_EMITTER_FILE_NAME = "file.filename";
  public static final String DISABLE_SSL_VERIFICATION_KEY = "rest.disable_ssl_verification";
  public static final String REST_DISABLE_CHUNKED_ENCODING = "rest.disable_chunked_encoding";
  public static final String CONFIG_LOG_MCPS = "log.mcps";

  public static final String MAX_RETRIES = "rest.max_retries";
  public static final String RETRY_INTERVAL_IN_SEC = "rest.retry_interval_in_sec";
  public static final String KAFKA_MCP_TOPIC = "kafka.mcp_topic";
  public static final String KAFKA_EMITTER_BOOTSTRAP = "kafka.bootstrap";
  public static final String KAFKA_EMITTER_SCHEMA_REGISTRY_URL = "kafka.schema_registry_url";
  public static final String KAFKA_EMITTER_SCHEMA_REGISTRY_CONFIG = "kafka.schema_registry_config";
  public static final String KAFKA_EMITTER_PRODUCER_CONFIG = "kafka.producer_config";

  public static final String S3_EMITTER_BUCKET = "s3.bucket";
  public static final String S3_EMITTER_REGION = "s3.region";
  public static final String S3_EMITTER_ENDPOINT = "s3.endpoint";
  public static final String S3_EMITTER_PREFIX = "s3.prefix";
  public static final String S3_EMITTER_ACCESS_KEY = "s3.access_key";
  public static final String S3_EMITTER_SECRET_KEY = "s3.secret_key";
  public static final String S3_EMITTER_PROFILE = "s3.profile";
  public static final String S3_EMITTER_FILE_NAME = "s3.filename";

  public static final String COALESCE_KEY = "coalesce_jobs";
  public static final String PATCH_ENABLED = "patch.enabled";
  public static final String LEGACY_LINEAGE_CLEANUP = "legacyLineageCleanup.enabled";
  public static final String DISABLE_SYMLINK_RESOLUTION = "disableSymlinkResolution";

  public static final String STAGE_METADATA_COALESCING = "stage_metadata_coalescing";
  public static final String STREAMING_JOB = "streaming_job";
  public static final String STREAMING_HEARTBEAT = "streaming_heartbeat";
  public static final String ORRERY_FLOW_NAME = "flow_name";
  public static final String DATASET_ENV_KEY = "metadata.dataset.env";
  public static final String DATASET_HIVE_PLATFORM_ALIAS = "metadata.dataset.hivePlatformAlias";
  public static final String DATASET_LOWERCASE_URNS = "metadata.dataset.lowerCaseUrns";

  public static final String DATASET_MATERIALIZE_KEY = "metadata.dataset.materialize";
  public static final String DATASET_PLATFORM_INSTANCE_KEY = "metadata.dataset.platformInstance";
  public static final String DATASET_CONNECTIONS_KEY = "metadata.dataset.connections";
  public static final String DATASET_INCLUDE_SCHEMA_METADATA_DEPRECATED_ALIAS =
      "metadata.dataset.experimental_include_schema_metadata";
  public static final String DATASET_INCLUDE_SCHEMA_METADATA =
      "metadata.dataset.include_schema_metadata";
  public static final String SPARK_PLATFORM_INSTANCE_KEY = "platformInstance";
  public static final String REMOVE_PARTITION_PATTERN = "metadata.remove_partition_pattern";
  public static final String SPARK_APP_NAME = "spark.app.name";
  public static final String SPARK_MASTER = "spark.master";
  public static final String PLATFORM_KEY = "platform";
  public static final String PATH_SPEC_LIST_KEY = "path_spec_list";
  public static final String FILE_PARTITION_REGEXP_PATTERN = "file_partition_regexp";
  public static final String FABRIC_TYPE_KEY = "env";
  public static final String PLATFORM_INSTANCE_KEY = "platformInstance";
  public static final String DATABRICKS_CLUSTER_KEY = "databricks.cluster";
  public static final String PIPELINE_KEY = "metadata.pipeline";
  public static final String PIPELINE_PLATFORM_INSTANCE_KEY = PIPELINE_KEY + ".platformInstance";
  public static final String ENABLE_ENHANCED_MERGE_INTO_EXTRACTION =
      "metadata.dataset.enableEnhancedMergeIntoExtraction";

  public static final String CAPTURE_COLUMN_LEVEL_LINEAGE = "captureColumnLevelLineage";
  public static final String COLUMN_LINEAGE_INCLUDE_INDIRECT = "columnLineage.includeIndirect";

  public static final String TAGS_KEY = "tags";

  public static final String DOMAINS_KEY = "domains";

  private static final Logger log = LoggerFactory.getLogger(SparkConfigParser.class);
  public static final String SPARK_DATABRICKS_CLUSTER_USAGE_TAGS_CLUSTER_ALL_TAGS =
      "spark.databricks.clusterUsageTags.clusterAllTags";

  private static final ObjectMapper mapper = new ObjectMapper();

  private SparkConfigParser() {}

  public static Properties moveKeysToRoot(Properties properties, String prefix) {
    Properties newProperties = new Properties();
    Enumeration<?> propertyNames = properties.propertyNames();

    while (propertyNames.hasMoreElements()) {
      String key = (String) propertyNames.nextElement();
      String value = properties.getProperty(key);

      if (key.startsWith(prefix)) {
        key = key.substring(prefix.length());
      }

      newProperties.setProperty(key, value);
      log.info("Setting property {} to {}", key, value);
    }

    return newProperties;
  }

  public static Config parsePropertiesToConfig(Properties properties) {
    properties
        .keySet()
        .removeIf(
            o ->
                (!o.toString().startsWith("spark.orrery.")
                    && !o.toString()
                        .startsWith(SPARK_DATABRICKS_CLUSTER_USAGE_TAGS_CLUSTER_ALL_TAGS)));
    properties = SparkConfigParser.moveKeysToRoot(properties, "spark.orrery.");
    return ConfigFactory.parseProperties(properties);
  }

  public static Config parseSparkConfig() {
    if (SparkEnv.get() == null) {
      return ConfigFactory.empty();
    }

    SparkConf conf = SparkEnv.get().conf();
    String propertiesString =
        Arrays.stream(conf.getAllWithPrefix("spark.orrery."))
            .map(tup -> tup._1 + "= \"" + tup._2 + "\"")
            .collect(Collectors.joining("\n"));

    return ConfigFactory.parseString(propertiesString);
  }

  public static Optional<Map<String, String>> getDatabricksClusterTags(
      String databricksClusterTags) {
    try {
      List<Map<String, String>> list =
          mapper.readValue(
              databricksClusterTags, new TypeReference<List<Map<String, String>>>() {});
      Map<String, String> hashMap = new HashMap<>();
      for (Map<String, String> map : list) {
        hashMap.put(map.get("key"), map.get("value"));
      }
      return Optional.of(hashMap);
    } catch (Exception e) {
      log.warn("Error parsing databricks cluster tags", e);
    }
    return Optional.empty();
  }

  public static OrreryOpenlineageConfig sparkConfigToOrreryOpenlineageConf(
      Config sparkConfig, SparkAppContext sparkAppContext) {
    OrreryOpenlineageConfig.OrreryOpenlineageConfigBuilder builder =
        OrreryOpenlineageConfig.builder();
    builder.isSpark(true);
    builder.filePartitionRegexpPattern(
        SparkConfigParser.getFilePartitionRegexpPattern(sparkConfig));
    builder.fabricType(SparkConfigParser.getCommonFabricType(sparkConfig));
    builder.includeSchemaMetadata(SparkConfigParser.isIncludeSchemaMetadata(sparkConfig));
    builder.materializeDataset(SparkConfigParser.isDatasetMaterialize(sparkConfig));
    builder.pathSpecs(SparkConfigParser.getPathSpecListMap(sparkConfig));
    String pipelineName = SparkConfigParser.getPipelineName(sparkConfig, sparkAppContext);
    if (pipelineName != null) {
      builder.pipelineName(pipelineName);
    }
    builder.platformInstance(SparkConfigParser.getPlatformInstance(sparkConfig));
    builder.commonDatasetPlatformInstance(SparkConfigParser.getCommonPlatformInstance(sparkConfig));
    builder.connectionInstanceMap(SparkConfigParser.getConnectionInstanceMap(sparkConfig));
    builder.hivePlatformAlias(SparkConfigParser.getHivePlatformAlias(sparkConfig));
    builder.usePatch(SparkConfigParser.isPatchEnabled(sparkConfig));
    builder.removeLegacyLineage(SparkConfigParser.isLegacyLineageCleanupEnabled(sparkConfig));
    builder.disableSymlinkResolution(SparkConfigParser.isDisableSymlinkResolution(sparkConfig));
    builder.lowerCaseDatasetUrns(SparkConfigParser.isLowerCaseDatasetUrns(sparkConfig));
    builder.captureColumnLevelLineage(SparkConfigParser.isCaptureColumnLevelLineage(sparkConfig));
    builder.includeIndirectColumnLineage(
        SparkConfigParser.isIncludeIndirectColumnLineage(sparkConfig));
    builder.enhancedMergeIntoExtraction(
        SparkConfigParser.isEnhancedMergeIntoExtractionEnabled(sparkConfig));
    try {
      String parentJob = SparkConfigParser.getParentJobKey(sparkConfig);
      if (parentJob != null) {
        builder.parentJobUrn(DataJobUrn.createFromString(parentJob));
      }
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
    return builder.build();
  }

  public static FabricType getCommonFabricType(Config orreryConfig) {
    String fabricTypeString =
        orreryConfig.hasPath(DATASET_ENV_KEY)
            ? orreryConfig.getString(DATASET_ENV_KEY).toUpperCase()
            : "PROD";
    FabricType fabricType = null;
    try {
      fabricType = FabricType.valueOf(fabricTypeString);
    } catch (IllegalArgumentException e) {
      log.warn("Invalid env ({}). Setting env to default PROD", fabricTypeString);
      fabricType = FabricType.PROD;
    }
    return fabricType;
  }

  public static String getHivePlatformAlias(Config orreryConfig) {
    return orreryConfig.hasPath(DATASET_HIVE_PLATFORM_ALIAS)
        ? orreryConfig.getString(DATASET_HIVE_PLATFORM_ALIAS)
        : "hive";
  }

  public static String getCommonPlatformInstance(Config orreryConfig) {
    return orreryConfig.hasPath(DATASET_PLATFORM_INSTANCE_KEY)
        ? orreryConfig.getString(DATASET_PLATFORM_INSTANCE_KEY)
        : null;
  }

  /**
   * Parses the {@code metadata.dataset.connections} block: connection identity (the canonical
   * OpenLineage namespace authority — arn:aws:glue:{region}:{account}, snowflake://{account},
   * postgres://{host}:{port}, ...) -> {platformInstance, env}. Lets a single Spark job stamp the
   * correct per-connection instance on upstreams it reads from multiple accounts/regions/hosts. The
   * platform is implied by the namespace scheme, so it is not part of the value.
   */
  public static Map<String, ConnectionInstanceDetail> getConnectionInstanceMap(
      Config orreryConfig) {
    HashMap<String, ConnectionInstanceDetail> map = new HashMap<>();
    if (!orreryConfig.hasPath(DATASET_CONNECTIONS_KEY)) {
      return map;
    }
    Config connections = orreryConfig.getConfig(DATASET_CONNECTIONS_KEY);
    for (String namespace : connections.root().keySet()) {
      // Quote the key so a namespace with colons/slashes is treated as a single literal path
      // element.
      Config connectionConfig = connections.getConfig("\"" + namespace + "\"");
      ConnectionInstanceDetail.ConnectionInstanceDetailBuilder detailBuilder =
          ConnectionInstanceDetail.builder();
      if (connectionConfig.hasPath(PLATFORM_INSTANCE_KEY)) {
        detailBuilder.platformInstance(
            Optional.of(connectionConfig.getString(PLATFORM_INSTANCE_KEY)));
      }
      if (connectionConfig.hasPath(FABRIC_TYPE_KEY)) {
        String envValue = connectionConfig.getString(FABRIC_TYPE_KEY);
        try {
          // Normalize + validate the env at config-load time so a bad value is reported once,
          // loudly, with the exact recipe path — instead of being silently dropped to the job
          // default fabric per dataset during URN construction (which would emit URNs under the
          // wrong env and dangle lineage with no actionable signal).
          detailBuilder.env(Optional.of(FabricType.valueOf(envValue.toUpperCase())));
        } catch (IllegalArgumentException e) {
          log.error(
              "Invalid env '{}' for connection '{}' (under {}.\"{}\".{}); must be one of {}. "
                  + "Ignoring this connection's env; its datasets will use the job default fabric.",
              envValue,
              namespace,
              DATASET_CONNECTIONS_KEY,
              namespace,
              FABRIC_TYPE_KEY,
              Arrays.toString(FabricType.values()));
        }
      }
      ConnectionInstanceDetail detail = detailBuilder.build();
      if (!detail.getPlatformInstance().isPresent() && !detail.getEnv().isPresent()) {
        // A block with neither platformInstance nor env (e.g. a typo'd sub-key such as
        // `platform_instance` instead of `platformInstance`) is a phantom "configured" connection
        // that resolves to nothing. Skip it and warn so the misconfig is visible rather than a
        // silent lookup miss later.
        log.warn(
            "Connection '{}' under {} has neither {} nor {} set (check for a typo'd sub-key); "
                + "ignoring it.",
            namespace,
            DATASET_CONNECTIONS_KEY,
            PLATFORM_INSTANCE_KEY,
            FABRIC_TYPE_KEY);
        continue;
      }
      map.put(namespace, detail);
    }
    return map;
  }

  public static Optional<Map<String, String>> getDatabricksTags(Config orreryConfig) {
    return orreryConfig.hasPath(SPARK_DATABRICKS_CLUSTER_USAGE_TAGS_CLUSTER_ALL_TAGS)
        ? getDatabricksClusterTags(
            orreryConfig.getString(SPARK_DATABRICKS_CLUSTER_USAGE_TAGS_CLUSTER_ALL_TAGS))
        : Optional.empty();
  }

  public static String getParentJobKey(Config orreryConfig) {
    return orreryConfig.hasPath(PARENT_JOB_KEY) ? orreryConfig.getString(PARENT_JOB_KEY) : null;
  }

  public static String[] getTags(Config orreryConfig) {
    return orreryConfig.hasPath(TAGS_KEY) ? orreryConfig.getString(TAGS_KEY).split(",") : null;
  }

  public static String[] getDomains(Config orreryConfig) {
    return orreryConfig.hasPath(DOMAINS_KEY)
        ? orreryConfig.getString(DOMAINS_KEY).split(",")
        : null;
  }

  public static String getSparkMaster(Config orreryConfig) {
    return orreryConfig.hasPath(SPARK_MASTER)
        ? orreryConfig
            .getString(SPARK_MASTER)
            .replaceAll(":", "_")
            .replaceAll("/", "_")
            .replaceAll(",", "_")
            .replaceAll("[_]+", "_")
        : "default";
  }

  public static String getRemovePartitionPattern(Config orreryConfig) {
    return orreryConfig.hasPath(REMOVE_PARTITION_PATTERN)
        ? orreryConfig.getString(REMOVE_PARTITION_PATTERN)
        : null;
  }

  public static String getSparkAppName(Config orreryConfig) {
    return orreryConfig.hasPath(SPARK_APP_NAME)
        ? orreryConfig.getString(SPARK_APP_NAME)
        : "default";
  }

  public static Map<String, List<PathSpec>> getPathSpecListMap(Config orreryConfig) {
    HashMap<String, List<PathSpec>> pathSpecMap = new HashMap<>();

    if (orreryConfig.hasPath(PLATFORM_KEY)) {
      for (String key : orreryConfig.getConfig(PLATFORM_KEY).root().keySet()) {
        String aliasKey = PLATFORM_KEY + "." + key;
        List<PathSpec> platformSpecs = new LinkedList<>();
        for (String pathSpecKey : orreryConfig.getConfig(aliasKey).root().keySet()) {
          PathSpec.PathSpecBuilder pathSpecBuilder = PathSpec.builder();
          pathSpecBuilder.alias(pathSpecKey);
          pathSpecBuilder.platform(key);
          if (orreryConfig.hasPath(aliasKey + ".env")) {
            pathSpecBuilder.env(Optional.ofNullable(orreryConfig.getString(aliasKey + ".env")));
          }
          if (orreryConfig.hasPath(aliasKey + "." + PLATFORM_INSTANCE_KEY)) {
            pathSpecBuilder.platformInstance(
                Optional.ofNullable(
                    orreryConfig.getString(aliasKey + "." + PLATFORM_INSTANCE_KEY)));
          }
          if (orreryConfig.hasPath(aliasKey + "." + PATH_SPEC_LIST_KEY)) {
            pathSpecBuilder.pathSpecList(
                Arrays.asList(
                    orreryConfig.getString(aliasKey + "." + PATH_SPEC_LIST_KEY).split(",")));
          }
          platformSpecs.add(pathSpecBuilder.build());
        }
        pathSpecMap.put(key, platformSpecs);
      }
    }
    return pathSpecMap;
  }

  public static String getPlatformInstance(Config pathSpecConfig) {
    return pathSpecConfig.hasPath(PIPELINE_PLATFORM_INSTANCE_KEY)
        ? pathSpecConfig.getString(PIPELINE_PLATFORM_INSTANCE_KEY)
        : null;
  }

  public static String getFilePartitionRegexpPattern(Config config) {
    return config.hasPath(FILE_PARTITION_REGEXP_PATTERN)
        ? config.getString(FILE_PARTITION_REGEXP_PATTERN)
        : null;
  }

  public static int getStreamingHeartbeatSec(Config orreryConfig) {
    return orreryConfig.hasPath(STREAMING_HEARTBEAT)
        ? orreryConfig.getInt(STREAMING_HEARTBEAT)
        : 5 * 60;
  }

  public static boolean isDatasetMaterialize(Config orreryConfig) {
    return orreryConfig.hasPath(DATASET_MATERIALIZE_KEY)
        && orreryConfig.getBoolean(DATASET_MATERIALIZE_KEY);
  }

  public static boolean isLogMcps(Config orreryConfig) {
    if (orreryConfig.hasPath(CONFIG_LOG_MCPS)) {
      return orreryConfig.getBoolean(CONFIG_LOG_MCPS);
    }
    return true;
  }

  public static boolean isIncludeSchemaMetadata(Config orreryConfig) {
    if (orreryConfig.hasPath(DATASET_INCLUDE_SCHEMA_METADATA)) {
      return orreryConfig.getBoolean(DATASET_INCLUDE_SCHEMA_METADATA);
    } else {
      // TODO: Deprecate eventually
      return orreryConfig.hasPath(DATASET_INCLUDE_SCHEMA_METADATA_DEPRECATED_ALIAS)
          && orreryConfig.getBoolean(DATASET_INCLUDE_SCHEMA_METADATA_DEPRECATED_ALIAS);
    }
  }

  public static String getPipelineName(Config orreryConfig, SparkAppContext appContext) {
    String name = appContext != null && appContext.appName != null ? appContext.appName : null;
    if (orreryConfig.hasPath(ORRERY_FLOW_NAME)) {
      name = orreryConfig.getString(ORRERY_FLOW_NAME);
    }
    if (orreryConfig.hasPath(DATABRICKS_CLUSTER_KEY)) {
      return (orreryConfig.getString(DATABRICKS_CLUSTER_KEY) + "_" + name).replaceAll("[,]", "");
    }

    // TODO: appending of platform instance needs to be done at central location
    // like adding constructor to dataflowurl
    if (orreryConfig.hasPath(PIPELINE_PLATFORM_INSTANCE_KEY)) {
      name = orreryConfig.getString(PIPELINE_PLATFORM_INSTANCE_KEY) + "." + name;
    }
    return name;
  }

  public static boolean isCoalesceEnabled(Config orreryConfig) {
    if (!orreryConfig.hasPath(COALESCE_KEY)) {
      return true;
    }
    return orreryConfig.hasPath(COALESCE_KEY) && orreryConfig.getBoolean(COALESCE_KEY);
  }

  public static boolean isPatchEnabled(Config orreryConfig) {
    if (!orreryConfig.hasPath(PATCH_ENABLED)) {
      return false;
    }
    return orreryConfig.hasPath(PATCH_ENABLED) && orreryConfig.getBoolean(PATCH_ENABLED);
  }

  public static boolean isLegacyLineageCleanupEnabled(Config orreryConfig) {
    if (!orreryConfig.hasPath(LEGACY_LINEAGE_CLEANUP)) {
      return false;
    }
    return orreryConfig.hasPath(LEGACY_LINEAGE_CLEANUP)
        && orreryConfig.getBoolean(LEGACY_LINEAGE_CLEANUP);
  }

  public static boolean isDisableSymlinkResolution(Config orreryConfig) {
    if (!orreryConfig.hasPath(DISABLE_SYMLINK_RESOLUTION)) {
      return false;
    }
    return orreryConfig.hasPath(DISABLE_SYMLINK_RESOLUTION)
        && orreryConfig.getBoolean(DISABLE_SYMLINK_RESOLUTION);
  }

  public static boolean isEmitCoalescePeriodically(Config orreryConfig) {
    if (!orreryConfig.hasPath(STAGE_METADATA_COALESCING)) {
      // if databricks tags are present and stage_metadata_coalescing is not present, then default
      // to true for coalescing periodically
      // because on DataBricks platform we don't get application stop event
      return getDatabricksTags(orreryConfig).isPresent() && isCoalesceEnabled(orreryConfig);
    }

    return orreryConfig.hasPath(STAGE_METADATA_COALESCING)
        && orreryConfig.getBoolean(STAGE_METADATA_COALESCING);
  }

  public static boolean isLowerCaseDatasetUrns(Config orreryConfig) {
    return orreryConfig.hasPath(DATASET_LOWERCASE_URNS)
        && orreryConfig.getBoolean(DATASET_LOWERCASE_URNS);
  }

  public static boolean isEnhancedMergeIntoExtractionEnabled(Config orreryConfig) {
    return orreryConfig.hasPath(ENABLE_ENHANCED_MERGE_INTO_EXTRACTION)
        && orreryConfig.getBoolean(ENABLE_ENHANCED_MERGE_INTO_EXTRACTION);
  }

  public static boolean isCaptureColumnLevelLineage(Config orreryConfig) {
    if (orreryConfig.hasPath(CAPTURE_COLUMN_LEVEL_LINEAGE)) {
      return orreryConfig.getBoolean(CAPTURE_COLUMN_LEVEL_LINEAGE);
    }

    return true;
  }

  public static boolean isIncludeIndirectColumnLineage(Config orreryConfig) {
    if (orreryConfig.hasPath(COLUMN_LINEAGE_INCLUDE_INDIRECT)) {
      return orreryConfig.getBoolean(COLUMN_LINEAGE_INCLUDE_INDIRECT);
    }

    return true;
  }
}
