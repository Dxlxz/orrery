package com.linkedin.metadata.usage.instrumentation;

import com.orrery.authentication.Actor;
import com.orrery.authentication.ActorType;
import com.orrery.authentication.Authentication;
import com.orrery.authentication.token.TokenClaimNames;
import com.linkedin.metadata.Constants;
import io.orreryproject.metadata.context.usage.AuthChannel;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

public class UsageAuthChannelResolverTest {

  private static final String USER = "urn:li:corpuser:orrery";

  @Test
  public void testBasicCredentialsMapToSession() {
    Authentication authentication =
        new Authentication(new Actor(ActorType.USER, "orrery"), "Basic dXNlcjpwYXNz");

    Assert.assertEquals(
        UsageAuthChannelResolver.resolve(authentication, USER), AuthChannel.SESSION);
  }

  @Test
  public void testPersonalAccessTokenMapsToPat() {
    Authentication authentication =
        new Authentication(
            new Actor(ActorType.USER, "orrery"),
            "Bearer token",
            Map.of(TokenClaimNames.TOKEN_TYPE, "PERSONAL"));

    Assert.assertEquals(UsageAuthChannelResolver.resolve(authentication, USER), AuthChannel.PAT);
  }

  @Test
  public void testServiceAccountTokenMapsToPat() {
    Authentication authentication =
        new Authentication(
            new Actor(ActorType.USER, "ingestion"),
            "Bearer token",
            Map.of(TokenClaimNames.TOKEN_TYPE, "SERVICE_ACCOUNT"));

    Assert.assertEquals(UsageAuthChannelResolver.resolve(authentication, USER), AuthChannel.PAT);
  }

  @Test
  public void testBearerSessionTokenMapsToSession() {
    Authentication authentication =
        new Authentication(
            new Actor(ActorType.USER, "orrery"),
            "Bearer token",
            Map.of(TokenClaimNames.TOKEN_TYPE, "SESSION"));

    Assert.assertEquals(
        UsageAuthChannelResolver.resolve(authentication, USER), AuthChannel.SESSION);
  }

  @Test
  public void testOAuth2AccessTokenMapsToOAuth() {
    Authentication authentication =
        new Authentication(
            new Actor(ActorType.USER, "orrery"),
            "Bearer token",
            Map.of(TokenClaimNames.TOKEN_TYPE, "OAUTH2_ACCESS"));

    Assert.assertEquals(UsageAuthChannelResolver.resolve(authentication, USER), AuthChannel.OAUTH);
  }

  @Test
  public void testExternalBearerWithoutClaimsMapsToOAuth() {
    Authentication authentication =
        new Authentication(new Actor(ActorType.USER, "orrery"), "Bearer external-jwt");

    Assert.assertEquals(UsageAuthChannelResolver.resolve(authentication, USER), AuthChannel.OAUTH);
  }

  @Test
  public void testSystemAndAnonymousActors() {
    Authentication authentication =
        new Authentication(new Actor(ActorType.USER, "orrery"), "Bearer token");

    Assert.assertEquals(
        UsageAuthChannelResolver.resolve(authentication, Constants.SYSTEM_ACTOR),
        AuthChannel.SYSTEM);
    Assert.assertEquals(
        UsageAuthChannelResolver.resolve(authentication, Constants.ANONYMOUS_ACTOR),
        AuthChannel.ANONYMOUS);
  }

  @Test
  public void testMissingCredentialsMapsToUnknown() {
    Authentication authentication = new Authentication(new Actor(ActorType.USER, "orrery"), "");

    Assert.assertEquals(
        UsageAuthChannelResolver.resolve(authentication, USER), AuthChannel.UNKNOWN);
  }
}
