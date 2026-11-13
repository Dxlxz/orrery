package orrery.spark;

import static com.linkedin.metadata.Constants.*;
import static orrery.spark.converter.SparkStreamingEventToOrrery.*;
import static io.orreryproject.openlineage.converter.OpenLineageToOrrery.*;
import static io.orreryproject.openlineage.utils.OrreryUtils.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedin.common.GlobalTags;
import com.linkedin.common.UrnArray;
import com.linkedin.common.urn.DataJobUrn;
import com.linkedin.data.DataMap;
import com.linkedin.data.template.JacksonDataTemplateCodec;
import com.linkedin.data.template.StringMap;
import com.linkedin.dataprocess.DataProcessInstanceRelationships;
import com.linkedin.dataprocess.RunResultType;
import com.linkedin.domain.Domains;
import com.linkedin.mxe.MetadataChangeProposal;
import orrery.client.Emitter;
import orrery.client.file.FileEmitter;
import orrery.client.kafka.KafkaEmitter;
import orrery.client.rest.RestEmitter;
import orrery.client.s3.S3Emitter;
import orrery.event.EventFormatter;
import orrery.event.MetadataChangeProposalWrapper;
import orrery.spark.conf.FileOrreryEmitterConfig;
import orrery.spark.conf.KafkaOrreryEmitterConfig;
import orrery.spark.conf.RestOrreryEmitterConfig;
import orrery.spark.conf.S3OrreryEmitterConfig;
import orrery.spark.conf.SparkLineageConf;
import io.orreryproject.openlineage.converter.OpenLineageToOrrery;
import io.orreryproject.openlineage.dataset.OrreryDataset;
import io.orreryproject.openlineage.dataset.OrreryJob;
import io.openlineage.client.OpenLineage;
import io.openlineage.client.OpenLineageClientUtils;
import io.openlineage.spark.agent.EventEmitter;
import io.openlineage.spark.api.SparkOpenLineageConfig;
import java.io.IOException;
import java.net.URISyntaxException;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class OrreryEventEmitter extends EventEmitter {
  private final AtomicBoolean streaming = new AtomicBoolean(false);

  private final List<OrreryJob> _orreryJobs = new LinkedList<>();
  private final Map<String, MetadataChangeProposalWrapper> schemaMap = new HashMap<>();
  private SparkLineageConf orreryConf;
  private static final int DEFAULT_TIMEOUT_SEC = 10;
  private final ObjectMapper objectMapper;
  private final JacksonDataTemplateCodec dataTemplateCodec;

  private final EventFormatter eventFormatter = new EventFormatter();

  public OrreryEventEmitter(SparkOpenLineageConfig config, String applicationJobName)
      throws URISyntaxException {
    super(config, applicationJobName);
    objectMapper = new ObjectMapper().setSerializationInclusion(JsonInclude.Include.NON_NULL);
    int maxSize =
        Integer.parseInt(
            System.getenv()
                .getOrDefault(INGESTION_MAX_SERIALIZED_STRING_LENGTH, MAX_JACKSON_STRING_SIZE));
    objectMapper
        .getFactory()
        .setStreamReadConstraints(StreamReadConstraints.builder().maxStringLength(maxSize).build());
    dataTemplateCodec = new JacksonDataTemplateCodec(objectMapper.getFactory());
  }

  private Optional<Emitter> getEmitter() {
    Optional<Emitter> emitter = Optional.empty();
    if (orreryConf.getOrreryEmitterConfig() != null) {
      if (orreryConf.getOrreryEmitterConfig() instanceof RestOrreryEmitterConfig) {
        RestOrreryEmitterConfig orreryRestEmitterConfig =
            (RestOrreryEmitterConfig) orreryConf.getOrreryEmitterConfig();
        emitter = Optional.of(new RestEmitter(orreryRestEmitterConfig.getRestEmitterConfig()));
      } else if (orreryConf.getOrreryEmitterConfig() instanceof KafkaOrreryEmitterConfig) {
        KafkaOrreryEmitterConfig orreryKafkaEmitterConfig =
            (KafkaOrreryEmitterConfig) orreryConf.getOrreryEmitterConfig();
        try {
          emitter =
              Optional.of(
                  new KafkaEmitter(
                      orreryKafkaEmitterConfig.getKafkaEmitterConfig(),
                      orreryKafkaEmitterConfig.getMcpTopic()));
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      } else if (orreryConf.getOrreryEmitterConfig() instanceof FileOrreryEmitterConfig) {
        FileOrreryEmitterConfig orreryFileEmitterConfig =
            (FileOrreryEmitterConfig) orreryConf.getOrreryEmitterConfig();
        emitter = Optional.of(new FileEmitter(orreryFileEmitterConfig.getFileEmitterConfig()));
      } else if (orreryConf.getOrreryEmitterConfig() instanceof S3OrreryEmitterConfig) {
        S3OrreryEmitterConfig orreryFileEmitterConfig =
            (S3OrreryEmitterConfig) orreryConf.getOrreryEmitterConfig();
        try {
          emitter = Optional.of(new S3Emitter(orreryFileEmitterConfig.getS3EmitterConfig()));
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      } else {
        log.error(
            "Orrery Transport {} not recognized. Orrery Lineage emission will not work",
            RestOrreryEmitterConfig.class.getName());
      }
    } else {
      log.error("No Transport set. Orrery Lineage emission will not work");
    }
    return emitter;
  }

  public Optional<OrreryJob> convertOpenLineageRunEventToOrreryJob(OpenLineage.RunEvent event) {
    Optional<OrreryJob> orreryJob = Optional.empty();
    try {
      log.debug("Emitting lineage: {}", OpenLineageClientUtils.toJson(event));
      if (!isStreaming()) {
        orreryJob =
            Optional.ofNullable(convertRunEventToJob(event, orreryConf.getOpenLineageConf()));
        if (!orreryJob.isPresent()) {
          return orreryJob;
        }
        log.info(
            "Converted Job: {}, from {}", orreryJob.get(), OpenLineageClientUtils.toJson(event));
        _orreryJobs.add(orreryJob.get());
        return orreryJob;
      }
    } catch (IOException | URISyntaxException e) {
      throw new RuntimeException("Error: " + e.getMessage(), e);
    }
    return orreryJob;
  }

  public void emit(OpenLineage.RunEvent event) {
    long startTime = System.currentTimeMillis();
    // We have to serialize and deserialize the event to make sure the event is in the correct
    // format
    event = OpenLineageClientUtils.runEventFromJson(OpenLineageClientUtils.toJson(event));
    Optional<OrreryJob> job = convertOpenLineageRunEventToOrreryJob(event);
    if (!job.isPresent()) {
      return;
    }

    if (!orreryConf.getTags().isEmpty()) {
      GlobalTags tags = OpenLineageToOrrery.generateTags(orreryConf.getTags());
      job.get().setFlowGlobalTags(tags);
    }

    if (!orreryConf.getDomains().isEmpty()) {
      Domains domains = OpenLineageToOrrery.generateDomains(orreryConf.getDomains());
      job.get().setFlowDomains(domains);
    }

    if (isStreaming()) {
      log.info("Streaming mode is enabled. Skipping lineage emission.");
      return;
    }
    if (!orreryConf.isCoalesceEnabled()) {
      log.info("Emitting lineage");
      try {
        emitMcps(job.get().toMcps(orreryConf.getOpenLineageConf()));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      log.debug(
          "Emitting non-coalesced lineage completed successfully: {}",
          OpenLineageClientUtils.toJson(event));
    }
    if (orreryConf.isCoalesceEnabled() && orreryConf.isEmitCoalescePeriodically()) {
      log.info("Emitting coalesced lineage periodically");
      emitCoalesced();
      log.debug(
          "Collecting coalesced lineage periodically completed successfully: {}",
          OpenLineageClientUtils.toJson(event));
    }
    long elapsedTime = System.currentTimeMillis() - startTime;
    log.info("Collecting lineage successfully in {} ms", elapsedTime);
  }

  public void emitCoalesced() {
    long startTime = System.currentTimeMillis();

    if (isStreaming()) {
      log.info("Streaming mode is enabled. Skipping lineage emission.");
      return;
    }

    if (orreryConf.isCoalesceEnabled()) {
      List<MetadataChangeProposal> mcps = generateCoalescedMcps();
      log.info("Emitting Coalesced lineage completed successfully");
      emitMcps(mcps);
    }
    long elapsedTime = System.currentTimeMillis() - startTime;
    log.info("Emitting coalesced lineage completed in {} ms", elapsedTime);
  }

  public List<MetadataChangeProposal> generateCoalescedMcps() {
    List<MetadataChangeProposal> mcps = new ArrayList<>();

    if (_orreryJobs.isEmpty()) {
      log.warn("No lineage events to emit. Maybe the spark job finished prematurely?");
      return mcps;
    }

    OrreryJob orreryJob = OrreryJob.builder().build();
    AtomicLong minStartTime = new AtomicLong(Long.MAX_VALUE);
    AtomicLong maxEndTime = new AtomicLong();
    _orreryJobs.forEach(
        storedOrreryJob -> {
          log.info("Merging job stored job {} with {}", storedOrreryJob, orreryJob);
          DataJobUrn jobUrn =
              jobUrn(
                  storedOrreryJob.getFlowUrn(), storedOrreryJob.getFlowUrn().getFlowIdEntity());
          orreryJob.setJobUrn(jobUrn);
          orreryJob.setFlowUrn(storedOrreryJob.getFlowUrn());
          orreryJob.setFlowPlatformInstance(storedOrreryJob.getFlowPlatformInstance());
          if ((orreryJob.getJobInfo() == null) && (storedOrreryJob.getJobInfo() != null)) {
            orreryJob.setJobInfo(storedOrreryJob.getJobInfo());
            orreryJob.getJobInfo().setName(storedOrreryJob.getFlowUrn().getFlowIdEntity());
          }
          if (storedOrreryJob.getJobInfo() != null
              && storedOrreryJob.getJobInfo().getCustomProperties() != null) {
            if (orreryJob.getJobInfo().getCustomProperties() == null) {
              orreryJob
                  .getJobInfo()
                  .setCustomProperties(storedOrreryJob.getJobInfo().getCustomProperties());
            } else {
              Map<String, String> mergedProperties =
                  Stream.of(
                          orreryJob.getJobInfo().getCustomProperties(),
                          storedOrreryJob.getJobInfo().getCustomProperties())
                      .flatMap(map -> map.entrySet().stream())
                      .collect(
                          Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1));
              orreryJob.getJobInfo().setCustomProperties(new StringMap(mergedProperties));
            }
          }
          if (orreryJob.getDataFlowInfo() == null) {
            orreryJob.setDataFlowInfo(storedOrreryJob.getDataFlowInfo());
          }

          if (storedOrreryJob.getStartTime() < minStartTime.get()) {
            minStartTime.set(storedOrreryJob.getStartTime());
          }

          if (storedOrreryJob.getEndTime() > maxEndTime.get()) {
            maxEndTime.set(storedOrreryJob.getEndTime());
          }

          mergeDatasets(storedOrreryJob.getOutSet(), orreryJob.getOutSet());

          mergeDatasets(storedOrreryJob.getInSet(), orreryJob.getInSet());

          mergeDataProcessInstance(orreryJob, storedOrreryJob);

          mergeCustomProperties(orreryJob, storedOrreryJob);
        });

    orreryJob.setStartTime(minStartTime.get());
    orreryJob.setEndTime(maxEndTime.get());
    if (!orreryConf.getTags().isEmpty()) {
      GlobalTags tags = OpenLineageToOrrery.generateTags(orreryConf.getTags());
      orreryJob.setFlowGlobalTags(tags);
    }

    if (!orreryConf.getDomains().isEmpty()) {
      Domains domains = OpenLineageToOrrery.generateDomains(orreryConf.getDomains());
      orreryJob.setFlowDomains(domains);
    }
    try {
      if (orreryConf.getOpenLineageConf().getParentJobUrn() != null) {
        orreryJob.getParentJobs().add(orreryConf.getOpenLineageConf().getParentJobUrn());
      }
    } catch (ClassCastException e) {
      log.warn(
          orreryConf.getOpenLineageConf().getParentJobUrn()
              + " is not a valid Datajob URN. Skipping setting up upstream job.");
    }

    log.info("Generating MCPs for job: {}", orreryJob);
    try {
      return orreryJob.toMcps(orreryConf.getOpenLineageConf());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static void mergeDatasets(
      Set<OrreryDataset> storedOrreryJob, Set<OrreryDataset> orreryJob) {
    for (OrreryDataset dataset : storedOrreryJob) {
      Optional<OrreryDataset> oldDataset =
          orreryJob.stream().filter(ds -> ds.getUrn().equals(dataset.getUrn())).findFirst();
      if (oldDataset.isPresent()) {
        if (dataset.getSchemaMetadata() != null) {
          oldDataset.get().setSchemaMetadata(dataset.getSchemaMetadata());
        }
        if (dataset.getLineage() != null) {
          oldDataset.get().setLineage(dataset.getLineage());
        }
      } else {
        orreryJob.add(dataset);
      }
    }
  }

  private static void mergeDataProcessInstance(OrreryJob orreryJob, OrreryJob storedOrreryJob) {
    // To merge multiple events into one DataProcess we should do the following steps:
    // 1. A run is only in SUCCESS if all the process instance status are SUCCESS
    // 2. A run is in failed state if any of the run events is in FAILED/UNKNOWN/SKIPPED state
    //
    // We should set as id the first event to make sure it won't change if we ingest periodically
    // coalesced data
    // Todo: Status can be SUCCESS only if all the process instance status are SUCCESS
    if (orreryJob.getDataProcessInstanceUrn() == null) {
      orreryJob.setDataProcessInstanceUrn(storedOrreryJob.getDataProcessInstanceUrn());
    }

    if (storedOrreryJob.getEventTime() > orreryJob.getEventTime()) {
      orreryJob.setEventTime(storedOrreryJob.getEventTime());
      orreryJob.setDataProcessInstanceProperties(
          storedOrreryJob.getDataProcessInstanceProperties());
      DataProcessInstanceRelationships dataProcessInstanceRelationships =
          new DataProcessInstanceRelationships();
      dataProcessInstanceRelationships.setParentTemplate(orreryJob.getJobUrn());
      dataProcessInstanceRelationships.setUpstreamInstances(new UrnArray());
      orreryJob.setDataProcessInstanceRelationships(dataProcessInstanceRelationships);
    }
    log.info("DataProcessInstanceRunEvent: {}", storedOrreryJob.getDataProcessInstanceRunEvent());
    if ((storedOrreryJob.getDataProcessInstanceRunEvent() != null)
        && (storedOrreryJob.getDataProcessInstanceRunEvent().getResult() != null)) {
      RunResultType result =
          storedOrreryJob.getDataProcessInstanceRunEvent().getResult().getType();
      if (orreryJob.getDataProcessInstanceRunEvent() == null) {
        orreryJob.setDataProcessInstanceRunEvent(
            storedOrreryJob.getDataProcessInstanceRunEvent());
      } else if (result == RunResultType.FAILURE) {
        orreryJob.setDataProcessInstanceRunEvent(
            storedOrreryJob.getDataProcessInstanceRunEvent());
      }
    }
    log.info("DataProcessInstanceRunEvent: {}", orreryJob.getDataProcessInstanceRunEvent());
  }

  private void mergeCustomProperties(OrreryJob orreryJob, OrreryJob storedOrreryJob) {
    if (storedOrreryJob.getDataFlowInfo().getCustomProperties() != null) {
      if (orreryJob.getDataFlowInfo().getCustomProperties() == null) {
        orreryJob
            .getDataFlowInfo()
            .setCustomProperties(storedOrreryJob.getDataFlowInfo().getCustomProperties());
      } else {
        Map<String, String> mergedProperties =
            Stream.of(
                    orreryJob.getDataFlowInfo().getCustomProperties(),
                    storedOrreryJob.getDataFlowInfo().getCustomProperties())
                .flatMap(map -> map.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1));
        mergedProperties.put("finishedAt", ZonedDateTime.now(ZoneOffset.UTC).toString());

        if (orreryConf.getSparkAppContext() != null) {
          if (orreryConf.getSparkAppContext().getStartTime() != null) {
            mergedProperties.put(
                "startedAt",
                ZonedDateTime.ofInstant(
                        Instant.ofEpochMilli(orreryConf.getSparkAppContext().getStartTime()),
                        ZoneOffset.UTC)
                    .toString());
          }
          if (orreryConf.getSparkAppContext().getAppAttemptId() != null) {
            mergedProperties.put("attemptId", orreryConf.getSparkAppContext().getAppAttemptId());
          }
          if (orreryConf.getSparkAppContext().getSparkUser() != null) {
            mergedProperties.put("sparkUser", orreryConf.getSparkAppContext().getSparkUser());
          }

          if (orreryConf.getSparkAppContext().getAppId() != null) {
            mergedProperties.put("appId", orreryConf.getSparkAppContext().getAppId());
          }

          if (orreryConf.getSparkAppContext().getDatabricksTags() != null) {
            mergedProperties.putAll(orreryConf.getSparkAppContext().getDatabricksTags());
          }
        }
        orreryJob.getDataFlowInfo().setCustomProperties(new StringMap(mergedProperties));
      }
    }
  }

  protected void emitMcps(List<MetadataChangeProposal> mcps) {
    Optional<Emitter> emitter = getEmitter();
    if (emitter.isPresent()) {
      mcps.stream()
          .map(
              mcp -> {
                try {
                  if (this.orreryConf.isLogMcps()) {
                    DataMap map = mcp.data();
                    String serializedMCP = dataTemplateCodec.mapToString(map);
                    log.info("emitting mcpw: {}", serializedMCP);
                  } else {
                    log.info(
                        "emitting aspect: {} for urn: {}", mcp.getAspectName(), mcp.getEntityUrn());
                  }
                  return emitter.get().emit(mcp);
                } catch (IOException ioException) {
                  log.error("Failed to emit metadata to Orrery", ioException);
                  return null;
                }
              })
          .filter(Objects::nonNull)
          .collect(Collectors.toList())
          .forEach(
              future -> {
                try {
                  log.info(future.get(DEFAULT_TIMEOUT_SEC, TimeUnit.SECONDS).toString());
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                  // log error, but don't impact thread
                  log.error("Failed to emit metadata to Orrery", e);
                }
              });
      try {
        emitter.get().close();
      } catch (IOException e) {
        log.error("Issue while closing emitter" + e);
      }
    }
  }

  public void setConfig(SparkLineageConf sparkConfig) {
    this.orreryConf = sparkConfig;
  }

  public boolean isStreaming() {
    return streaming.get();
  }

  public void setStreaming(boolean enabled) {
    streaming.set(enabled);
  }
}
