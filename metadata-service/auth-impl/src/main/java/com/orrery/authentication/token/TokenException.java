package com.orrery.authentication.token;

/** A checked exception that is thrown when a Orrery-issued access token cannot be verified. */
public class TokenException extends Exception {

  public TokenException(final String message) {
    super(message);
  }

  public TokenException(final String message, final Throwable e) {
    super(message, e);
  }
}
