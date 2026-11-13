package com.linkedin.metadata.kafka.usage;

import com.linkedin.metadata.kafka.transformer.OrreryUsageEventTransformer;
import io.orreryproject.metadata.context.OperationContext;
import java.util.List;
import javax.annotation.Nonnull;

/**
 * Persists transformed Orrery usage events (Kafka pipeline) either to Elasticsearch/OpenSearch or
 * to PostgreSQL depending on deployment configuration.
 *
 * <p>The Kafka listener delivers a batch of records and invokes {@link #indexBatch(List)} once per
 * Kafka poll. Storage-aware impls are expected to commit the batch atomically where the storage
 * supports it (Postgres does a single transactional {@code INSERT}; Elasticsearch coalesces via
 * {@code BulkProcessor}).
 */
public interface OrreryUsageEventIndexer {

  /**
   * Index every event in the supplied batch. Empty batches must be a no-op. Implementations should
   * treat the batch as a single transaction where the underlying storage allows it.
   */
  void indexBatch(@Nonnull OperationContext opContext, @Nonnull List<IndexableUsageEvent> events);

  /**
   * Optional flush hook for impls that buffer beyond a single {@link #indexBatch(List)} call.
   * Default is a no-op.
   */
  default void flush() {}

  /** A transformed usage-event document together with its Kafka-offset-suffixed document id. */
  record IndexableUsageEvent(
      @Nonnull OrreryUsageEventTransformer.TransformedDocument document,
      @Nonnull String documentIdWithKafkaOffsetSuffix) {}
}
