package com.linkedin.metadata.search.elasticsearch.indexbuilder;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import com.linkedin.common.urn.Urn;
import com.linkedin.data.template.StringMap;
import com.linkedin.entity.EnvelopedAspect;
import com.linkedin.metadata.entity.upgrade.OrreryUpgradeResultConditionalPersist;
import com.linkedin.metadata.entity.upgrade.OrreryUpgradeResultStore;
import com.linkedin.metadata.utils.GenericRecordUtils;
import com.linkedin.mxe.MetadataChangeProposal;
import com.linkedin.upgrade.OrreryUpgradeResult;
import com.linkedin.upgrade.OrreryUpgradeState;
import io.orreryproject.metadata.context.OperationContext;
import java.util.Map;
import org.mockito.ArgumentCaptor;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class UpgradeRowWriterHeartbeatTest {
  private OrreryUpgradeResultStore store;
  private OperationContext opContext;
  private Urn urn;
  private UpgradeRowWriterHeartbeat heartbeat;

  @BeforeMethod
  public void setUp() throws Exception {
    store = mock(OrreryUpgradeResultStore.class);
    opContext = mock(OperationContext.class);
    urn = Urn.createFromString("urn:li:orreryUpgrade:" + ClusterMigrationState.WRITERS_UPGRADE_ID);
    heartbeat = new UpgradeRowWriterHeartbeat(store, opContext, urn, "gms-7f9c");
  }

  private static EnvelopedAspect row(Map<String, String> result) {
    return OrreryUpgradeResultConditionalPersist.toEnveloped(
        new OrreryUpgradeResult()
            .setState(OrreryUpgradeState.SUCCEEDED)
            .setResult(new StringMap(result)),
        "3");
  }

  private OrreryUpgradeResult written() throws Exception {
    ArgumentCaptor<MetadataChangeProposal> captor =
        ArgumentCaptor.forClass(MetadataChangeProposal.class);
    verify(store, times(1)).ingest(eq(opContext), captor.capture());
    return GenericRecordUtils.deserializeAspect(
        captor.getValue().getAspect().getValue(),
        captor.getValue().getAspect().getContentType(),
        OrreryUpgradeResult.class);
  }

  @Test
  public void testAReportCarriesTheStartAndKeepsOtherWritersThatStillReport() throws Exception {
    long now = System.currentTimeMillis();
    String other = ClusterMigrationState.writerKey("mae-a1b2");
    String longGone = ClusterMigrationState.writerKey("gms-dead");
    when(store.readLatest(opContext, urn))
        .thenReturn(
            row(
                Map.of(
                    other,
                    ClusterMigrationState.writerValue(now - 10_000L, now - 30_000L),
                    longGone,
                    ClusterMigrationState.writerValue(
                        1L, now - ClusterMigrationState.WRITER_RETENTION_MS - 60_000L))));

    heartbeat.poll();

    OrreryUpgradeResult written = written();
    ClusterMigrationState.Writer mine =
        ClusterMigrationState.parseWriter(
            ClusterMigrationState.writerKey("gms-7f9c"),
            written.getResult().get(ClusterMigrationState.writerKey("gms-7f9c")));
    assertTrue(mine.startedAt() <= mine.lastSeen());
    assertTrue(mine.lastSeen() >= now);
    assertEquals(
        written.getResult().get(other),
        ClusterMigrationState.writerValue(now - 10_000L, now - 30_000L),
        "another writer's report is left as it is");
    assertNull(written.getResult().get(longGone), "a report older than the retention is dropped");
    assertEquals(written.getState(), OrreryUpgradeState.SUCCEEDED);
  }

  @Test
  public void testTheFirstReportCreatesTheRow() throws Exception {
    when(store.readLatest(opContext, urn)).thenReturn(null);

    heartbeat.poll();

    OrreryUpgradeResult written = written();
    assertFalse(written.getResult().get(ClusterMigrationState.writerKey("gms-7f9c")).isEmpty());
    assertEquals(written.getState(), OrreryUpgradeState.SUCCEEDED);
  }
}
