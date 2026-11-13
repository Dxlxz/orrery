package io.orreryproject.openapi.openlineage.mapping;

import com.linkedin.mxe.MetadataChangeProposal;
import orrery.event.EventFormatter;
import io.orreryproject.openlineage.config.OrreryOpenlineageConfig;
import io.orreryproject.openlineage.converter.OpenLineageToOrrery;
import io.openlineage.client.OpenLineage;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.stream.Stream;
import lombok.Builder;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class RunEventMapper {

  public RunEventMapper() {}

  public Stream<MetadataChangeProposal> map(
      OpenLineage.RunEvent runEvent, RunEventMapper.MappingConfig mappingConfig) {
    EventFormatter eventFormatter = new EventFormatter();
    try {
      return OpenLineageToOrrery.convertRunEventToJob(runEvent, mappingConfig.getOrreryConfig())
          .toMcps(mappingConfig.orreryConfig)
          .stream();
    } catch (IOException | URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  @Builder
  @Getter
  public static class MappingConfig {
    OrreryOpenlineageConfig orreryConfig;
  }
}
