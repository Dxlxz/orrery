package com.linkedin.orrery.graphql.exception;

public enum OrreryGraphQLErrorCode {
  BAD_REQUEST(400),
  UNAUTHORIZED(403),
  NOT_FOUND(404),
  CONFLICT(409),
  /** Transient failure; clients may retry (aligns with HTTP 503). */
  SERVICE_UNAVAILABLE(503),
  /**
   * Server-side deadline/timeout exceeded; the operation was too expensive to finish (HTTP 504).
   */
  DEADLINE_EXCEEDED(504),
  SERVER_ERROR(500);

  private final int _code;

  public int getCode() {
    return _code;
  }

  OrreryGraphQLErrorCode(final int code) {
    _code = code;
  }
}
