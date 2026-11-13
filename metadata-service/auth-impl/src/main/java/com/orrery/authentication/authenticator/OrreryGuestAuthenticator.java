package com.orrery.authentication.authenticator;

import static com.orrery.authentication.AuthenticationConstants.*;

import com.orrery.authentication.Actor;
import com.orrery.authentication.ActorType;
import com.orrery.authentication.Authentication;
import com.orrery.authentication.AuthenticationException;
import com.orrery.authentication.AuthenticationRequest;
import com.orrery.authentication.AuthenticatorContext;
import com.orrery.plugins.auth.authentication.Authenticator;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;

/**
 * Authenticator that enables Guest users (when configured), bypassing authentication. This still
 * requires a specific user to be configured and designated as the guest user. A User who has not
 * authenticated will be treated as the guest user and all the guest user permissions will apply
 *
 * <p>This authenticator requires the name of the guest user that must be implicitly used as the
 * logged in user.
 */
@Slf4j
public class OrreryGuestAuthenticator implements Authenticator {

  public static final String GUEST_USER = "guestUser";
  public static final String ENABLED = "enabled";
  static final String DEFAULT_GUEST_USER = "guest";
  String guestUser = DEFAULT_GUEST_USER;
  boolean enabled = false;

  @Override
  public void init(final Map<String, Object> config, final AuthenticatorContext context) {
    if (config != null) {
      if (config.containsKey(GUEST_USER)) {
        guestUser = (String) config.get(GUEST_USER);
      }
      if (config.containsKey(ENABLED)) {
        enabled = Boolean.parseBoolean((String) config.get(ENABLED));
      }
    }
  }

  @Override
  public Authentication authenticate(@Nonnull AuthenticationRequest context)
      throws AuthenticationException {
    if (!enabled) {
      throw new AuthenticationException("Guest Authentication is disabled");
    }
    return new Authentication(new Actor(ActorType.USER, guestUser), "guestUser");
  }
}
