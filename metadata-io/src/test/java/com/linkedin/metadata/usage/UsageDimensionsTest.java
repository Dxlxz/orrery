package com.linkedin.metadata.usage;

import io.orreryproject.metadata.context.RequestContext;
import io.orreryproject.metadata.context.usage.AttributionType;
import io.orreryproject.metadata.context.usage.AuthChannel;
import java.util.List;
import java.util.Map;
import org.testng.Assert;
import org.testng.annotations.Test;

public class UsageDimensionsTest {

  @Test
  public void testDimensionConstantValues() {
    Assert.assertEquals(UsageDimensions.USAGE_OPERATION, "usage_operation");
    Assert.assertEquals(UsageDimensions.REQUEST_API, "request_api");
    Assert.assertEquals(UsageDimensions.AGENT_CLASS, "agent_class");
    Assert.assertEquals(UsageDimensions.AGENT_NAME, "agent_name");
    Assert.assertEquals(UsageDimensions.AUTH_CHANNEL, "auth_channel");
    Assert.assertEquals(UsageDimensions.INGESTION_RUNNER, "ingestion_runner");
    Assert.assertEquals(UsageDimensions.ACTOR_CLASS, "actor_class");
    Assert.assertEquals(UsageDimensions.ENTITY_TYPE, "entity_type");
    Assert.assertEquals(UsageDimensions.PLATFORM, "platform");
  }

  @Test
  public void testStableKeyOrder() {
    Assert.assertEquals(
        UsageDimensions.STABLE_KEY_ORDER,
        List.of(
            UsageDimensions.USAGE_OPERATION,
            UsageDimensions.REQUEST_API,
            UsageDimensions.AGENT_CLASS,
            UsageDimensions.AGENT_NAME,
            UsageDimensions.AUTH_CHANNEL,
            UsageDimensions.INGESTION_RUNNER,
            UsageDimensions.ACTOR_CLASS,
            UsageDimensions.ENTITY_TYPE,
            UsageDimensions.PLATFORM));
  }

  @Test
  public void testFromRequestContextIncludesOptionalDimensions() {
    RequestContext requestContext =
        RequestContext.builder()
            .actorUrn("urn:li:corpuser:test")
            .sourceIP("127.0.0.1")
            .requestAPI(RequestContext.RequestAPI.OPENAPI)
            .requestID("test")
            .userAgent("Mozilla/5.0")
            .authChannel(AuthChannel.SESSION)
            .build();

    Map<String, String> dimensions =
        UsageDimensions.fromRequestContext(requestContext, "metadata_read", "regular");

    Assert.assertEquals(dimensions.get(UsageDimensions.USAGE_OPERATION), "metadata_read");
    Assert.assertEquals(dimensions.get(UsageDimensions.REQUEST_API), "openapi");
    Assert.assertEquals(dimensions.get(UsageDimensions.AGENT_CLASS), "browser");
    Assert.assertEquals(dimensions.get(UsageDimensions.AUTH_CHANNEL), "session");
    Assert.assertEquals(dimensions.get(UsageDimensions.ACTOR_CLASS), "regular");
    Assert.assertFalse(dimensions.containsKey(UsageDimensions.INGESTION_RUNNER));
  }

  @Test
  public void testFromRequestContextOmitsNullOptionalsAndDefaultsAuthChannel() {
    RequestContext requestContext =
        RequestContext.builder()
            .actorUrn("urn:li:corpuser:test")
            .sourceIP("127.0.0.1")
            .requestAPI(RequestContext.RequestAPI.GRAPHQL)
            .requestID("test")
            .userAgent("orrery-cli/1.0")
            .build();

    Map<String, String> dimensions = UsageDimensions.fromRequestContext(requestContext, null, null);

    Assert.assertFalse(dimensions.containsKey(UsageDimensions.USAGE_OPERATION));
    Assert.assertFalse(dimensions.containsKey(UsageDimensions.ACTOR_CLASS));
    Assert.assertEquals(dimensions.get(UsageDimensions.REQUEST_API), "graphql");
    Assert.assertEquals(dimensions.get(UsageDimensions.AUTH_CHANNEL), "unknown");
    Assert.assertNotNull(dimensions.get(UsageDimensions.AGENT_CLASS));
  }

  @Test
  public void testFromRequestContextIncludesNormalizedAgentNameWhenEnabled() {
    RequestContext requestContext =
        RequestContext.builder()
            .actorUrn("urn:li:corpuser:test")
            .sourceIP("127.0.0.1")
            .requestAPI(RequestContext.RequestAPI.OPENAPI)
            .requestID("test")
            .userAgent("Orrery-Client/1.0.0 (sdk; Orrery/Custom-Agent; 1.0.1)")
            .build();

    Map<String, String> dimensions =
        UsageDimensions.fromRequestContext(requestContext, "metadata_read", "regular", true);

    Assert.assertEquals(dimensions.get(UsageDimensions.AGENT_NAME), "orrery/custom-agent");
  }

  @Test
  public void testResolveAttribution() {
    RequestContext requestContext =
        RequestContext.builder()
            .actorUrn("urn:li:corpuser:test")
            .sourceIP("127.0.0.1")
            .requestAPI(RequestContext.RequestAPI.OPENAPI)
            .requestID("test")
            .userAgent("Mozilla/5.0")
            .build();

    Assert.assertEquals(UsageDimensions.resolveAttribution(requestContext), AttributionType.HUMAN);
  }
}
