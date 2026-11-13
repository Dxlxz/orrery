package com.linkedin.gms.factory.context.services;

import io.orreryproject.metadata.services.RestrictedService;
import io.orreryproject.metadata.services.SecretService;
import javax.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class RestrictedServiceFactory {

  @Autowired
  @Qualifier("orrerySecretService")
  private SecretService secretService;

  @Bean(name = "restrictedService")
  @Scope("singleton")
  @Nonnull
  protected RestrictedService getInstance() throws Exception {
    return new RestrictedService(secretService);
  }
}
