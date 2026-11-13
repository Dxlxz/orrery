package com.orrery.authentication.authenticator;

import static com.orrery.authentication.AuthenticationConstants.*;
import static com.orrery.authentication.token.TokenClaims.ACTOR_ID_CLAIM_NAME;
import static com.orrery.authentication.token.TokenClaims.ACTOR_TYPE_CLAIM_NAME;
import static com.orrery.authentication.token.TokenClaims.TOKEN_TYPE_CLAIM_NAME;
import static com.orrery.authentication.token.TokenClaims.TOKEN_VERSION_CLAIM_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.fail;

import com.orrery.authentication.Actor;
import com.orrery.authentication.ActorType;
import com.orrery.authentication.Authentication;
import com.orrery.authentication.AuthenticationException;
import com.orrery.authentication.AuthenticationExpiredException;
import com.orrery.authentication.AuthenticationRequest;
import com.orrery.authentication.AuthenticationUnavailableException;
import com.orrery.authentication.AuthenticatorContext;
import com.orrery.authentication.token.StatefulTokenService;
import com.orrery.authentication.token.TestHazelcast;
import com.orrery.authentication.token.TokenType;
import com.google.common.collect.ImmutableMap;
import com.linkedin.common.urn.Urn;
import com.linkedin.data.schema.annotation.PathSpecBasedSchemaAnnotationVisitor;
import com.linkedin.metadata.entity.EntityService;
import com.linkedin.metadata.models.registry.ConfigEntityRegistry;
import io.orreryproject.metadata.context.OperationContext;
import io.orreryproject.test.metadata.context.TestOperationContexts;
import jakarta.persistence.PersistenceException;
import java.util.Collections;
import java.util.Map;
import org.mockito.Mockito;
import org.testng.annotations.Test;

public class OrreryTokenAuthenticatorTest {

  private static final String TEST_SIGNING_KEY = "WnEdIeTG/VVCLQqGwC/BAkqyY0k+H8NEAtWGejrBI94=";
  private static final String TEST_SALT = "WnEdIeTG/VVCLQqGwC/BAkqyY0k+H8NEAtWGejrBI93=";
  private static final String SIGNING_KEY_CONFIG_NAME = "signingKey";
  private static final String SALT_CONFIG_NAME = "salt";
  private static final String SIGNING_ALG_CONFIG_NAME = "signingAlg";

  final EntityService mockService = mock(EntityService.class);
  final StatefulTokenService statefulTokenService =
      new StatefulTokenService(
          testOperationContext(),
          TEST_SIGNING_KEY,
          "HS256",
          null,
          mockService,
          TEST_SALT,
          TestHazelcast.instance());

  private static OperationContext testOperationContext() {
    PathSpecBasedSchemaAnnotationVisitor.class
        .getClassLoader()
        .setClassAssertionStatus(PathSpecBasedSchemaAnnotationVisitor.class.getName(), false);
    final ConfigEntityRegistry configEntityRegistry =
        new ConfigEntityRegistry(
            OrreryTokenAuthenticatorTest.class
                .getClassLoader()
                .getResourceAsStream("test-entity-registry.yaml"));

    return TestOperationContexts.systemContextNoSearchAuthorization(configEntityRegistry);
  }

  @Test
  public void testInit() {
    final OrreryTokenAuthenticator authenticator = new OrreryTokenAuthenticator();
    AuthenticatorContext authenticatorContext =
        new AuthenticatorContext(
            ImmutableMap.of(ENTITY_SERVICE, mockService, TOKEN_SERVICE, statefulTokenService));
    assertThrows(
        () ->
            authenticator.init(
                ImmutableMap.of(
                    SIGNING_KEY_CONFIG_NAME, TEST_SIGNING_KEY, SIGNING_ALG_CONFIG_NAME, "HS256"),
                null));

    // Correct configs provided.
    authenticator.init(
        ImmutableMap.of(
            SIGNING_KEY_CONFIG_NAME,
            TEST_SIGNING_KEY,
            SALT_CONFIG_NAME,
            TEST_SALT,
            SIGNING_ALG_CONFIG_NAME,
            "HS256"),
        authenticatorContext);
  }

  @Test
  public void testAuthenticateFailureMissingAuthorizationHeader() {
    final OrreryTokenAuthenticator authenticator = new OrreryTokenAuthenticator();

    authenticator.init(
        ImmutableMap.of(
            SIGNING_KEY_CONFIG_NAME,
            TEST_SIGNING_KEY,
            SALT_CONFIG_NAME,
            TEST_SALT,
            SIGNING_ALG_CONFIG_NAME,
            "HS256"),
        new AuthenticatorContext(
            ImmutableMap.of(ENTITY_SERVICE, mockService, TOKEN_SERVICE, statefulTokenService)));

    final AuthenticationRequest context = new AuthenticationRequest(Collections.emptyMap());
    assertThrows(AuthenticationException.class, () -> authenticator.authenticate(context));
  }

  @Test
  public void testAuthenticateFailureMissingBearerCredentials() {
    final OrreryTokenAuthenticator authenticator = new OrreryTokenAuthenticator();
    authenticator.init(
        ImmutableMap.of(
            SIGNING_KEY_CONFIG_NAME,
            TEST_SIGNING_KEY,
            SALT_CONFIG_NAME,
            TEST_SALT,
            SIGNING_ALG_CONFIG_NAME,
            "HS256"),
        new AuthenticatorContext(
            ImmutableMap.of(ENTITY_SERVICE, mockService, TOKEN_SERVICE, statefulTokenService)));

    final AuthenticationRequest context =
        new AuthenticationRequest(
            ImmutableMap.of(AUTHORIZATION_HEADER_NAME, "Basic username:password"));
    assertThrows(AuthenticationException.class, () -> authenticator.authenticate(context));
  }

  @Test
  public void testAuthenticateFailureInvalidToken() {
    final OrreryTokenAuthenticator authenticator = new OrreryTokenAuthenticator();

    authenticator.init(
        ImmutableMap.of(
            SIGNING_KEY_CONFIG_NAME,
            TEST_SIGNING_KEY,
            SALT_CONFIG_NAME,
            TEST_SALT,
            SIGNING_ALG_CONFIG_NAME,
            "HS256"),
        new AuthenticatorContext(
            ImmutableMap.of(ENTITY_SERVICE, mockService, TOKEN_SERVICE, statefulTokenService)));

    final AuthenticationRequest context =
        new AuthenticationRequest(
            ImmutableMap.of(AUTHORIZATION_HEADER_NAME, "Bearer someRandomToken"));
    assertThrows(AuthenticationException.class, () -> authenticator.authenticate(context));
  }

  @Test
  public void testAuthenticateSuccess() throws Exception {
    Mockito.when(mockService.exists(any(OperationContext.class), any(Urn.class), eq(true)))
        .thenReturn(true);

    final OrreryTokenAuthenticator authenticator = new OrreryTokenAuthenticator();
    authenticator.init(
        ImmutableMap.of(
            SIGNING_KEY_CONFIG_NAME,
            TEST_SIGNING_KEY,
            SALT_CONFIG_NAME,
            TEST_SALT,
            SIGNING_ALG_CONFIG_NAME,
            "HS256"),
        new AuthenticatorContext(
            ImmutableMap.of(ENTITY_SERVICE, mockService, TOKEN_SERVICE, statefulTokenService)));

    final Actor orrery = new Actor(ActorType.USER, "orrery");
    final String validToken =
        authenticator._statefulTokenService.generateAccessToken(
            testOperationContext(),
            TokenType.PERSONAL,
            orrery,
            "some token",
            "A token description",
            orrery.toUrnStr());

    final String authorizationHeaderValue = String.format("Bearer %s", validToken);
    final AuthenticationRequest context =
        new AuthenticationRequest(
            ImmutableMap.of(AUTHORIZATION_HEADER_NAME, authorizationHeaderValue));

    final Authentication authentication = authenticator.authenticate(context);

    // Validate the resulting authentication object
    assertNotNull(authentication);
    assertEquals(authentication.getActor().getType(), ActorType.USER);
    assertEquals(authentication.getActor().getId(), "orrery");
    assertEquals(authentication.getCredentials(), authorizationHeaderValue);

    Map<String, Object> claimsMap = authentication.getClaims();
    assertEquals(claimsMap.get(TOKEN_VERSION_CLAIM_NAME), 2);
    assertEquals(claimsMap.get(TOKEN_TYPE_CLAIM_NAME), "PERSONAL");
    assertEquals(claimsMap.get(ACTOR_TYPE_CLAIM_NAME), "USER");
    assertEquals(claimsMap.get(ACTOR_ID_CLAIM_NAME), "orrery");
  }

  @Test
  public void testAuthenticateTokenStoreUnavailable() throws Exception {
    // Issue a valid token with one service, then validate it on a fresh service (empty local
    // revocation map) whose entity store fails, as during database connection-pool exhaustion.
    Mockito.when(mockService.exists(any(OperationContext.class), any(Urn.class), eq(true)))
        .thenReturn(true);
    final Actor orrery = new Actor(ActorType.USER, "orrery");
    final String validToken =
        statefulTokenService.generateAccessToken(
            testOperationContext(),
            TokenType.PERSONAL,
            orrery,
            "some token",
            "A token description",
            orrery.toUrnStr());

    final EntityService failingService = mock(EntityService.class);
    Mockito.when(failingService.exists(any(OperationContext.class), any(Urn.class), eq(true)))
        .thenThrow(new PersistenceException("Unsuccessfully waited for a connection"));
    final StatefulTokenService coldTokenService =
        new StatefulTokenService(
            testOperationContext(),
            TEST_SIGNING_KEY,
            "HS256",
            null,
            failingService,
            TEST_SALT,
            null);

    final OrreryTokenAuthenticator authenticator = new OrreryTokenAuthenticator();
    authenticator.init(
        ImmutableMap.of(
            SIGNING_KEY_CONFIG_NAME,
            TEST_SIGNING_KEY,
            SALT_CONFIG_NAME,
            TEST_SALT,
            SIGNING_ALG_CONFIG_NAME,
            "HS256"),
        new AuthenticatorContext(
            ImmutableMap.of(ENTITY_SERVICE, failingService, TOKEN_SERVICE, coldTokenService)));

    final AuthenticationRequest context =
        new AuthenticationRequest(
            ImmutableMap.of(AUTHORIZATION_HEADER_NAME, String.format("Bearer %s", validToken)));
    assertThrows(
        AuthenticationUnavailableException.class, () -> authenticator.authenticate(context));
  }

  @Test
  public void testAuthenticateExpiredToken() {
    Mockito.when(mockService.exists(any(OperationContext.class), any(Urn.class), eq(true)))
        .thenReturn(true);

    final OrreryTokenAuthenticator authenticator = new OrreryTokenAuthenticator();
    authenticator.init(
        ImmutableMap.of(
            SIGNING_KEY_CONFIG_NAME,
            TEST_SIGNING_KEY,
            SALT_CONFIG_NAME,
            TEST_SALT,
            SIGNING_ALG_CONFIG_NAME,
            "HS256"),
        new AuthenticatorContext(
            ImmutableMap.of(ENTITY_SERVICE, mockService, TOKEN_SERVICE, statefulTokenService)));

    final Actor actor = new Actor(ActorType.USER, "orrery");
    String token =
        authenticator._statefulTokenService.generateAccessToken(
            testOperationContext(),
            TokenType.SESSION,
            actor,
            0L,
            System.currentTimeMillis() - 1,
            "token",
            "token",
            actor.toUrnStr());

    AuthenticationRequest authRequest =
        new AuthenticationRequest(null, null, Map.of(AUTHORIZATION_HEADER_NAME, "Bearer " + token));

    try {
      Authentication authentication = authenticator.authenticate(authRequest);
      fail(
          String.format(
              "Expected exception `AuthenticationExpiredException` was not thrown. %s",
              authentication));
    } catch (AuthenticationExpiredException e) {
      assertEquals(
          e.getMessage(),
          "Failed to authenticate inbound request: Failed to validate Orrery token. Token has expired.");
    } catch (AuthenticationException e) {
      fail("Unexpected exception occurred: " + e.getMessage());
    }
  }
}
