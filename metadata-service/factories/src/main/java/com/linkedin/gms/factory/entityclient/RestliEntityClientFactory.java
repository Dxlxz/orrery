package com.linkedin.gms.factory.entityclient;

import com.linkedin.common.client.restli.RestliRequestContextResolver;
import com.linkedin.entity.client.EntityClient;
import com.linkedin.entity.client.EntityClientConfig;
import com.linkedin.entity.client.RestliEntityClient;
import com.linkedin.entity.client.SystemEntityClient;
import com.linkedin.entity.client.SystemRestliEntityClient;
import com.linkedin.gms.factory.restli.RestliRequestContextResolverFactory;
import com.linkedin.metadata.config.cache.client.EntityClientCacheConfig;
import com.linkedin.metadata.restli.DefaultRestliClientFactory;
import com.linkedin.metadata.restli.RestliClientSslConfig;
import com.linkedin.metadata.utils.BasePathUtils;
import com.linkedin.metadata.utils.metrics.MetricUtils;
import com.linkedin.restli.client.Client;
import java.net.URI;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/** The Java Entity Client should be preferred if executing within the GMS service. */
@Configuration
@Import(RestliRequestContextResolverFactory.class)
@ConditionalOnProperty(name = "entityClient.impl", havingValue = "restli")
public class RestliEntityClientFactory {

  @Value("${orrery.gms.truststore.path:#{null}}")
  private String truststorePath;

  @Value("${orrery.gms.truststore.password:#{null}}")
  private String truststorePassword;

  @Value("${orrery.gms.truststore.type:#{null}}")
  private String truststoreType;

  @Value("${orrery.gms.keystore.path:#{null}}")
  private String keystorePath;

  @Value("${orrery.gms.keystore.password:#{null}}")
  private String keystorePassword;

  @Value("${orrery.gms.keystore.type:#{null}}")
  private String keystoreType;

  @Value("${orrery.gms.keystore.keyPassword:#{null}}")
  private String keyPassword;

  @Bean("entityClient")
  public EntityClient entityClient(
      @Value("${orrery.gms.host}") String gmsHost,
      @Value("${orrery.gms.port}") int gmsPort,
      @Value("${orrery.gms.basePath:}") String gmsBasePath,
      @Value("${orrery.gms.basePathEnabled:false}") boolean gmsBasePathEnabled,
      @Value("${orrery.gms.useSSL}") boolean gmsUseSSL,
      @Value("${orrery.gms.uri}") String gmsUri,
      @Value("${orrery.gms.sslContext.protocol}") String gmsSslProtocol,
      final EntityClientConfig entityClientConfig,
      final MetricUtils metricUtils,
      final RestliRequestContextResolver restliRequestContextResolver) {
    final Client restClient;
    RestliClientSslConfig sslConfig = buildSslConfig();
    if (gmsUri != null) {
      restClient =
          DefaultRestliClientFactory.getRestLiClient(URI.create(gmsUri), gmsSslProtocol, sslConfig);
    } else {
      // Use the same logic as GMSConfiguration.getResolvedBasePath()
      String resolvedBasePath = BasePathUtils.resolveBasePath(gmsBasePathEnabled, gmsBasePath);
      restClient =
          DefaultRestliClientFactory.getRestLiClient(
              gmsHost, gmsPort, resolvedBasePath, gmsUseSSL, gmsSslProtocol, null, sslConfig);
    }
    return new RestliEntityClient(
        restClient, entityClientConfig, metricUtils, restliRequestContextResolver);
  }

  @Bean("systemEntityClient")
  public SystemEntityClient systemEntityClient(
      @Value("${orrery.gms.host}") String gmsHost,
      @Value("${orrery.gms.port}") int gmsPort,
      @Value("${orrery.gms.basePath:}") String gmsBasePath,
      @Value("${orrery.gms.basePathEnabled:false}") boolean gmsBasePathEnabled,
      @Value("${orrery.gms.useSSL}") boolean gmsUseSSL,
      @Value("${orrery.gms.uri}") String gmsUri,
      @Value("${orrery.gms.sslContext.protocol}") String gmsSslProtocol,
      final EntityClientCacheConfig entityClientCacheConfig,
      final EntityClientConfig entityClientConfig,
      final MetricUtils metricUtils,
      final RestliRequestContextResolver restliRequestContextResolver) {

    final Client restClient;
    RestliClientSslConfig sslConfig = buildSslConfig();
    if (gmsUri != null) {
      restClient =
          DefaultRestliClientFactory.getRestLiClient(URI.create(gmsUri), gmsSslProtocol, sslConfig);
    } else {
      // Use the same logic as GMSConfiguration.getResolvedBasePath()
      String resolvedBasePath = BasePathUtils.resolveBasePath(gmsBasePathEnabled, gmsBasePath);
      restClient =
          DefaultRestliClientFactory.getRestLiClient(
              gmsHost, gmsPort, resolvedBasePath, gmsUseSSL, gmsSslProtocol, null, sslConfig);
    }
    return new SystemRestliEntityClient(
        restClient,
        entityClientConfig,
        entityClientCacheConfig,
        metricUtils,
        restliRequestContextResolver);
  }

  private RestliClientSslConfig buildSslConfig() {
    return RestliClientSslConfig.fromNullableStrings(
        truststorePath,
        truststorePassword,
        truststoreType,
        keystorePath,
        keystorePassword,
        keystoreType,
        keyPassword);
  }
}
