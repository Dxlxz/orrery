package com.linkedin.gms.factory.kafka;

import com.linkedin.gms.factory.config.ConfigurationProvider;
import com.linkedin.metadata.config.messaging.KafkaMessagingEnabledCondition;
import com.linkedin.metadata.dao.producer.GenericProducerImpl;
import com.linkedin.metadata.dao.producer.KafkaHealthChecker;
import com.linkedin.metadata.dao.producer.KafkaUsageEventPublisher;
import com.linkedin.metadata.dao.producer.context.outbound.OutboundContextResolver;
import com.linkedin.metadata.event.GenericProducer;
import com.linkedin.metadata.event.UsageEventPublisher;
import com.linkedin.metadata.utils.metrics.MetricUtils;
import org.apache.kafka.clients.producer.Producer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

@Configuration
@Conditional(KafkaMessagingEnabledCondition.class)
public class UsageEventPublisherFactory {

  @Autowired private KafkaHealthChecker kafkaHealthChecker;

  @Bean(name = "orreryUsageGenericProducer")
  protected GenericProducer<String> orreryUsageGenericProducer(
      @Qualifier("orreryUsageProducer") Producer<String, String> usageProducer,
      MetricUtils metricUtils,
      ConfigurationProvider configurationProvider) {
    GenericProducerImpl<String> producer =
        new GenericProducerImpl<>(usageProducer, kafkaHealthChecker, metricUtils, "USAGE");
    if (configurationProvider.getOrrery().isReadOnly()) {
      producer.setWritable(false);
    }
    return producer;
  }

  /**
   * Legacy bean name retained for injection sites that still qualify {@code
   * orreryUsageEventProducer}.
   */
  @Bean(name = {"usageEventPublisher", "orreryUsageEventProducer"})
  protected UsageEventPublisher usageEventPublisher(
      @Qualifier("orreryUsageProducer") Producer<String, String> usageProducer,
      MetricUtils metricUtils,
      ConfigurationProvider configurationProvider,
      OutboundContextResolver outboundContextResolver) {
    KafkaUsageEventPublisher publisher =
        new KafkaUsageEventPublisher(
            usageProducer, kafkaHealthChecker, metricUtils, outboundContextResolver);
    if (configurationProvider.getOrrery().isReadOnly()) {
      publisher.setWritable(false);
    }
    return publisher;
  }
}
