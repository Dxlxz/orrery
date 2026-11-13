package com.orrery.authentication;

import com.orrery.plugins.auth.authentication.Authenticator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;

/**
 * Context class to provide Authenticator implementations with concrete objects necessary for their
 * correct workings. Orrery creates {@link AuthenticatorContext} instance and provides it as an
 * argument to init method of {@link Authenticator}
 */
public class AuthenticatorContext {
  private final Map<String, Object> contextMap;

  public AuthenticatorContext(@Nonnull final Map<String, Object> context) {
    Objects.requireNonNull(context);
    contextMap = new HashMap<>();
    contextMap.putAll(context);
  }

  /**
   * @return contextMap The contextMap contains below key and value {@link
   *     com.orrery.plugins.PluginConstant#PLUGIN_HOME PLUGIN_HOME}: Directory path where plugin is
   *     installed
   */
  @Nonnull
  public Map<String, Object> data() {
    return contextMap;
  }
}
