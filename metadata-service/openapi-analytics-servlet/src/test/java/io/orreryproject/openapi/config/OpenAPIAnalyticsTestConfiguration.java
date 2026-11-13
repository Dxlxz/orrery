package io.orreryproject.openapi.config;

import static io.orreryproject.openapi.delegates.OrreryUsageEventsImpl.ORRERY_USAGE_INDEX;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.orrery.authentication.Actor;
import com.orrery.authentication.ActorType;
import com.orrery.authentication.Authentication;
import com.orrery.authentication.AuthenticationContext;
import com.orrery.authorization.AuthorizationResult;
import com.orrery.authorization.AuthorizerChain;
import com.linkedin.metadata.models.registry.*;
import com.linkedin.metadata.search.elasticsearch.ElasticSearchService;
import com.linkedin.metadata.search.utils.SearchOperation;
import io.orreryproject.metadata.context.OperationContext;
import io.orreryproject.test.metadata.context.TestOperationContexts;
import java.io.IOException;
import java.util.Optional;
import org.mockito.Mockito;
import org.opensearch.action.search.SearchResponse;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

@TestConfiguration
public class OpenAPIAnalyticsTestConfiguration {
  @Bean(name = "systemOperationContext")
  public OperationContext systemOperationContext() {
    return TestOperationContexts.systemContextNoSearchAuthorization();
  }

  @Bean
  @Primary
  public ElasticSearchService orreryUsageEventsApiDelegate() throws IOException {
    ElasticSearchService elasticSearchService = mock(ElasticSearchService.class);
    SearchResponse mockResp = mock(SearchResponse.class);
    when(elasticSearchService.raw(
            any(OperationContext.class),
            eq(ORRERY_USAGE_INDEX),
            anyString(),
            eq(SearchOperation.ORRERY_USAGE)))
        .thenReturn(Optional.of(mockResp));
    return elasticSearchService;
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
    return new ConfigEntityRegistry(
        OpenAPIAnalyticsTestConfiguration.class
            .getClassLoader()
            .getResourceAsStream("entity-registry.yml"));
  }
}
