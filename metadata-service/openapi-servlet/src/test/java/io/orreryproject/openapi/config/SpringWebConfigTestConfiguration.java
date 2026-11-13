package io.orreryproject.openapi.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedin.gms.factory.config.ConfigurationProvider;
import com.linkedin.metadata.models.registry.EntityRegistry;
import io.orreryproject.metadata.context.OperationContext;
import io.orreryproject.openapi.v3.OpenAPIV3Generator;
import io.orreryproject.test.metadata.context.TestOperationContexts;
import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.properties.SpringDocConfigProperties;
import org.springdoc.core.providers.ObjectMapperProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

@EnableWebMvc
@TestConfiguration
public class SpringWebConfigTestConfiguration {

  @Bean
  public OperationContext systemOperationContext() {
    return TestOperationContexts.systemContextNoSearchAuthorization();
  }

  @Bean
  public EntityRegistry entityRegistry(final OperationContext systemOperationContext) {
    return systemOperationContext.getEntityRegistry();
  }

  @Bean
  public SpringDocConfigProperties springDocConfigProperties() {
    return new SpringDocConfigProperties();
  }

  @Bean
  public ObjectMapperProvider objectMapperProvider(
      SpringDocConfigProperties springDocConfigProperties) {
    return new ObjectMapperProvider(springDocConfigProperties) {
      @Override
      public ObjectMapper jsonMapper() {
        ObjectMapper mapper = super.jsonMapper();
        return mapper;
      }
    };
  }

  @Bean
  public OpenAPI openAPIs(
      final EntityRegistry entityRegistry, final ConfigurationProvider configurationProvider) {
    return OpenAPIV3Generator.generateOpenApiSpec(entityRegistry, configurationProvider);
  }
}
