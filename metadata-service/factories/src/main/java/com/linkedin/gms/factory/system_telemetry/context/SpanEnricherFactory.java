package com.linkedin.gms.factory.system_telemetry.context;

import io.orreryproject.metadata.context.kafka.SpanProducerRecordEnricher;
import io.orreryproject.metadata.context.kafka.SpanProducerRecordResolver;
import io.orreryproject.metadata.context.telemetry.EnrichingSpanProcessor;
import io.orreryproject.metadata.context.telemetry.SpanContextEnricher;
import java.util.List;
import javax.annotation.Nonnull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SpanEnricherFactory {
  @Bean
  @Nonnull
  public SpanProducerRecordResolver spanProducerRecordResolver(
      @Nonnull List<SpanProducerRecordEnricher> enrichers) {
    return new SpanProducerRecordResolver(enrichers);
  }

  @Bean
  @Nonnull
  public EnrichingSpanProcessor enrichingSpanProcessor(
      @Nonnull List<SpanContextEnricher> enrichers) {
    return new EnrichingSpanProcessor(enrichers);
  }
}
