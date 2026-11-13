package com.orrery.authentication;

import java.util.Map;
import lombok.Data;

/**
 * POJO representing {@link com.orrery.plugins.auth.authentication.Authenticator} configurations
 * provided in the application.yaml.
 */
@Data
public class AuthenticatorConfiguration {
  /**
   * A fully-qualified class name for the {@link
   * com.orrery.plugins.auth.authentication.Authenticator} implementation to be registered.
   */
  private String type;

  /**
   * A set of authenticator-specific configurations passed through during "init" of the
   * authenticator.
   */
  private Map<String, Object> configs;
}
