package com.linkedin.metadata.kafka;

import com.linkedin.gms.factory.config.ConfigurationProvider;
import com.linkedin.gms.factory.system_telemetry.OpenTelemetryBaseFactory;
import com.linkedin.metadata.event.GenericProducer;
import com.linkedin.metadata.utils.metrics.MetricUtils;
import io.orreryproject.metadata.context.SystemTelemetryContext;
import io.orreryproject.metadata.context.kafka.SpanProducerRecordResolver;
import io.orreryproject.metadata.context.telemetry.EnrichingSpanProcessor;
import javax.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MCEOpenTelemetryConfig extends OpenTelemetryBaseFactory {

  @Override
  protected String getApplicationComponent() {
    return "orrery-mce-consumer";
  }

  @Bean
  @Override
  protected SystemTelemetryContext traceContext(
      MetricUtils metricUtils,
      ConfigurationProvider configurationProvider,
      @Autowired(required = false) @Qualifier("orreryUsageGenericProducer") @Nullable
          GenericProducer<String> usageProducer,
      SpanProducerRecordResolver spanProducerRecordResolver,
      EnrichingSpanProcessor enrichingSpanProcessor) {
    return super.traceContext(
        metricUtils,
        configurationProvider,
        usageProducer,
        spanProducerRecordResolver,
        enrichingSpanProcessor);
  }
}
