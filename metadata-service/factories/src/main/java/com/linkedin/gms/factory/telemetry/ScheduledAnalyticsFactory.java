package com.linkedin.gms.factory.telemetry;

import static com.linkedin.gms.factory.telemetry.TelemetryUtils.ORRERY_ENGAGE_ENDPOINT;
import static com.linkedin.gms.factory.telemetry.TelemetryUtils.ORRERY_EVENTS_ENDPOINT;

import com.orrery.telemetry.MixpanelEventSender;
import com.linkedin.gms.factory.config.ConfigurationProvider;
import com.linkedin.metadata.entity.EntityService;
import com.linkedin.metadata.utils.metrics.MetricUtils;
import com.linkedin.metadata.version.GitVersion;
import com.mixpanel.mixpanelapi.MixpanelAPI;
import io.orreryproject.metadata.context.OperationContext;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Slf4j
@Configuration
@EnableScheduling
public class ScheduledAnalyticsFactory {

  @Bean
  @ConditionalOnProperty("telemetry.enabledServer")
  public DailyReport dailyReport(
      @Qualifier("systemOperationContext") OperationContext systemOperationContext,
      ConfigurationProvider configurationProvider,
      EntityService<?> entityService,
      GitVersion gitVersion,
      @Nullable MetricUtils metricUtils) {
    // The daily report always goes to Orrery's own collector with Orrery's token, on a client
    // of its own rather than the one TrackingService uses. DailyReport.close() stops the sender
    // when the bean is destroyed.
    MixpanelEventSender sender =
        new MixpanelEventSender(
            "daily-report",
            new MixpanelAPI(ORRERY_EVENTS_ENDPOINT, ORRERY_ENGAGE_ENDPOINT),
            metricUtils);
    return new DailyReport(
        systemOperationContext, configurationProvider, entityService, gitVersion, sender);
  }
}
