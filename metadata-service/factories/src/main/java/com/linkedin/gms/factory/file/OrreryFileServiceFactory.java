package com.linkedin.gms.factory.file;

import com.linkedin.entity.client.EntityClient;
import com.linkedin.metadata.service.OrreryFileService;
import javax.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrreryFileServiceFactory {
  @Bean(name = "orreryFileService")
  @Nonnull
  protected OrreryFileService getInstance(
      @Qualifier("entityClient") final EntityClient entityClient) throws Exception {
    return new OrreryFileService(entityClient);
  }
}
