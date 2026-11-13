package io.orreryproject.iceberg.catalog.rest.common;

import io.orreryproject.iceberg.catalog.credentials.CachingCredentialProvider;
import io.orreryproject.iceberg.catalog.credentials.CredentialProvider;
import io.orreryproject.iceberg.catalog.credentials.S3CredentialProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.services.sts.StsClient;

@Configuration
public class IcebergSpringWebConfig {

  @Bean(destroyMethod = "close")
  public S3CredentialProvider credentialProvider(
      @Autowired(required = false) @Qualifier("stsClient") StsClient stsClient) {
    return new S3CredentialProvider(stsClient);
  }

  @Bean
  public CredentialProvider cachingCredentialProvider(S3CredentialProvider credentialProvider) {
    return new CachingCredentialProvider(credentialProvider);
  }
}
