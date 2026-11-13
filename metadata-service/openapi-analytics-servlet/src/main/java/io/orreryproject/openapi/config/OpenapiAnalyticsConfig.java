package io.orreryproject.openapi.config;

import io.orreryproject.openapi.delegates.OrreryUsageEventsImpl;
import io.orreryproject.openapi.v2.generated.controller.OrreryUsageEventsApiDelegate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenapiAnalyticsConfig {
  @Bean
  public OrreryUsageEventsApiDelegate orreryUsageEventsApiDelegate() {
    return new OrreryUsageEventsImpl();
  }
}
