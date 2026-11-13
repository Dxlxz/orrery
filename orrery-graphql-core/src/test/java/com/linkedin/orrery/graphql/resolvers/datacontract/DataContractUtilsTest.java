package com.linkedin.orrery.graphql.resolvers.datacontract;

import static org.mockito.Mockito.mock;

import com.orrery.authentication.Actor;
import com.orrery.authentication.ActorType;
import com.orrery.authentication.Authentication;
import com.orrery.authorization.AuthorizationRequest;
import com.orrery.authorization.AuthorizationResult;
import com.orrery.authorization.EntitySpec;
import com.orrery.plugins.auth.authorization.Authorizer;
import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.metadata.config.OrreryAppConfiguration;
import graphql.Assert;
import io.orreryproject.metadata.context.OperationContext;
import io.orreryproject.test.metadata.context.TestOperationContexts;
import java.util.Collections;
import java.util.Optional;
import org.mockito.Mockito;
import org.testng.annotations.Test;

public class DataContractUtilsTest {

  @Test
  public void testCanEditDataContract() {
    Urn testUrn = UrnUtils.getUrn("urn:li:dataContract:test");
    boolean result =
        DataContractUtils.canEditDataContract(
            new QueryContext() {
              @Override
              public boolean isAuthenticated() {
                return true;
              }

              @Override
              public Authentication getAuthentication() {
                Authentication auth = new Authentication(new Actor(ActorType.USER, "test"), "TEST");
                return auth;
              }

              @Override
              public Authorizer getAuthorizer() {
                Authorizer authorizer = mock(Authorizer.class);
                Mockito.when(authorizer.authorize(Mockito.any(AuthorizationRequest.class)))
                    .thenReturn(
                        new AuthorizationResult(
                            new AuthorizationRequest(
                                "TEST",
                                "test",
                                Optional.of(new EntitySpec("dataset", "test")),
                                Collections.emptyList()),
                            AuthorizationResult.Type.ALLOW,
                            "TEST"));
                return authorizer;
              }

              @Override
              public OperationContext getOperationContext() {
                return TestOperationContexts.userContextNoSearchAuthorization(
                    getAuthorizer(), getAuthentication());
              }

              @Override
              public OrreryAppConfiguration getOrreryAppConfig() {
                return new OrreryAppConfiguration();
              }

              @Override
              public int getMaxParentDepth() {
                return 50;
              }
            },
            testUrn);
    Assert.assertTrue(result);
  }
}
