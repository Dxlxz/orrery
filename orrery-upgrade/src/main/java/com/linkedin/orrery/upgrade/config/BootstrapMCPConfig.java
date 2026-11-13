package com.linkedin.orrery.upgrade.config;

import com.linkedin.orrery.upgrade.system.bootstrapmcps.BootstrapMCP;
import com.linkedin.metadata.entity.EntityService;
import io.orreryproject.metadata.context.OperationContext;
import java.io.IOException;
import javax.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BootstrapMCPConfig {

  @Nonnull
  @Value("${systemUpdate.bootstrap.mcpConfig}")
  private String bootstrapMCPConfig;

  @Bean(name = "bootstrapMCPNonBlocking")
  public BootstrapMCP bootstrapMCPNonBlocking(
      final OperationContext opContext, EntityService<?> entityService) throws IOException {
    return new BootstrapMCP(opContext, bootstrapMCPConfig, entityService, false);
  }

  @Bean(name = "bootstrapMCPBlocking")
  public BootstrapMCP bootstrapMCPBlocking(
      final OperationContext opContext, EntityService<?> entityService) throws IOException {
    return new BootstrapMCP(opContext, bootstrapMCPConfig, entityService, true);
  }
}
