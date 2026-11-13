package com.linkedin.gms.factory.auth;

import com.orrery.authentication.group.GroupService;
import com.orrery.authorization.OrreryAuthorizer;
import com.linkedin.entity.client.SystemEntityClient;
import io.orreryproject.metadata.context.OperationContext;
import javax.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

@Configuration
public class OrreryAuthorizerFactory {

  @Value("${authorization.defaultAuthorizer.cacheRefreshIntervalSecs}")
  private Integer policyCacheRefreshIntervalSeconds;

  @Value("${authorization.defaultAuthorizer.cachePolicyFetchSize}")
  private Integer policyCacheFetchSize;

  @Value("${authorization.defaultAuthorizer.enabled:true}")
  private Boolean policiesEnabled;

  @Bean(name = "orreryAuthorizer")
  @Scope("singleton")
  @Nonnull
  protected OrreryAuthorizer orreryAuthorizer(
      @Qualifier("systemOperationContext") final OperationContext systemOpContext,
      final SystemEntityClient systemEntityClient,
      @Qualifier("groupService") final GroupService groupService) {

    final OrreryAuthorizer.AuthorizationMode mode =
        policiesEnabled
            ? OrreryAuthorizer.AuthorizationMode.DEFAULT
            : OrreryAuthorizer.AuthorizationMode.ALLOW_ALL;

    return new OrreryAuthorizer(
        systemOpContext,
        systemEntityClient,
        groupService,
        10,
        policyCacheRefreshIntervalSeconds,
        mode,
        policyCacheFetchSize);
  }
}
