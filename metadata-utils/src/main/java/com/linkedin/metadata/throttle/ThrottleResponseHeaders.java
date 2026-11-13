package com.linkedin.metadata.throttle;

/** Standard HTTP response headers for Orrery throttling and rate limiting. */
public final class ThrottleResponseHeaders {
  public static final String RULE = "X-Orrery-RateLimit-Rule";
  public static final String TYPE = "X-Orrery-RateLimit-Type";
  public static final String SOURCE = "X-Orrery-RateLimit-Source";
  public static final String ENDPOINT_RULE = "X-Orrery-RateLimit-Endpoint-Rule";
  public static final String RETRY_AFTER = "Retry-After";

  private ThrottleResponseHeaders() {}
}
