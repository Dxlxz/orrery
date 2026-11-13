package orrery.spark;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import orrery.spark.conf.SparkAppContext;
import orrery.spark.conf.SparkConfigParser;
import orrery.spark.conf.SparkLineageConf;
import io.orreryproject.openlineage.config.OrreryOpenlineageConfig;
import io.orreryproject.openlineage.dataset.HdfsPathDataset;
import io.orreryproject.openlineage.dataset.SparkDataset;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
public class HdfsPathDatasetTest {

  @Test
  public void testNoPathSpecList()
      throws InstantiationException, IllegalArgumentException, URISyntaxException {
    Config orreryConfig =
        ConfigFactory.parseMap(
            new HashMap<String, Object>() {
              {
                put(SparkConfigParser.DATASET_ENV_KEY, "PROD");
              }
            });

    SparkLineageConf.SparkLineageConfBuilder sparkLineageConfBuilder = SparkLineageConf.builder();
    sparkLineageConfBuilder.openLineageConf(
        SparkConfigParser.sparkConfigToOrreryOpenlineageConf(
            orreryConfig, new SparkAppContext()));

    log.warn("Test log");
    SparkDataset dataset =
        HdfsPathDataset.create(
            new URI("s3://my-bucket/foo/tests/bar.avro"),
            sparkLineageConfBuilder.build().getOpenLineageConf());
    assertEquals(
        "urn:li:dataset:(urn:li:dataPlatform:s3,my-bucket/foo/tests/bar.avro,PROD)",
        dataset.urn().toString());
  }

  @Test
  public void testPathSpecList()
      throws InstantiationException, IllegalArgumentException, URISyntaxException {
    Config orreryConfig =
        ConfigFactory.parseMap(
            new HashMap<String, Object>() {
              {
                put(SparkConfigParser.DATASET_ENV_KEY, "PROD");
                put(
                    SparkConfigParser.PLATFORM_KEY
                        + "."
                        + "s3"
                        + "."
                        + SparkConfigParser.PATH_SPEC_LIST_KEY,
                    String.join(
                        ",", "s3a://wrong-my-bucket/foo/{table}", "s3a://my-bucket/foo/{table}"));
              }
            });
    SparkLineageConf.SparkLineageConfBuilder sparkLineageConfBuilder = SparkLineageConf.builder();
    sparkLineageConfBuilder.openLineageConf(
        SparkConfigParser.sparkConfigToOrreryOpenlineageConf(
            orreryConfig, new SparkAppContext()));

    SparkDataset dataset =
        HdfsPathDataset.create(
            new URI("s3a://my-bucket/foo/tests/bar.avro"),
            sparkLineageConfBuilder.build().getOpenLineageConf());
    assertEquals(
        "urn:li:dataset:(urn:li:dataPlatform:s3,my-bucket/foo/tests,PROD)",
        dataset.urn().toString());
  }

  @Test
  public void testUrisWithPartitionRegexp()
      throws InstantiationException, IllegalArgumentException, URISyntaxException {
    Config rawOrreryConfig =
        ConfigFactory.parseMap(
            new HashMap<String, Object>() {
              {
                put(SparkConfigParser.DATASET_ENV_KEY, "PROD");
                put(SparkConfigParser.FILE_PARTITION_REGEXP_PATTERN, "year=.*/month=.*/day=.*");
              }
            });

    SparkLineageConf.SparkLineageConfBuilder sparkLineageConfBuilder = SparkLineageConf.builder();
    sparkLineageConfBuilder.openLineageConf(
        SparkConfigParser.sparkConfigToOrreryOpenlineageConf(
            rawOrreryConfig, new SparkAppContext()));
    OrreryOpenlineageConfig orreryConfig = sparkLineageConfBuilder.build().getOpenLineageConf();

    SparkDataset dataset =
        HdfsPathDataset.create(
            new URI("s3://bucket-a/kafka_backup/my-table/year=2022/month=10/day=11/my-file.tx"),
            orreryConfig);
    assertEquals(
        "urn:li:dataset:(urn:li:dataPlatform:s3,bucket-a/kafka_backup/my-table,PROD)",
        dataset.urn().toString());

    dataset =
        HdfsPathDataset.create(
            new URI("s3://bucket-b/kafka_backup/my-table/year=2023/month=11/day=23/my-file.tx"),
            orreryConfig);
    assertEquals(
        "urn:li:dataset:(urn:li:dataPlatform:s3,bucket-b/kafka_backup/my-table,PROD)",
        dataset.urn().toString());

    dataset =
        HdfsPathDataset.create(
            new URI(
                "s3://bucket-c/my-backup/my-other-folder/my-table/year=2023/month=11/day=23/my-file.tx"),
            orreryConfig);
    assertEquals(
        "urn:li:dataset:(urn:li:dataPlatform:s3,bucket-c/my-backup/my-other-folder/my-table,PROD)",
        dataset.urn().toString());

    dataset =
        HdfsPathDataset.create(
            new URI("s3://bucket-d/kafka_backup/my-table/non-partitioned/"), orreryConfig);
    assertEquals(
        "urn:li:dataset:(urn:li:dataPlatform:s3,bucket-d/kafka_backup/my-table/non-partitioned,PROD)",
        dataset.urn().toString());
  }

  @Test
  public void testNoMatchPathSpecListWithFolder()
      throws InstantiationException, IllegalArgumentException, URISyntaxException {
    Config rawOrreryConfig =
        ConfigFactory.parseMap(
            new HashMap<String, Object>() {
              {
                put(SparkConfigParser.DATASET_ENV_KEY, "PROD");
              }
            });
    String gcsPath =
        "gcs://gcs-spike-standard-offerwall-dev-useast1/events_creation_timestamp_enhanced";
    String expectedUrn =
        "urn:li:dataset:(urn:li:dataPlatform:gcs,gcs-spike-standard-offerwall-dev-useast1/events_creation_timestamp_enhanced,PROD)";

    SparkLineageConf.SparkLineageConfBuilder sparkLineageConfBuilder = SparkLineageConf.builder();
    sparkLineageConfBuilder.openLineageConf(
        SparkConfigParser.sparkConfigToOrreryOpenlineageConf(
            rawOrreryConfig, new SparkAppContext()));
    OrreryOpenlineageConfig orreryConfig = sparkLineageConfBuilder.build().getOpenLineageConf();

    SparkDataset dataset = HdfsPathDataset.create(new URI(gcsPath), orreryConfig);
    assertEquals(expectedUrn, dataset.urn().toString());
  }

  @Test
  public void testNoMatchPathSpecList()
      throws InstantiationException, IllegalArgumentException, URISyntaxException {
    Config orreryConfig =
        ConfigFactory.parseMap(
            new HashMap<String, Object>() {
              {
                put(SparkConfigParser.DATASET_ENV_KEY, "PROD");
                put(
                    SparkConfigParser.PLATFORM_KEY
                        + "."
                        + "s3"
                        + "."
                        + SparkConfigParser.PATH_SPEC_LIST_KEY,
                    String.join(",", "s3a://wrong-my-bucket/foo/{table}"));
              }
            });
    SparkLineageConf sparkLineageConf =
        SparkLineageConf.toSparkLineageConf(orreryConfig, null, null);
    SparkDataset dataset =
        HdfsPathDataset.create(
            new URI("s3a://my-bucket/foo/tests/bar.avro"), sparkLineageConf.getOpenLineageConf());
    assertEquals(
        "urn:li:dataset:(urn:li:dataPlatform:s3,my-bucket/foo/tests/bar.avro,PROD)",
        dataset.urn().toString());
  }

  @Test
  public void testPathSpecListPlatformInstance()
      throws InstantiationException, IllegalArgumentException, URISyntaxException {
    Config orreryConfig =
        ConfigFactory.parseMap(
            new HashMap<String, Object>() {
              {
                put(SparkConfigParser.DATASET_ENV_KEY, "PROD");
                put(SparkConfigParser.DATASET_PLATFORM_INSTANCE_KEY, "instance");
                put(
                    SparkConfigParser.PLATFORM_KEY
                        + "."
                        + "s3"
                        + "."
                        + SparkConfigParser.PATH_SPEC_LIST_KEY,
                    String.join(
                        ",", "s3a://wrong-my-bucket/foo/{table}", "s3a://my-bucket/foo/{table}"));
              }
            });
    SparkLineageConf sparkLineageConf =
        SparkLineageConf.toSparkLineageConf(orreryConfig, null, null);

    SparkDataset dataset =
        HdfsPathDataset.create(
            new URI("s3a://my-bucket/foo/tests/bar.avro"), sparkLineageConf.getOpenLineageConf());
    assertEquals(
        "urn:li:dataset:(urn:li:dataPlatform:s3,my-bucket/foo/tests,PROD)",
        dataset.urn().toString());
  }

  @Test
  public void testPathAliasList()
      throws InstantiationException, IllegalArgumentException, URISyntaxException {
    Config orreryConfig =
        ConfigFactory.parseMap(
            new HashMap<String, Object>() {
              {
                put(SparkConfigParser.DATASET_ENV_KEY, "PROD");
                put(
                    SparkConfigParser.PLATFORM_KEY
                        + "."
                        + "s3"
                        + "."
                        + SparkConfigParser.PATH_SPEC_LIST_KEY,
                    String.join(",", "s3a://my-bucket/{table}"));
              }
            });
    SparkLineageConf sparkLineageConf =
        SparkLineageConf.toSparkLineageConf(orreryConfig, null, null);

    SparkDataset dataset =
        HdfsPathDataset.create(
            new URI("s3a://my-bucket/foo/tests/bar.avro"), sparkLineageConf.getOpenLineageConf());
    assertEquals(
        "urn:li:dataset:(urn:li:dataPlatform:s3,my-bucket/foo,PROD)", dataset.urn().toString());
  }

  // ====================================================================
  // GCS tests
  // ====================================================================
  @Test
  public void testGcsNoPathSpecList()
      throws InstantiationException, IllegalArgumentException, URISyntaxException {
    Config orreryConfig =
        ConfigFactory.parseMap(
            new HashMap<String, Object>() {
              {
                put(SparkConfigParser.DATASET_ENV_KEY, "PROD");
              }
            });
    SparkLineageConf sparkLineageConf =
        SparkLineageConf.toSparkLineageConf(orreryConfig, null, null);

    SparkDataset dataset =
        HdfsPathDataset.create(
            new URI("gs://my-bucket/foo/tests/bar.avro"), sparkLineageConf.getOpenLineageConf());
    assertEquals(
        "urn:li:dataset:(urn:li:dataPlatform:gcs,my-bucket/foo/tests/bar.avro,PROD)",
        dataset.urn().toString());
  }

  @Test
  public void testGcsPathSpecList()
      throws InstantiationException, IllegalArgumentException, URISyntaxException {
    Config orreryConfig =
        ConfigFactory.parseMap(
            new HashMap<String, Object>() {
              {
                put(SparkConfigParser.DATASET_ENV_KEY, "PROD");
                put(
                    SparkConfigParser.PLATFORM_KEY
                        + "."
                        + "gcs"
                        + "."
                        + SparkConfigParser.PATH_SPEC_LIST_KEY,
                    String.join(
                        ",", "s3a://wrong-my-bucket/foo/{table}", "gs://my-bucket/foo/{table}"));
              }
            });
    SparkLineageConf sparkLineageConf =
        SparkLineageConf.toSparkLineageConf(orreryConfig, null, null);

    SparkDataset dataset =
        HdfsPathDataset.create(
            new URI("gs://my-bucket/foo/tests/bar.avro"), sparkLineageConf.getOpenLineageConf());
    assertEquals(
        "urn:li:dataset:(urn:li:dataPlatform:gcs,my-bucket/foo/tests,PROD)",
        dataset.urn().toString());
  }
}
