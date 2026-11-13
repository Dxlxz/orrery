package com.orrery.authentication.authenticator;

import static com.orrery.authentication.AuthenticationConstants.*;
import static org.testng.Assert.*;

import com.orrery.authentication.ActorType;
import com.orrery.authentication.Authentication;
import com.orrery.authentication.AuthenticationException;
import com.orrery.authentication.AuthenticationRequest;
import com.google.common.collect.ImmutableMap;
import java.util.Collections;
import org.testng.annotations.Test;

public class OrrerySystemAuthenticatorTest {

  private static final String TEST_CLIENT_ID = "clientId";
  private static final String TEST_CLIENT_SECRET = "clientSecret";

  @Test
  public void testInit() {
    final OrrerySystemAuthenticator authenticator = new OrrerySystemAuthenticator();
    assertThrows(() -> authenticator.init(null, null));
    assertThrows(() -> authenticator.init(Collections.emptyMap(), null));
    assertThrows(
        () -> authenticator.init(ImmutableMap.of(SYSTEM_CLIENT_ID_CONFIG, TEST_CLIENT_ID), null));
    assertThrows(
        () ->
            authenticator.init(
                ImmutableMap.of(SYSTEM_CLIENT_SECRET_CONFIG, TEST_CLIENT_SECRET), null));

    // Correct configs provided.
    authenticator.init(
        ImmutableMap.of(
            SYSTEM_CLIENT_ID_CONFIG,
            TEST_CLIENT_ID,
            SYSTEM_CLIENT_SECRET_CONFIG,
            TEST_CLIENT_SECRET),
        null);
  }

  @Test
  public void testAuthenticateFailureMissingAuthorizationHeader() {
    final OrrerySystemAuthenticator authenticator = new OrrerySystemAuthenticator();
    authenticator.init(
        ImmutableMap.of(
            SYSTEM_CLIENT_ID_CONFIG,
            TEST_CLIENT_ID,
            SYSTEM_CLIENT_SECRET_CONFIG,
            TEST_CLIENT_SECRET),
        null);

    final AuthenticationRequest context = new AuthenticationRequest(Collections.emptyMap());
    assertThrows(AuthenticationException.class, () -> authenticator.authenticate(context));
  }

  @Test
  public void testAuthenticateFailureMissingBasicCredentials() {
    final OrrerySystemAuthenticator authenticator = new OrrerySystemAuthenticator();
    authenticator.init(
        ImmutableMap.of(
            SYSTEM_CLIENT_ID_CONFIG,
            TEST_CLIENT_ID,
            SYSTEM_CLIENT_SECRET_CONFIG,
            TEST_CLIENT_SECRET),
        null);

    final AuthenticationRequest context =
        new AuthenticationRequest(
            ImmutableMap.of(
                AUTHORIZATION_HEADER_NAME, "Bearer something") // Missing basic authentication.
            );
    assertThrows(AuthenticationException.class, () -> authenticator.authenticate(context));
  }

  @Test
  public void testAuthenticateFailureMismatchingCredentials() {
    final OrrerySystemAuthenticator authenticator = new OrrerySystemAuthenticator();
    authenticator.init(
        ImmutableMap.of(
            SYSTEM_CLIENT_ID_CONFIG,
            TEST_CLIENT_ID,
            SYSTEM_CLIENT_SECRET_CONFIG,
            TEST_CLIENT_SECRET),
        null);

    final AuthenticationRequest context =
        new AuthenticationRequest(
            ImmutableMap.of(
                AUTHORIZATION_HEADER_NAME,
                "Basic incorrectId:incorrectSecret") // Incorrect authentication
            );
    assertThrows(AuthenticationException.class, () -> authenticator.authenticate(context));
  }

  @Test
  public void testAuthenticateSuccessNoDelegatedActor() throws Exception {

    final OrrerySystemAuthenticator authenticator = new OrrerySystemAuthenticator();
    authenticator.init(
        ImmutableMap.of(
            SYSTEM_CLIENT_ID_CONFIG,
            TEST_CLIENT_ID,
            SYSTEM_CLIENT_SECRET_CONFIG,
            TEST_CLIENT_SECRET),
        null);

    final String authorizationHeaderValue =
        String.format("Basic %s:%s", TEST_CLIENT_ID, TEST_CLIENT_SECRET);
    final AuthenticationRequest context =
        new AuthenticationRequest(
            ImmutableMap.of(AUTHORIZATION_HEADER_NAME, authorizationHeaderValue));

    final Authentication authentication = authenticator.authenticate(context);

    // Validate the resulting authentication object
    assertNotNull(authentication);
    assertEquals(authentication.getActor().getType(), ActorType.USER);
    assertEquals(authentication.getActor().getId(), TEST_CLIENT_ID);
    assertEquals(authentication.getCredentials(), authorizationHeaderValue);
    assertEquals(authentication.getClaims(), Collections.emptyMap());
  }

  @Test
  public void testAuthenticateSuccessDelegatedActor() throws Exception {

    final OrrerySystemAuthenticator authenticator = new OrrerySystemAuthenticator();
    authenticator.init(
        ImmutableMap.of(
            SYSTEM_CLIENT_ID_CONFIG,
            TEST_CLIENT_ID,
            SYSTEM_CLIENT_SECRET_CONFIG,
            TEST_CLIENT_SECRET),
        null);

    final String authorizationHeaderValue =
        String.format("Basic %s:%s", TEST_CLIENT_ID, TEST_CLIENT_SECRET);
    final AuthenticationRequest context =
        new AuthenticationRequest(
            ImmutableMap.of(
                AUTHORIZATION_HEADER_NAME,
                authorizationHeaderValue,
                LEGACY_X_ORRERY_ACTOR_HEADER,
                "urn:li:corpuser:orrery"));

    final Authentication authentication = authenticator.authenticate(context);

    // Validate the resulting authentication object
    assertNotNull(authentication);
    assertEquals(authentication.getActor().getType(), ActorType.USER);
    assertEquals(authentication.getActor().getId(), TEST_CLIENT_ID);
    assertEquals(authentication.getCredentials(), authorizationHeaderValue);
    assertEquals(authentication.getClaims(), Collections.emptyMap());
  }

  @Test
  public void testAuthenticateSuccessWithPasswordContainingSingleColon() throws Exception {
    final String secretWithColon = "my:secret";
    final OrrerySystemAuthenticator authenticator = new OrrerySystemAuthenticator();
    authenticator.init(
        ImmutableMap.of(
            SYSTEM_CLIENT_ID_CONFIG, TEST_CLIENT_ID, SYSTEM_CLIENT_SECRET_CONFIG, secretWithColon),
        null);

    final String authorizationHeaderValue =
        String.format("Basic %s:%s", TEST_CLIENT_ID, secretWithColon);
    final AuthenticationRequest context =
        new AuthenticationRequest(
            ImmutableMap.of(AUTHORIZATION_HEADER_NAME, authorizationHeaderValue));

    final Authentication authentication = authenticator.authenticate(context);

    assertNotNull(authentication);
    assertEquals(authentication.getActor().getType(), ActorType.USER);
    assertEquals(authentication.getActor().getId(), TEST_CLIENT_ID);
    assertEquals(authentication.getCredentials(), authorizationHeaderValue);
  }

  @Test
  public void testAuthenticateSuccessWithPasswordContainingMultipleColons() throws Exception {
    final String secretWithMultipleColons = "my:complex:secret:2025";
    final OrrerySystemAuthenticator authenticator = new OrrerySystemAuthenticator();
    authenticator.init(
        ImmutableMap.of(
            SYSTEM_CLIENT_ID_CONFIG,
            TEST_CLIENT_ID,
            SYSTEM_CLIENT_SECRET_CONFIG,
            secretWithMultipleColons),
        null);

    final String authorizationHeaderValue =
        String.format("Basic %s:%s", TEST_CLIENT_ID, secretWithMultipleColons);
    final AuthenticationRequest context =
        new AuthenticationRequest(
            ImmutableMap.of(AUTHORIZATION_HEADER_NAME, authorizationHeaderValue));

    final Authentication authentication = authenticator.authenticate(context);

    assertNotNull(authentication);
    assertEquals(authentication.getActor().getType(), ActorType.USER);
    assertEquals(authentication.getActor().getId(), TEST_CLIENT_ID);
    assertEquals(authentication.getCredentials(), authorizationHeaderValue);
  }
}
