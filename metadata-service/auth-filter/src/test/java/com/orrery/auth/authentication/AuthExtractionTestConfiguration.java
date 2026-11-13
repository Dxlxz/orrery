package com.orrery.auth.authentication;

import static org.mockito.Mockito.*;

import com.orrery.auth.authentication.filter.AuthenticationExtractionFilter;
import com.orrery.authentication.AuthenticationConfiguration;
import com.orrery.authentication.AuthenticatorConfiguration;
import com.orrery.authentication.TokenServiceConfiguration;
import com.orrery.authentication.token.StatefulTokenService;
import com.linkedin.gms.factory.config.ConfigurationProvider;
import com.linkedin.metadata.config.AuthPluginConfiguration;
import com.linkedin.metadata.config.OrreryConfiguration;
import com.linkedin.metadata.config.PluginConfiguration;
import com.linkedin.metadata.entity.EntityService;
import io.orreryproject.metadata.context.OperationContext;
import io.orreryproject.test.metadata.context.TestOperationContexts;
import jakarta.servlet.ServletException;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

/**
 * Test configuration for AuthenticationExtractionFilter tests.
 *
 * <p>Provides the necessary beans and configuration for testing the authentication extraction tier
 * of the two-tier authentication system.
 */
@Configuration
public class AuthExtractionTestConfiguration {

  @Bean
  public EntityService<?> entityService() {
    return mock(EntityService.class);
  }

  @Bean("systemOperationContext")
  public OperationContext systemOperationContext() {
    return TestOperationContexts.systemContextNoSearchAuthorization();
  }

  @Bean("orreryTokenService")
  public StatefulTokenService statefulTokenService(
      ConfigurationProvider configurationProvider, EntityService<?> entityService) {
    TokenServiceConfiguration tokenServiceConfiguration =
        configurationProvider.getAuthentication().getTokenService();
    return new StatefulTokenService(
        TestOperationContexts.systemContextNoSearchAuthorization(),
        tokenServiceConfiguration.getSigningKey(),
        tokenServiceConfiguration.getSigningAlgorithm(),
        tokenServiceConfiguration.getIssuer(),
        entityService,
        tokenServiceConfiguration.getSalt(),
        AuthFilterHazelcast.instance());
  }

  @Bean
  public ConfigurationProvider configurationProvider() {
    ConfigurationProvider configurationProvider = new ConfigurationProvider();
    AuthenticationConfiguration authenticationConfiguration = new AuthenticationConfiguration();
    authenticationConfiguration.setEnabled(true);
    configurationProvider.setAuthentication(authenticationConfiguration);
    OrreryConfiguration orreryConfiguration = new OrreryConfiguration();
    PluginConfiguration pluginConfiguration = new PluginConfiguration();
    AuthPluginConfiguration authPluginConfiguration = new AuthPluginConfiguration();
    authenticationConfiguration.setSystemClientId("__orrery_system");
    authenticationConfiguration.setSystemClientSecret("JohnSnowKnowsNothing");
    TokenServiceConfiguration tokenServiceConfiguration = new TokenServiceConfiguration();
    tokenServiceConfiguration.setIssuer("orrery-metadata-service");
    tokenServiceConfiguration.setSigningKey("WnEdIeTG/VVCLQqGwC/BAkqyY0k+H8NEAtWGejrBI94=");
    tokenServiceConfiguration.setSalt("ohDVbJBvHHVJh9S/UA4BYF9COuNnqqVhr9MLKEGXk1O=");
    tokenServiceConfiguration.setSigningAlgorithm("HS256");
    authenticationConfiguration.setTokenService(tokenServiceConfiguration);
    AuthenticatorConfiguration authenticator = new AuthenticatorConfiguration();
    authenticator.setType("com.orrery.authentication.authenticator.OrreryTokenAuthenticator");
    authenticator.setConfigs(
        Map.of(
            "signingKey",
            "WnEdIeTG/VVCLQqGwC/BAkqyY0k+H8NEAtWGejrBI94=",
            "salt",
            "ohDVbJBvHHVJh9S/UA4BYF9COuNnqqVhr9MLKEGXk1O="));
    List<AuthenticatorConfiguration> authenticators = List.of(authenticator);
    authenticationConfiguration.setAuthenticators(authenticators);
    authPluginConfiguration.setPath("");
    pluginConfiguration.setAuth(authPluginConfiguration);
    orreryConfiguration.setPlugin(pluginConfiguration);
    configurationProvider.setOrrery(orreryConfiguration);
    return configurationProvider;
  }

  @Bean
  @DependsOn({
    "configurationProvider",
    "orreryTokenService",
    "entityService",
    "systemOperationContext"
  })
  public AuthenticationExtractionFilter authenticationExtractionFilter() throws ServletException {
    return new AuthenticationExtractionFilter();
  }
}
