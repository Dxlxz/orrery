package com.linkedin.metadata.kafka.config;

import com.linkedin.gms.factory.common.IndexConventionFactory;
import com.linkedin.metadata.kafka.elasticsearch.ElasticsearchConnector;
import com.linkedin.metadata.kafka.usage.OrreryUsageEventIndexer;
import com.linkedin.metadata.kafka.usage.ElasticsearchOrreryUsageEventIndexer;
import com.linkedin.metadata.utils.elasticsearch.IndexConvention;
import javax.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UsageEventsIndexingConfiguration {

  @Bean(name = "orreryUsageEventIndexer")
  @Nonnull
  public OrreryUsageEventIndexer orreryUsageEventIndexer(
      ElasticsearchConnector elasticsearchConnector,
      @Qualifier(IndexConventionFactory.INDEX_CONVENTION_BEAN) IndexConvention indexConvention) {
    return new ElasticsearchOrreryUsageEventIndexer(elasticsearchConnector, indexConvention);
  }
}
