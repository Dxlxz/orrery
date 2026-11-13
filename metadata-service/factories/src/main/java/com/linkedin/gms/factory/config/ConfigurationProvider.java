package com.linkedin.gms.factory.config;

import com.orrery.authentication.AuthenticationConfiguration;
import com.orrery.authorization.AuthorizationConfiguration;
import com.linkedin.metadata.config.OrreryAppConfiguration;
import com.linkedin.metadata.config.GMSConfiguration;
import com.linkedin.metadata.config.PlatformAnalyticsConfiguration;
import com.linkedin.metadata.config.postgres.PostgresSqlSetupProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@EqualsAndHashCode(callSuper = true)
@Component
@EnableConfigurationProperties({KafkaProperties.class, PostgresSqlSetupProperties.class})
@ConfigurationProperties
@Data
public class ConfigurationProvider extends OrreryAppConfiguration {
  /** Authentication related configs */
  private AuthenticationConfiguration authentication;

  /** Authorizer related configs */
  private AuthorizationConfiguration authorization;

  /** Configuration for the health check server */
  private HealthCheckConfiguration healthCheck;

  /** Enable/disable Orrery analytics */
  private PlatformAnalyticsConfiguration platformAnalytics;

  /**
   * Provides GMSConfiguration bean for dependency injection. This extracts the GMS configuration
   * from the main Orrery configuration.
   */
  @Bean("gmsConfiguration")
  public GMSConfiguration gmsConfiguration() {
    return this.getOrrery().getGms();
  }
}
