package com.orrery.authentication.authenticator;

import static com.orrery.authentication.AuthenticationConstants.*;
import static com.orrery.authentication.authenticator.OrreryGuestAuthenticator.*;
import static com.orrery.authentication.token.TokenClaims.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import com.orrery.authentication.Authentication;
import com.orrery.authentication.AuthenticationException;
import com.orrery.authentication.AuthenticationRequest;
import com.orrery.authentication.AuthenticatorContext;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import org.testng.annotations.Test;

public class OrreryGuestAuthenticatorTest {
  @Test
  public void testInit() {
    final OrreryGuestAuthenticator authenticator = new OrreryGuestAuthenticator();
    AuthenticatorContext authenticatorContext = new AuthenticatorContext(Collections.emptyMap());
    authenticator.init(null, authenticatorContext);
    assertEquals(authenticator.guestUser, DEFAULT_GUEST_USER);
    assertFalse(authenticator.enabled);

    authenticator.init(Collections.emptyMap(), authenticatorContext);
    assertEquals(authenticator.guestUser, DEFAULT_GUEST_USER);
    assertFalse(authenticator.enabled);

    // Correct configs provided.
    authenticator.init(
        ImmutableMap.of(GUEST_USER, "publicUser", ENABLED, "false"), authenticatorContext);
    assertEquals(authenticator.guestUser, "publicUser");
    assertFalse(authenticator.enabled);

    // Correct configs provided.
    authenticator.init(ImmutableMap.of(GUEST_USER, "guest", ENABLED, "true"), authenticatorContext);
    assertEquals(authenticator.guestUser, "guest");
    assertTrue(authenticator.enabled);
  }

  @Test
  public void testGuestUserDisabled() {
    final OrreryGuestAuthenticator authenticator = new OrreryGuestAuthenticator();

    authenticator.init(Collections.emptyMap(), new AuthenticatorContext(Collections.emptyMap()));

    final AuthenticationRequest context = new AuthenticationRequest(Collections.emptyMap());
    assertThrows(() -> authenticator.authenticate(context));
  }

  @Test
  public void testGuestUserEnabled() throws AuthenticationException {
    final OrreryGuestAuthenticator authenticator = new OrreryGuestAuthenticator();

    authenticator.init(
        ImmutableMap.of(GUEST_USER, "guest", ENABLED, "true"),
        new AuthenticatorContext(Collections.emptyMap()));

    final AuthenticationRequest context = new AuthenticationRequest(Collections.emptyMap());
    Authentication authentication = authenticator.authenticate(context);
    assertNotNull(authentication);
    assertEquals(authentication.getActor().getId(), "guest");
  }
}
