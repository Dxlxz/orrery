package com.linkedin.orrery.graphql.types.ingestion;

import static com.linkedin.orrery.graphql.TestUtils.getMockAllowContext;
import static com.linkedin.orrery.graphql.types.ingestion.TestUtils.*;
import static org.mockito.ArgumentMatchers.any;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertThrows;

import com.orrery.authentication.Authentication;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.linkedin.common.urn.Urn;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.IngestionSource;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.client.EntityClient;
import com.linkedin.ingestion.OrreryIngestionSourceConfig;
import com.linkedin.ingestion.OrreryIngestionSourceInfo;
import com.linkedin.ingestion.OrreryIngestionSourceSchedule;
import com.linkedin.metadata.Constants;
import com.linkedin.metadata.key.OrreryIngestionSourceKey;
import com.linkedin.r2.RemoteInvocationException;
import graphql.execution.DataFetcherResult;
import java.util.HashSet;
import java.util.List;
import org.mockito.Mockito;
import org.testng.annotations.Test;

public class IngestionSourceTypeTest {
  private static final String TEST_1_URN = "urn:li:orreryIngestionSource:id-1";
  private static final String TEST_2_URN = "urn:li:orreryIngestionSource:id-2";

  @Test
  public void testBatchLoad() throws Exception {
    EntityClient client = Mockito.mock(EntityClient.class);
    Urn ingestionSourceUrn1 = Urn.createFromString(TEST_1_URN);
    Urn ingestionSourceUrn2 = Urn.createFromString(TEST_2_URN);
    OrreryIngestionSourceSchedule schedule = getSchedule("UTC", "* * * * *");
    OrreryIngestionSourceConfig config = getConfig("0.8.18", "{}", "id");
    OrreryIngestionSourceInfo ingestionSourceInfo =
        getIngestionSourceInfo("Source", "mysql", schedule, config);
    EntityResponse entityResponse =
        getEntityResponse()
            .setEntityName(Constants.INGESTION_SOURCE_ENTITY_NAME)
            .setUrn(ingestionSourceUrn1);
    addAspect(
        entityResponse,
        Constants.INGESTION_SOURCE_KEY_ASPECT_NAME,
        new OrreryIngestionSourceKey().setId("id-1"));
    addAspect(entityResponse, Constants.INGESTION_INFO_ASPECT_NAME, ingestionSourceInfo);
    Mockito.when(
            client.batchGetV2(
                any(),
                Mockito.eq(Constants.INGESTION_SOURCE_ENTITY_NAME),
                Mockito.eq(
                    new HashSet<>(ImmutableSet.of(ingestionSourceUrn1, ingestionSourceUrn2))),
                Mockito.eq(IngestionSourceType.ASPECTS_TO_FETCH)))
        .thenReturn(ImmutableMap.of(ingestionSourceUrn1, entityResponse));

    IngestionSourceType type = new IngestionSourceType(client);
    QueryContext mockContext = getMockAllowContext();
    List<DataFetcherResult<IngestionSource>> result =
        type.batchLoad(ImmutableList.of(TEST_1_URN, TEST_2_URN), mockContext);

    Mockito.verify(client, Mockito.times(1))
        .batchGetV2(
            any(),
            Mockito.eq(Constants.INGESTION_SOURCE_ENTITY_NAME),
            Mockito.eq(ImmutableSet.of(ingestionSourceUrn1, ingestionSourceUrn2)),
            Mockito.eq(IngestionSourceType.ASPECTS_TO_FETCH));
    assertEquals(result.size(), 2);
    IngestionSource ingestionSource1 = result.get(0).getData();
    assertEquals(ingestionSource1.getUrn(), TEST_1_URN);
    verifyIngestionSourceInfo(ingestionSource1, ingestionSourceInfo);
    assertNull(result.get(1));
  }

  @Test
  public void testBatchLoadClientException() throws Exception {
    EntityClient mockClient = Mockito.mock(EntityClient.class);
    Mockito.doThrow(RemoteInvocationException.class)
        .when(mockClient)
        .batchGetV2(any(), Mockito.anyString(), Mockito.anySet(), Mockito.anySet());
    IngestionSourceType type = new IngestionSourceType(mockClient);

    // Execute Batch load
    QueryContext context = Mockito.mock(QueryContext.class);
    Mockito.when(context.getAuthentication()).thenReturn(Mockito.mock(Authentication.class));
    assertThrows(
        RuntimeException.class,
        () -> type.batchLoad(ImmutableList.of(TEST_1_URN, TEST_2_URN), context));
  }
}
