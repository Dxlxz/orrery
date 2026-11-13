package com.orrery.authentication.token;

/** Represents a type of JWT access token granted by the Orrery token service. */
public enum TokenType {

  /** A UI-initiated session token */
  SESSION,
  /** A personal token for programmatic use */
  PERSONAL,
  /** A service account token for programmatic use */
  SERVICE_ACCOUNT;
}
