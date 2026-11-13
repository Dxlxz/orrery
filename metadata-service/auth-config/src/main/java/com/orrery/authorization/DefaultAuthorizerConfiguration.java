package com.orrery.authorization;

import lombok.Data;

@Data
public class DefaultAuthorizerConfiguration {
  /** Whether authorization via Orrery policies is enabled. */
  private boolean enabled;

  /** The duration between policies cache refreshes. */
  private int cacheRefreshIntervalSecs;
}
