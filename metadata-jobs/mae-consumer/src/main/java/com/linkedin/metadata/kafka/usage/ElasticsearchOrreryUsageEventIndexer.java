package com.linkedin.metadata.kafka.usage;

import com.linkedin.events.metadata.ChangeType;
import com.linkedin.metadata.kafka.elasticsearch.ElasticsearchConnector;
import com.linkedin.metadata.kafka.elasticsearch.JsonElasticEvent;
import com.linkedin.metadata.utils.elasticsearch.IndexConvention;
import io.orreryproject.metadata.context.OperationContext;
import java.util.List;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class ElasticsearchOrreryUsageEventIndexer implements OrreryUsageEventIndexer {

  private final ElasticsearchConnector elasticSearchConnector;
  private final IndexConvention indexConvention;

  /**
   * Forward each event in the batch to the {@link ElasticsearchConnector}, which already coalesces
   * them via {@code BulkProcessor}; the indexer itself does not need to manage a separate batch.
   */
  @Override
  public void indexBatch(
      @Nonnull OperationContext opContext, @Nonnull List<IndexableUsageEvent> events) {
    String indexName = indexConvention.getIndexName(opContext, "orrery_usage_event");
    for (IndexableUsageEvent event : events) {
      JsonElasticEvent elasticEvent = new JsonElasticEvent(event.document().getDocument());
      elasticEvent.setId(event.documentIdWithKafkaOffsetSuffix());
      elasticEvent.setIndex(indexName);
      elasticEvent.setActionType(ChangeType.CREATE);
      elasticSearchConnector.feedElasticEvent(opContext, elasticEvent);
    }
  }
}
