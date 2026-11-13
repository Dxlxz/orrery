package com.orrery.authentication.authenticator;

import static com.orrery.authentication.AuthenticationConstants.SYSTEM_CLIENT_ID_CONFIG;

import com.orrery.authentication.Actor;
import com.orrery.authentication.ActorType;
import com.orrery.authentication.Authentication;
import com.orrery.authentication.AuthenticationException;
import com.orrery.authentication.AuthenticationRequest;
import com.orrery.authentication.AuthenticatorContext;
import com.orrery.plugins.auth.authentication.Authenticator;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

/**
 * This Authenticator is used for allowing access for unauthenticated health check endpoints
 *
 * <p>It exists to support load balancers, liveness/readiness checks
 */
@Slf4j
public class HealthStatusAuthenticator implements Authenticator {
  private static final Set<String> HEALTH_ENDPOINTS =
      Set.of("/openapi/check/", "/openapi/up/", "/actuator/health", "/health");
  private String systemClientId;

  @Override
  public void init(
      @Nonnull final Map<String, Object> config, @Nullable final AuthenticatorContext context) {
    Objects.requireNonNull(config, "Config parameter cannot be null");
    this.systemClientId =
        Objects.requireNonNull(
            (String) config.get(SYSTEM_CLIENT_ID_CONFIG),
            String.format("Missing required config %s", SYSTEM_CLIENT_ID_CONFIG));
  }

  @Override
  public Authentication authenticate(@Nonnull AuthenticationRequest context)
      throws AuthenticationException {
    Objects.requireNonNull(context);
    if (HEALTH_ENDPOINTS.stream()
        .anyMatch(
            prefix ->
                String.join("", context.getServletInfo(), context.getPathInfo())
                    .startsWith(prefix))) {
      return new Authentication(
          new Actor(ActorType.USER, systemClientId), "", Collections.emptyMap());
    }
    throw new AuthenticationException("Authorization not allowed. Non-health check endpoint.");
  }
}
