package com.linkedin.gms.factory.usage;

import com.linkedin.common.client.restli.RestliRequestContextResolver;
import com.linkedin.gms.factory.config.ConfigurationProvider;
import com.linkedin.gms.factory.restli.RestliRequestContextResolverFactory;
import com.linkedin.metadata.restli.DefaultRestliClientFactory;
import com.linkedin.metadata.restli.RestliClientSslConfig;
import com.linkedin.metadata.utils.metrics.MetricUtils;
import com.linkedin.parseq.retry.backoff.ExponentialBackoff;
import com.linkedin.r2.transport.http.client.HttpClientFactory;
import com.linkedin.restli.client.Client;
import com.linkedin.usage.RestliUsageClient;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(RestliRequestContextResolverFactory.class)
public class UsageClientFactory {

  @Value("${orrery.gms.host:localhost}")
  private String gmsHost;

  @Value("${orrery.gms.port:8080}")
  private int gmsPort;

  @Value("${orrery.gms.useSSL:false}")
  private boolean gmsUseSSL;

  @Value("${orrery.gms.sslContext.protocol:#{null}}")
  private String gmsSslProtocol;

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

  @Value("${usageClient.retryInterval:2}")
  private int retryInterval;

  @Value("${usageClient.numRetries:0}")
  private int numRetries;

  @Value("${usageClient.timeoutMs:3000}")
  private long timeoutMs;

  @Autowired
  @Qualifier("configurationProvider")
  private ConfigurationProvider configurationProvider;

  @Bean("usageClient")
  public RestliUsageClient getUsageClient(
      MetricUtils metricUtils, RestliRequestContextResolver restliRequestContextResolver) {
    Map<String, String> params = new HashMap<>();
    params.put(HttpClientFactory.HTTP_REQUEST_TIMEOUT, String.valueOf(timeoutMs));

    RestliClientSslConfig sslConfig = buildSslConfig();
    Client restClient =
        DefaultRestliClientFactory.getRestLiClient(
            gmsHost, gmsPort, gmsUseSSL, gmsSslProtocol, params, sslConfig);
    return new RestliUsageClient(
        restClient,
        new ExponentialBackoff(retryInterval),
        numRetries,
        configurationProvider.getCache().getClient().getUsageClient(),
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
