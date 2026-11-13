package io.orreryproject.openapi.test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.orrery.authorization.AuthorizationRequest;
import com.orrery.authorization.AuthorizationResult;
import com.orrery.authorization.AuthorizerChain;
import io.orreryproject.metadata.context.OperationContext;
import java.util.Map;

/** Shared stubs for mocked {@link AuthorizerChain} beans in openapi-servlet integration tests. */
public final class AuthorizerChainTestSupport {

  private static final AuthorizationResult ALLOW =
      new AuthorizationResult(null, AuthorizationResult.Type.ALLOW, "");

  private AuthorizerChainTestSupport() {}

  /**
   * Stubs only the 1-arg {@link AuthorizerChain#authorize(AuthorizationRequest)} path. Exercises
   * {@link io.orreryproject.metadata.context.AuthorizationContext}'s fallback when a mock {@link
   * io.orreryproject.metadata.context.OperationContextAuthorizer} returns null.
   */
  public static void stubAllowViaOneArgOnly(AuthorizerChain authorizerChain) {
    when(authorizerChain.authorize(any(AuthorizationRequest.class))).thenReturn(ALLOW);
  }

  /**
   * Stubs the session {@link io.orreryproject.metadata.context.OperationContextAuthorizer} 3-arg
   * path used by {@link io.orreryproject.metadata.context.AuthorizationContext}.
   */
  public static void stubAllowViaOperationContextAuthorizer(AuthorizerChain authorizerChain) {
    when(authorizerChain.authorize(
            any(AuthorizationRequest.class), any(Map.class), any(OperationContext.class)))
        .thenReturn(ALLOW);
  }

  /** Stubs both authorization entry points. */
  public static void stubAllowAllPaths(AuthorizerChain authorizerChain) {
    stubAllowViaOneArgOnly(authorizerChain);
    stubAllowViaOperationContextAuthorizer(authorizerChain);
  }
}
