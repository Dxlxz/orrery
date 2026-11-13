package com.orrery.authorization;

import com.orrery.authorization.config.ViewAuthorizationConfiguration;
import com.orrery.plugins.auth.authorization.Authorizer;
import java.util.List;
import lombok.Data;

/** POJO representing the "authorization" configuration block in application.yaml. */
@Data
public class AuthorizationConfiguration {
  /** Configuration for the default Orrery Policies-based authorizer. */
  private DefaultAuthorizerConfiguration defaultAuthorizer;

  /** List of configurations for {@link Authorizer}s to be registered */
  private List<AuthorizerConfiguration> authorizers;

  private ViewAuthorizationConfiguration view;

  private RegistryApiConfiguration registryApi = new RegistryApiConfiguration();
}
