package io.orreryproject.openapi.config;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.orrery.authentication.Actor;
import com.orrery.authentication.ActorType;
import com.orrery.authentication.Authentication;
import com.orrery.authentication.AuthenticationContext;
import com.orrery.authorization.AuthorizationResult;
import com.orrery.authorization.AuthorizerChain;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.linkedin.metadata.models.registry.ConfigEntityRegistry;
import com.linkedin.metadata.models.registry.EntityRegistry;
import com.linkedin.metadata.models.registry.EntityRegistryException;
import com.linkedin.metadata.models.registry.MergedEntityRegistry;
import com.linkedin.metadata.models.registry.PluginEntityRegistryLoader;
import com.linkedin.metadata.models.registry.SnapshotEntityRegistry;
import com.linkedin.metadata.search.ScrollResult;
import com.linkedin.metadata.search.SearchEntityArray;
import com.linkedin.metadata.search.SearchService;
import io.orreryproject.metadata.context.OperationContext;
import io.orreryproject.openapi.dto.UrnResponseMap;
import io.orreryproject.openapi.generated.EntityResponse;
import io.orreryproject.openapi.v1.entities.EntitiesController;
import io.orreryproject.test.metadata.context.TestOperationContexts;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.ResponseEntity;

@TestConfiguration
public class OpenAPIEntityTestConfiguration {
  @Bean
  public ObjectMapper objectMapper() {
    return new ObjectMapper(new YAMLFactory());
  }

  @Bean
  @Primary
  public SearchService searchService() {
    SearchService searchService = mock(SearchService.class);
    when(searchService.scrollAcrossEntities(
            any(OperationContext.class), anyList(), any(), any(), any(), any(), any(), anyInt()))
        .thenReturn(new ScrollResult().setEntities(new SearchEntityArray()));

    return searchService;
  }

  @Bean
  public AuthorizerChain authorizerChain() {
    AuthorizerChain authorizerChain = Mockito.mock(AuthorizerChain.class);

    Authentication authentication = Mockito.mock(Authentication.class);
    when(authentication.getActor()).thenReturn(new Actor(ActorType.USER, "orrery"));
    when(authorizerChain.authorize(any()))
        .thenReturn(new AuthorizationResult(null, AuthorizationResult.Type.ALLOW, ""));
    AuthenticationContext.setAuthentication(authentication);

    return authorizerChain;
  }

  @Bean("entityRegistry")
  @Primary
  public EntityRegistry entityRegistry() throws EntityRegistryException, InterruptedException {
    /*
      Considered a few different approach to loading a custom model. Chose this method
      to as closely match a production configuration rather than direct project to project
      dependency.
    */
    PluginEntityRegistryLoader custom =
        new PluginEntityRegistryLoader(
            getClass().getResource("/custom-model").getFile(), 60, true, null);

    ConfigEntityRegistry standard =
        new ConfigEntityRegistry(
            OpenAPIEntityTestConfiguration.class
                .getClassLoader()
                .getResourceAsStream("entity-registry.yml"));
    MergedEntityRegistry entityRegistry =
        new MergedEntityRegistry(SnapshotEntityRegistry.getInstance()).apply(standard);
    custom.withBaseRegistry(entityRegistry).start(true);

    return entityRegistry;
  }

  /* Controllers not under this module */
  @Bean
  @Primary
  public EntitiesController entitiesController() {
    EntitiesController entitiesController = mock(EntitiesController.class);
    when(entitiesController.getEntities(nullable(HttpServletRequest.class), any(), any()))
        .thenAnswer(
            params -> {
              String[] urns = params.getArgument(1);
              String[] aspects = params.getArgument(2);
              return ResponseEntity.ok(
                  UrnResponseMap.builder()
                      .responses(
                          Arrays.stream(urns)
                              .map(urn -> Map.entry(urn, EntityResponse.builder().urn(urn).build()))
                              .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))
                      .build());
            });

    return entitiesController;
  }

  @Bean(name = "systemOperationContext")
  public OperationContext operationContext(final EntityRegistry entityRegistry) {
    return TestOperationContexts.systemContextNoSearchAuthorization(entityRegistry);
  }
}
