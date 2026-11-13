package com.orrery.plugins.test;

import com.orrery.authentication.Actor;
import com.orrery.authentication.ActorType;
import com.orrery.authentication.Authentication;
import com.orrery.authentication.AuthenticationException;
import com.orrery.authentication.AuthenticationRequest;
import com.orrery.authentication.AuthenticatorContext;
import com.orrery.plugins.auth.authentication.Authenticator;
import java.util.Map;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TestLenientModeAuthenticator implements Authenticator {
  @Override
  public void init(
      @Nonnull Map<String, Object> authenticatorConfig, @Nullable AuthenticatorContext context) {}

  @Nullable
  @Override
  public Authentication authenticate(@Nonnull AuthenticationRequest authenticationRequest)
      throws AuthenticationException {
    // We should be able to access user directory as we are going to be loaded with Lenient mode
    // IsolatedClassLoader
    String userHome = System.getProperty("user.home");
    assert userHome != null;
    return new Authentication(new Actor(ActorType.USER, "fake"), "foo:bar");
  }
}
