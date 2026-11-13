package com.linkedin.orrery.graphql.exception;

import graphql.GraphQLException;

public class OrreryGraphQLException extends GraphQLException {

  private final OrreryGraphQLErrorCode code;

  public OrreryGraphQLException(String message, OrreryGraphQLErrorCode code) {
    super(message);
    this.code = code;
  }

  public OrreryGraphQLException(String message, OrreryGraphQLErrorCode code, Throwable cause) {
    super(message, cause);
    this.code = code;
  }

  public OrreryGraphQLErrorCode errorCode() {
    return this.code;
  }
}
