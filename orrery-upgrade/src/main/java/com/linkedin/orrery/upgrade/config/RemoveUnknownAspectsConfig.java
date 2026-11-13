package com.linkedin.orrery.upgrade.config;

import com.linkedin.orrery.upgrade.removeunknownaspects.RemoveUnknownAspects;
import com.linkedin.metadata.entity.EntityService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RemoveUnknownAspectsConfig {
  @Bean(name = "removeUnknownAspects")
  public RemoveUnknownAspects removeUnknownAspects(EntityService<?> entityService) {
    return new RemoveUnknownAspects(entityService);
  }
}
