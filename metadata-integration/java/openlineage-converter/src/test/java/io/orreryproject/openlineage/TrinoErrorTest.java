package io.orreryproject.openlineage;

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

public class TrinoErrorTest {
  @Test
  public void testTrinoProducerUrlCausesError() throws Exception {
    URI trinoProducerUri = URI.create("https://github.com/trinodb/trino/");
    OrreryOpenlineageConfig config =
        OrreryOpenlineageConfig.builder().fabricType(FabricType.PROD).build();

    OpenLineage openLineage = new OpenLineage(trinoProducerUri);
    OpenLineage.RunEvent runEvent =
        openLineage
            .newRunEventBuilder()
            .eventTime(ZonedDateTime.now())
            .eventType(OpenLineage.RunEvent.EventType.COMPLETE)
            .run(openLineage.newRunBuilder().runId(UUID.randomUUID()).build())
            .job(
                openLineage
                    .newJobBuilder()
                    .namespace("trino://my-cluster")
                    .name("test_query")
                    .facets(openLineage.newJobFacetsBuilder().build())
                    .build())
            .inputs(java.util.Collections.emptyList())
            .outputs(java.util.Collections.emptyList())
            .build();

    OrreryJob orreryJob = OpenLineageToOrrery.convertRunEventToJob(runEvent, config);
    assertNotNull(orreryJob);
  }
}
