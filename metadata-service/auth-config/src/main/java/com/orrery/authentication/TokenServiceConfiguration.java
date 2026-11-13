package com.orrery.authentication;

import lombok.Data;

@Data
/** Configurations for Orrery token service */
public class TokenServiceConfiguration {
  private String signingKey;
  private String salt;
  private String issuer;
  private String signingAlgorithm;
}
