package com.linkedin.orrery.graphql.exception;

/** Exception thrown when authentication fails. */
public class AuthorizationException extends OrreryGraphQLException {

  public AuthorizationException(String message) {
    super(message, OrreryGraphQLErrorCode.UNAUTHORIZED);
  }

  public AuthorizationException(String message, Throwable cause) {
    super(message, OrreryGraphQLErrorCode.UNAUTHORIZED, cause);
  }
}
