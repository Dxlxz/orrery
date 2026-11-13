package io.orreryproject.test.metadata.context;

import com.orrery.authentication.Authentication;
import com.orrery.authorization.AuthorizationRequest;
import com.orrery.authorization.AuthorizationResult;
import com.orrery.authorization.AuthorizationSession;
import com.orrery.authorization.EntitySpec;
import com.orrery.plugins.auth.authorization.Authorizer;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.BiFunction;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class TestAuthSession implements AuthorizationSession {
  public static AuthorizationSession ALLOW_ALL =
      from((priv, authorizer) -> new AuthorizationResult(null, AuthorizationResult.Type.ALLOW, ""));

  public static AuthorizationSession from(Authentication auth, Authorizer authorizer) {
    return from(
        (privilege, resourceSpec) -> {
          final AuthorizationRequest request =
              new AuthorizationRequest(
                  auth.getActor().toUrnStr(),
                  privilege,
                  Optional.ofNullable(resourceSpec),
                  Collections.emptyList());
          return authorizer.authorize(request);
        });
  }

  public static AuthorizationSession from(
      BiFunction<String, EntitySpec, AuthorizationResult> authFunction) {
    return new TestAuthSession(authFunction);
  }

  private final BiFunction<String, EntitySpec, AuthorizationResult> authFunction;

  public TestAuthSession(BiFunction<String, EntitySpec, AuthorizationResult> authFunction) {
    this.authFunction = authFunction;
  }

  @Override
  public AuthorizationResult authorize(
      @Nonnull String privilege, @Nullable EntitySpec resourceSpec) {
    return authFunction.apply(privilege, resourceSpec);
  }

  @Override
  public AuthorizationResult authorize(
      @Nonnull String privilege,
      @Nullable EntitySpec resourceSpec,
      @Nonnull Collection<EntitySpec> subResources) {
    return authorize(privilege, resourceSpec);
  }
}
