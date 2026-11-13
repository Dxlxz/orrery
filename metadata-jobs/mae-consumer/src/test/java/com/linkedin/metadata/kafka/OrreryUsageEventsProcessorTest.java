package com.linkedin.metadata.kafka;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import com.linkedin.metadata.kafka.transformer.OrreryUsageEventTransformer;
import com.linkedin.metadata.kafka.usage.OrreryUsageEventIndexer;
import com.linkedin.metadata.pgqueue.PgQueueStringDecode;
import com.linkedin.metadata.queue.PgQueuePayloadCompression;
import com.linkedin.metadata.queue.QueueMessageHandle;
import com.linkedin.metadata.queue.QueueReceivedMessage;
import com.linkedin.metadata.utils.metrics.MetricUtils;
import io.orreryproject.metadata.context.OperationContext;
import io.orreryproject.metadata.context.SystemTelemetryContext;
import io.orreryproject.test.metadata.context.TestOperationContexts;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class OrreryUsageEventsProcessorTest {

  private static final String TEST_TOPIC = "OrreryUsageEvent_v1";
  private static final String TEST_KEY = "test-key";
  private static final String TEST_EVENT_ID = "event-123";
  private static final long TEST_OFFSET = 12345L;
  private static final long TEST_TIMESTAMP = System.currentTimeMillis();

  @Mock private OrreryUsageEventIndexer usageEventIndexer;

  @Mock private OrreryUsageEventTransformer orreryUsageEventTransformer;

  @Mock private ConsumerRecord<String, String> mockRecord;

  private OperationContext systemOperationContext;

  @Mock private MetricUtils metricUtils;

  private OrreryUsageEventsProcessor processor;

  @BeforeMethod
  public void setUp() {
    MockitoAnnotations.openMocks(this);

    when(metricUtils.getRegistry()).thenReturn(new SimpleMeterRegistry());

    systemOperationContext =
        TestOperationContexts.Builder.builder()
            .systemTelemetryContextSupplier(
                () ->
                    SystemTelemetryContext.builder()
                        .tracer(SystemTelemetryContext.TEST.getTracer())
                        .metricUtils(metricUtils)
                        .build())
            .buildSystemContext();

    processor =
        new OrreryUsageEventsProcessor(
            usageEventIndexer, orreryUsageEventTransformer, systemOperationContext);
    setConsumerGroupId(processor, "orrery-usage-event-consumer-job-client");
  }

  @Test
  public void testConsumeSuccessfulEvent() {
    String eventJson = "{\"type\":\"PageViewEvent\",\"timestamp\":1234567890}";
    String transformedDocument = "{\"transformed\":\"data\"}";

    stubRecord(mockRecord, TEST_KEY, TEST_TOPIC, 0, TEST_OFFSET, TEST_TIMESTAMP, 100, eventJson);

    OrreryUsageEventTransformer.TransformedDocument transformedDoc =
        new OrreryUsageEventTransformer.TransformedDocument(TEST_EVENT_ID, transformedDocument);

    when(orreryUsageEventTransformer.transformOrreryUsageEvent(
            any(OperationContext.class), eq(eventJson)))
        .thenReturn(Optional.of(transformedDoc));

    processor.consume(Collections.singletonList(mockRecord));

    verify(metricUtils).histogram(eq(OrreryUsageEventsProcessor.class), eq("kafkaLag"), anyLong());

    ArgumentCaptor<List<OrreryUsageEventIndexer.IndexableUsageEvent>> captor = listCaptor();
    verify(usageEventIndexer).indexBatch(any(OperationContext.class), captor.capture());
    assertEquals(captor.getValue().size(), 1);
    assertEquals(captor.getValue().get(0).document(), transformedDoc);
    assertEquals(captor.getValue().get(0).documentIdWithKafkaOffsetSuffix(), "event-123_12345");
  }

  @Test
  public void testConsumeWithFailedTransformation() {
    String eventJson = "{\"invalid\":\"event\"}";

    stubRecord(mockRecord, TEST_KEY, TEST_TOPIC, 0, TEST_OFFSET, TEST_TIMESTAMP, 50, eventJson);

    when(orreryUsageEventTransformer.transformOrreryUsageEvent(
            any(OperationContext.class), eq(eventJson)))
        .thenReturn(Optional.empty());

    processor.consume(Collections.singletonList(mockRecord));

    verify(metricUtils).histogram(eq(OrreryUsageEventsProcessor.class), eq("kafkaLag"), anyLong());
    // Still invoked even when nothing transformed: the indexer must observe an empty batch.
    ArgumentCaptor<List<OrreryUsageEventIndexer.IndexableUsageEvent>> captor = listCaptor();
    verify(usageEventIndexer).indexBatch(any(OperationContext.class), captor.capture());
    assertTrue(captor.getValue().isEmpty());
  }

  @Test
  public void testConsumeWithNoMetrics() {
    systemOperationContext =
        TestOperationContexts.Builder.builder()
            .systemTelemetryContextSupplier(
                () ->
                    systemOperationContext.getSystemTelemetryContext().toBuilder()
                        .metricUtils(null)
                        .build())
            .buildSystemContext();

    processor =
        new OrreryUsageEventsProcessor(
            usageEventIndexer, orreryUsageEventTransformer, systemOperationContext);

    String eventJson = "{\"type\":\"SearchEvent\"}";
    String transformedDocument = "{\"search\":\"data\"}";

    stubRecord(mockRecord, TEST_KEY, TEST_TOPIC, 0, TEST_OFFSET, TEST_TIMESTAMP, 75, eventJson);

    OrreryUsageEventTransformer.TransformedDocument transformedDoc =
        new OrreryUsageEventTransformer.TransformedDocument(TEST_EVENT_ID, transformedDocument);

    when(orreryUsageEventTransformer.transformOrreryUsageEvent(
            any(OperationContext.class), eq(eventJson)))
        .thenReturn(Optional.of(transformedDoc));

    processor.consume(Collections.singletonList(mockRecord));

    verify(metricUtils, never()).histogram(any(), any(), anyLong());
    verify(usageEventIndexer).indexBatch(any(OperationContext.class), any());
  }

  @Test
  public void testDocumentIdGenerationWithLargeOffset() {
    String eventJson = "{\"type\":\"Event\"}";
    String transformedDocument = "{\"data\":\"value\"}";
    long largeOffset = 9876543210L;

    stubRecord(mockRecord, TEST_KEY, TEST_TOPIC, 0, largeOffset, TEST_TIMESTAMP, 50, eventJson);

    OrreryUsageEventTransformer.TransformedDocument transformedDoc =
        new OrreryUsageEventTransformer.TransformedDocument(TEST_EVENT_ID, transformedDocument);

    when(orreryUsageEventTransformer.transformOrreryUsageEvent(
            any(OperationContext.class), eq(eventJson)))
        .thenReturn(Optional.of(transformedDoc));

    processor.consume(Collections.singletonList(mockRecord));

    verify(usageEventIndexer)
        .indexBatch(
            any(OperationContext.class),
            argThat(
                events ->
                    events.size() == 1
                        && events
                            .get(0)
                            .documentIdWithKafkaOffsetSuffix()
                            .equals("event-123_43210")));
  }

  @Test
  public void testDocumentIdGenerationWithSmallOffset() {
    String eventJson = "{\"type\":\"Event\"}";
    String transformedDocument = "{\"data\":\"value\"}";
    long smallOffset = 42L;

    stubRecord(mockRecord, TEST_KEY, TEST_TOPIC, 0, smallOffset, TEST_TIMESTAMP, 50, eventJson);

    OrreryUsageEventTransformer.TransformedDocument transformedDoc =
        new OrreryUsageEventTransformer.TransformedDocument(TEST_EVENT_ID, transformedDocument);

    when(orreryUsageEventTransformer.transformOrreryUsageEvent(
            any(OperationContext.class), eq(eventJson)))
        .thenReturn(Optional.of(transformedDoc));

    processor.consume(Collections.singletonList(mockRecord));

    verify(usageEventIndexer)
        .indexBatch(
            any(OperationContext.class),
            argThat(
                events ->
                    events.size() == 1
                        && events
                            .get(0)
                            .documentIdWithKafkaOffsetSuffix()
                            .equals("event-123_00042")));
  }

  @Test
  public void testDocumentIdGenerationWithSpecialCharacters() {
    String eventJson = "{\"type\":\"Event\"}";
    String transformedDocument = "{\"data\":\"value\"}";
    String eventIdWithSpecialChars = "event/123+456";

    stubRecord(mockRecord, TEST_KEY, TEST_TOPIC, 0, TEST_OFFSET, TEST_TIMESTAMP, 50, eventJson);

    OrreryUsageEventTransformer.TransformedDocument transformedDoc =
        new OrreryUsageEventTransformer.TransformedDocument(
            eventIdWithSpecialChars, transformedDocument);

    when(orreryUsageEventTransformer.transformOrreryUsageEvent(
            any(OperationContext.class), eq(eventJson)))
        .thenReturn(Optional.of(transformedDoc));

    processor.consume(Collections.singletonList(mockRecord));

    verify(usageEventIndexer)
        .indexBatch(
            any(OperationContext.class),
            argThat(
                events ->
                    events.size() == 1
                        && events
                            .get(0)
                            .documentIdWithKafkaOffsetSuffix()
                            .equals("event%2F123%2B456_12345")));
  }

  @Test
  public void testConsumeBatchOfMultipleEvents() {
    ConsumerRecord<String, String> r1 = mock(ConsumerRecord.class);
    ConsumerRecord<String, String> r2 = mock(ConsumerRecord.class);
    ConsumerRecord<String, String> r3 = mock(ConsumerRecord.class);
    stubRecord(r1, "k1", TEST_TOPIC, 0, 100L, TEST_TIMESTAMP, 50, "{\"a\":1}");
    stubRecord(r2, "k2", TEST_TOPIC, 0, 101L, TEST_TIMESTAMP, 50, "{\"a\":2}");
    stubRecord(r3, "k3", TEST_TOPIC, 0, 102L, TEST_TIMESTAMP, 50, "{\"a\":3}");

    when(orreryUsageEventTransformer.transformOrreryUsageEvent(
            any(OperationContext.class), any(String.class)))
        .thenAnswer(
            inv -> {
              String json = inv.getArgument(1);
              return Optional.of(
                  new OrreryUsageEventTransformer.TransformedDocument(
                      "id-" + json.charAt(json.length() - 2), json));
            });

    processor.consume(List.of(r1, r2, r3));

    ArgumentCaptor<List<OrreryUsageEventIndexer.IndexableUsageEvent>> captor = listCaptor();
    verify(usageEventIndexer).indexBatch(any(OperationContext.class), captor.capture());
    assertEquals(captor.getValue().size(), 3);
  }

  @Test
  public void testConsumeWithNullRecordValue() {
    stubRecord(mockRecord, TEST_KEY, TEST_TOPIC, 0, TEST_OFFSET, TEST_TIMESTAMP, 0, null);

    when(orreryUsageEventTransformer.transformOrreryUsageEvent(
            any(OperationContext.class), eq(null)))
        .thenReturn(Optional.empty());

    processor.consume(Collections.singletonList(mockRecord));

    ArgumentCaptor<List<OrreryUsageEventIndexer.IndexableUsageEvent>> captor = listCaptor();
    verify(usageEventIndexer).indexBatch(any(OperationContext.class), captor.capture());
    assertTrue(captor.getValue().isEmpty());
  }

  @Test
  public void testKafkaLagCalculation() {
    String eventJson = "{\"type\":\"Event\"}";
    long recordTimestamp = System.currentTimeMillis() - 5000;

    stubRecord(mockRecord, TEST_KEY, TEST_TOPIC, 0, TEST_OFFSET, recordTimestamp, 50, eventJson);

    when(orreryUsageEventTransformer.transformOrreryUsageEvent(
            any(OperationContext.class), eq(eventJson)))
        .thenReturn(Optional.empty());

    processor.consume(Collections.singletonList(mockRecord));

    ArgumentCaptor<Long> lagCaptor = ArgumentCaptor.forClass(Long.class);
    verify(metricUtils)
        .histogram(eq(OrreryUsageEventsProcessor.class), eq("kafkaLag"), lagCaptor.capture());

    assertTrue(lagCaptor.getValue() >= 5000L);
  }

  @Test
  public void testMicrometerKafkaQueueTimeMetric() {
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    when(metricUtils.getRegistry()).thenReturn(meterRegistry);

    String eventJson = "{\"type\":\"PageViewEvent\",\"timestamp\":1234567890}";
    String transformedDocument = "{\"transformed\":\"data\"}";

    long messageTimestamp = System.currentTimeMillis() - 3000;
    when(mockRecord.timestamp()).thenReturn(messageTimestamp);
    when(mockRecord.topic()).thenReturn("OrreryUsageEvent_v1");
    when(mockRecord.value()).thenReturn(eventJson);

    OrreryUsageEventTransformer.TransformedDocument transformedDoc =
        new OrreryUsageEventTransformer.TransformedDocument(TEST_EVENT_ID, transformedDocument);

    when(orreryUsageEventTransformer.transformOrreryUsageEvent(
            any(OperationContext.class), eq(eventJson)))
        .thenReturn(Optional.of(transformedDoc));

    processor.consume(Collections.singletonList(mockRecord));

    Timer timer =
        meterRegistry.timer(
            MetricUtils.MESSAGING_QUEUE_TIME,
            MetricUtils.MESSAGING_SYSTEM,
            MetricUtils.MESSAGING_SYSTEM_KAFKA,
            MetricUtils.MESSAGING_TOPIC,
            "OrreryUsageEvent_v1",
            MetricUtils.MESSAGING_CONSUMER_GROUP,
            "orrery-usage-event-consumer-job-client");

    assertNotNull(timer);
    assertEquals(timer.count(), 1);
    assertTrue(timer.totalTime(TimeUnit.MILLISECONDS) >= 2500);
    assertTrue(timer.totalTime(TimeUnit.MILLISECONDS) <= 3500);

    verify(metricUtils).histogram(eq(OrreryUsageEventsProcessor.class), eq("kafkaLag"), anyLong());
    verify(usageEventIndexer).indexBatch(any(OperationContext.class), any());
  }

  @Test
  public void testMicrometerKafkaQueueTimeWithDifferentTopics() {
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    when(metricUtils.getRegistry()).thenReturn(meterRegistry);

    String eventJson = "{\"type\":\"SearchEvent\"}";
    String transformedDocument = "{\"search\":\"data\"}";

    OrreryUsageEventTransformer.TransformedDocument transformedDoc =
        new OrreryUsageEventTransformer.TransformedDocument(TEST_EVENT_ID, transformedDocument);

    when(orreryUsageEventTransformer.transformOrreryUsageEvent(
            any(OperationContext.class), eq(eventJson)))
        .thenReturn(Optional.of(transformedDoc));

    long now = System.currentTimeMillis();
    when(mockRecord.timestamp()).thenReturn(now - 2000);
    when(mockRecord.topic()).thenReturn("OrreryUsageEvent_v1");
    when(mockRecord.value()).thenReturn(eventJson);
    processor.consume(Collections.singletonList(mockRecord));

    ConsumerRecord<String, String> mockRecord2 = mock(ConsumerRecord.class);
    when(mockRecord2.key()).thenReturn("test-key-2");
    when(mockRecord2.topic()).thenReturn("OrreryUsageEvent_v2");
    when(mockRecord2.partition()).thenReturn(0);
    when(mockRecord2.offset()).thenReturn(123L);
    when(mockRecord2.timestamp()).thenReturn(now - 5000);
    when(mockRecord2.serializedValueSize()).thenReturn(100);
    when(mockRecord2.value()).thenReturn(eventJson);

    processor.consume(Collections.singletonList(mockRecord2));

    Timer timer1 =
        meterRegistry.timer(
            MetricUtils.MESSAGING_QUEUE_TIME,
            MetricUtils.MESSAGING_SYSTEM,
            MetricUtils.MESSAGING_SYSTEM_KAFKA,
            MetricUtils.MESSAGING_TOPIC,
            "OrreryUsageEvent_v1",
            MetricUtils.MESSAGING_CONSUMER_GROUP,
            "orrery-usage-event-consumer-job-client");

    Timer timer2 =
        meterRegistry.timer(
            MetricUtils.MESSAGING_QUEUE_TIME,
            MetricUtils.MESSAGING_SYSTEM,
            MetricUtils.MESSAGING_SYSTEM_KAFKA,
            MetricUtils.MESSAGING_TOPIC,
            "OrreryUsageEvent_v2",
            MetricUtils.MESSAGING_CONSUMER_GROUP,
            "orrery-usage-event-consumer-job-client");

    assertEquals(timer1.count(), 1);
    assertEquals(timer2.count(), 1);

    assertTrue(timer1.totalTime(TimeUnit.MILLISECONDS) >= 1500);
    assertTrue(timer1.totalTime(TimeUnit.MILLISECONDS) <= 2500);

    assertTrue(timer2.totalTime(TimeUnit.MILLISECONDS) >= 4500);
    assertTrue(timer2.totalTime(TimeUnit.MILLISECONDS) <= 5500);
  }

  @Test
  public void testMicrometerMetricsWithFailedTransformation() {
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    when(metricUtils.getRegistry()).thenReturn(meterRegistry);

    String eventJson = "{\"invalid\":\"event\"}";
    long messageTimestamp = System.currentTimeMillis() - 4000;
    when(mockRecord.timestamp()).thenReturn(messageTimestamp);
    when(mockRecord.topic()).thenReturn("OrreryUsageEvent_v1");
    when(mockRecord.value()).thenReturn(eventJson);

    when(orreryUsageEventTransformer.transformOrreryUsageEvent(
            any(OperationContext.class), eq(eventJson)))
        .thenReturn(Optional.empty());

    processor.consume(Collections.singletonList(mockRecord));

    Timer timer =
        meterRegistry.timer(
            MetricUtils.MESSAGING_QUEUE_TIME,
            MetricUtils.MESSAGING_SYSTEM,
            MetricUtils.MESSAGING_SYSTEM_KAFKA,
            MetricUtils.MESSAGING_TOPIC,
            "OrreryUsageEvent_v1",
            MetricUtils.MESSAGING_CONSUMER_GROUP,
            "orrery-usage-event-consumer-job-client");

    assertEquals(timer.count(), 1);
    assertTrue(timer.totalTime(TimeUnit.MILLISECONDS) >= 3500);
    assertTrue(timer.totalTime(TimeUnit.MILLISECONDS) <= 4500);

    // Indexer is still invoked once per Kafka batch with the (empty) event list.
    verify(usageEventIndexer).indexBatch(any(OperationContext.class), argThat(List::isEmpty));
  }

  @Test
  public void testMicrometerKafkaQueueTimeAccuracy() {
    SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    when(metricUtils.getRegistry()).thenReturn(meterRegistry);

    String eventJson = "{\"type\":\"Event\"}";
    String transformedDocument = "{\"data\":\"value\"}";

    OrreryUsageEventTransformer.TransformedDocument transformedDoc =
        new OrreryUsageEventTransformer.TransformedDocument(TEST_EVENT_ID, transformedDocument);

    when(orreryUsageEventTransformer.transformOrreryUsageEvent(
            any(OperationContext.class), eq(eventJson)))
        .thenReturn(Optional.of(transformedDoc));

    long[] queueTimes = {100, 500, 1000, 2000, 5000};

    for (int i = 0; i < queueTimes.length; i++) {
      ConsumerRecord<String, String> testRecord = mock(ConsumerRecord.class);
      when(testRecord.key()).thenReturn("test-key-" + i);
      when(testRecord.topic()).thenReturn("OrreryUsageEvent_v1");
      when(testRecord.partition()).thenReturn(0);
      when(testRecord.offset()).thenReturn((long) (TEST_OFFSET + i));
      when(testRecord.timestamp()).thenReturn(System.currentTimeMillis() - queueTimes[i]);
      when(testRecord.serializedValueSize()).thenReturn(100);
      when(testRecord.value()).thenReturn(eventJson);

      processor.consume(Collections.singletonList(testRecord));
    }

    Timer timer =
        meterRegistry.timer(
            MetricUtils.MESSAGING_QUEUE_TIME,
            MetricUtils.MESSAGING_SYSTEM,
            MetricUtils.MESSAGING_SYSTEM_KAFKA,
            MetricUtils.MESSAGING_TOPIC,
            "OrreryUsageEvent_v1",
            MetricUtils.MESSAGING_CONSUMER_GROUP,
            "orrery-usage-event-consumer-job-client");

    assertEquals(timer.count(), queueTimes.length);

    double mean = timer.mean(TimeUnit.MILLISECONDS);
    assertTrue(mean >= 1500);
    assertTrue(mean <= 2000);

    assertTrue(timer.max(TimeUnit.MILLISECONDS) >= 4500);
    assertTrue(timer.max(TimeUnit.MILLISECONDS) <= 5500);

    verify(metricUtils, times(queueTimes.length))
        .histogram(eq(OrreryUsageEventsProcessor.class), eq("kafkaLag"), anyLong());
  }

  @Test
  public void testConsumePgQueueSuccessfulEvent() {
    String eventJson = "{\"type\":\"PageViewEvent\",\"timestamp\":1234567890}";
    String transformedDocument = "{\"transformed\":\"data\"}";
    long enqueueSeq = 42L;

    QueueReceivedMessage msg = pgQueueMessage(eventJson, enqueueSeq);

    OrreryUsageEventTransformer.TransformedDocument transformedDoc =
        new OrreryUsageEventTransformer.TransformedDocument(TEST_EVENT_ID, transformedDocument);

    try (MockedStatic<PgQueueStringDecode> mockedDecode =
        Mockito.mockStatic(PgQueueStringDecode.class)) {
      mockedDecode.when(() -> PgQueueStringDecode.decodeAsUtf8(msg)).thenReturn(eventJson);

      when(orreryUsageEventTransformer.transformOrreryUsageEvent(
              any(OperationContext.class), eq(eventJson)))
          .thenReturn(Optional.of(transformedDoc));

      processor.consume(TEST_TOPIC, Collections.singletonList(msg));

      verify(metricUtils)
          .histogram(eq(OrreryUsageEventsProcessor.class), eq("kafkaLag"), anyLong());

      ArgumentCaptor<List<OrreryUsageEventIndexer.IndexableUsageEvent>> captor = listCaptor();
      verify(usageEventIndexer).indexBatch(any(OperationContext.class), captor.capture());
      assertEquals(captor.getValue().size(), 1);
      assertEquals(captor.getValue().get(0).document(), transformedDoc);
      assertEquals(captor.getValue().get(0).documentIdWithKafkaOffsetSuffix(), "event-123_00042");
    }
  }

  @Test
  public void testConsumePgQueueWithFailedTransformation() {
    String eventJson = "{\"invalid\":\"event\"}";

    QueueReceivedMessage msg = pgQueueMessage(eventJson, 42L);

    try (MockedStatic<PgQueueStringDecode> mockedDecode =
        Mockito.mockStatic(PgQueueStringDecode.class)) {
      mockedDecode.when(() -> PgQueueStringDecode.decodeAsUtf8(msg)).thenReturn(eventJson);

      when(orreryUsageEventTransformer.transformOrreryUsageEvent(
              any(OperationContext.class), eq(eventJson)))
          .thenReturn(Optional.empty());

      processor.consume(TEST_TOPIC, Collections.singletonList(msg));

      ArgumentCaptor<List<OrreryUsageEventIndexer.IndexableUsageEvent>> captor = listCaptor();
      verify(usageEventIndexer).indexBatch(any(OperationContext.class), captor.capture());
      assertTrue(captor.getValue().isEmpty());
    }
  }

  @Test
  public void testConsumePgQueueBatchOfMultipleEvents() {
    QueueReceivedMessage m1 = pgQueueMessage("{\"a\":1}", 100L);
    QueueReceivedMessage m2 = pgQueueMessage("{\"a\":2}", 101L);
    QueueReceivedMessage m3 = pgQueueMessage("{\"a\":3}", 102L);

    try (MockedStatic<PgQueueStringDecode> mockedDecode =
        Mockito.mockStatic(PgQueueStringDecode.class)) {
      mockedDecode
          .when(() -> PgQueueStringDecode.decodeAsUtf8(any(QueueReceivedMessage.class)))
          .thenAnswer(
              inv -> {
                QueueReceivedMessage m = inv.getArgument(0);
                return new String(m.payload(), StandardCharsets.UTF_8);
              });

      when(orreryUsageEventTransformer.transformOrreryUsageEvent(
              any(OperationContext.class), any(String.class)))
          .thenAnswer(
              inv -> {
                String json = inv.getArgument(1);
                return Optional.of(
                    new OrreryUsageEventTransformer.TransformedDocument(
                        "id-" + json.charAt(json.length() - 2), json));
              });

      processor.consume(TEST_TOPIC, List.of(m1, m2, m3));

      ArgumentCaptor<List<OrreryUsageEventIndexer.IndexableUsageEvent>> captor = listCaptor();
      verify(usageEventIndexer).indexBatch(any(OperationContext.class), captor.capture());
      assertEquals(captor.getValue().size(), 3);
    }
  }

  private static QueueReceivedMessage pgQueueMessage(String payloadStr, long enqueueSeq) {
    byte[] payload = payloadStr.getBytes(StandardCharsets.UTF_8);
    QueueMessageHandle handle = new QueueMessageHandle(1L, Instant.now(), 1L, 0, enqueueSeq);
    return new QueueReceivedMessage(
        handle,
        0,
        payload,
        Optional.empty(),
        PgQueuePayloadCompression.NONE,
        Collections.emptyList(),
        "test-key",
        "test-lock-owner");
  }

  private static void stubRecord(
      ConsumerRecord<String, String> record,
      String key,
      String topic,
      int partition,
      long offset,
      long timestamp,
      int valueSize,
      String value) {
    when(record.key()).thenReturn(key);
    when(record.topic()).thenReturn(topic);
    when(record.partition()).thenReturn(partition);
    when(record.offset()).thenReturn(offset);
    when(record.timestamp()).thenReturn(timestamp);
    when(record.serializedValueSize()).thenReturn(valueSize);
    when(record.value()).thenReturn(value);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static ArgumentCaptor<List<OrreryUsageEventIndexer.IndexableUsageEvent>> listCaptor() {
    return (ArgumentCaptor) ArgumentCaptor.forClass(List.class);
  }

  private void setConsumerGroupId(OrreryUsageEventsProcessor processor, String consumerGroupId) {
    try {
      java.lang.reflect.Field field =
          OrreryUsageEventsProcessor.class.getDeclaredField("orreryUsageEventConsumerGroupId");
      field.setAccessible(true);
      field.set(processor, consumerGroupId);
    } catch (Exception e) {
      throw new RuntimeException("Failed to set orreryUsageEventConsumerGroupId field", e);
    }
  }
}
