package com.orrery.authentication.authenticator;

import static com.orrery.authentication.AuthenticationConstants.*;

import com.orrery.authentication.Actor;
import com.orrery.authentication.Authentication;
import com.orrery.authentication.AuthenticationConstants;
import com.orrery.authentication.AuthenticationException;
import com.orrery.authentication.AuthenticationExpiredException;
import com.orrery.authentication.AuthenticationRequest;
import com.orrery.authentication.AuthenticationUnavailableException;
import com.orrery.authentication.AuthenticatorContext;
import com.orrery.authentication.token.StatefulTokenService;
import com.orrery.authentication.token.StatelessTokenService;
import com.orrery.authentication.token.TokenClaims;
import com.orrery.authentication.token.TokenExpiredException;
import com.orrery.authentication.token.TokenStoreUnavailableException;
import com.orrery.plugins.auth.authentication.Authenticator;
import com.linkedin.metadata.entity.EntityService;
import java.util.Map;
import java.util.Objects;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;

/**
 * Authenticator that verifies Orrery-issued JSON web tokens.
 *
 * <p>This authenticator requires the following configurations:
 *
 * <p>- signingAlgorithm (optional): the algorithm used to verify JWT's. This should be THE SAME ONE
 * used by the {@link StatelessTokenService}. Defaults to HS256. - signingKey: a key used to sign
 * all JWT tokens using the provided signingAlgorithm
 */
@Slf4j
public class OrreryTokenAuthenticator implements Authenticator {

  // Package-Visible for testing.
  StatefulTokenService _statefulTokenService;

  @Override
  public void init(@Nonnull final Map<String, Object> config, final AuthenticatorContext context) {
    Objects.requireNonNull(context, "Context parameter cannot be null");
    log.debug("Initializing OrreryTokenAuthenticator");
    if (!context.data().containsKey(AuthenticationConstants.ENTITY_SERVICE)) {
      throw new IllegalArgumentException(
          "Unable to initialize OrreryTokenAuthenticator, entity service reference not"
              + " found.");
    }
    final Object entityService = context.data().get(ENTITY_SERVICE);
    if (!(entityService instanceof EntityService)) {
      throw new RuntimeException(
          "Unable to initialize OrreryTokenAuthenticator, entity service reference is not of type: "
              + "EntityService.class, found: "
              + entityService.getClass());
    }
    this._statefulTokenService =
        (StatefulTokenService) Objects.requireNonNull(context.data().get(TOKEN_SERVICE));
  }

  @Override
  public Authentication authenticate(@Nonnull AuthenticationRequest context)
      throws AuthenticationException {
    Objects.requireNonNull(context);
    final String authorizationHeader =
        context.getRequestHeaders().get(AUTHORIZATION_HEADER_NAME); // Case insensitive
    if (authorizationHeader != null) {
      if (authorizationHeader.startsWith("Bearer ") || authorizationHeader.startsWith("bearer ")) {
        return validateAndExtract(authorizationHeader);
      } else {
        throw new AuthenticationException("Authorization header missing 'Bearer' prefix.");
      }
    }
    throw new AuthenticationException("Request is missing 'Authorization' header.");
  }

  private Authentication validateAndExtract(final String credentials)
      throws AuthenticationException {
    log.debug("Found authentication token. Verifying...");
    final String token = credentials.substring(7);
    try {
      final TokenClaims claims = this._statefulTokenService.validateAccessToken(token);
      return new Authentication(
          new Actor(claims.getActorType(), claims.getActorId()), credentials, claims.asMap());
    } catch (TokenExpiredException e) {
      throw new AuthenticationExpiredException(e.getMessage(), e);
    } catch (TokenStoreUnavailableException e) {
      // The token may be valid; the store that records revocations could not be read.
      throw new AuthenticationUnavailableException(e.getMessage(), e);
    } catch (Exception e) {
      // Failed to validate the token
      throw new AuthenticationException("Unable to verify the provided token.", e);
    }
  }
}
