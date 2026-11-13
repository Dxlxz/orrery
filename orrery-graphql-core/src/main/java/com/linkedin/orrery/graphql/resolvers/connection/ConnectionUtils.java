package com.linkedin.orrery.graphql.resolvers.connection;

import com.orrery.authorization.AuthUtil;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.metadata.authorization.PoliciesConfig;
import javax.annotation.Nonnull;

/** Utilities for working with Orrery Connections. */
public class ConnectionUtils {

  /**
   * Returns true if the user is able to read and or write connection between Orrery and external
   * platforms.
   */
  public static boolean canManageConnections(@Nonnull QueryContext context) {
    return AuthUtil.isAuthorized(
        context.getOperationContext(), PoliciesConfig.MANAGE_CONNECTIONS_PRIVILEGE);
  }

  private ConnectionUtils() {}
}
