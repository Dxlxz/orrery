package com.orrery.plugins.auth;

import com.orrery.plugins.common.PluginType;
import com.orrery.plugins.configuration.Config;
import com.orrery.plugins.configuration.PluginConfig;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.testng.annotations.Test;

@Test
public class TestConfig {
  @Test
  public void testConfig() {
    PluginConfig authorizerConfig = new PluginConfig();
    authorizerConfig.setName("apache-ranger-authorizer");
    authorizerConfig.setType(PluginType.AUTHORIZER);
    authorizerConfig.setParams(
        Map.of(
            "className",
            "com.orrery.authorization.ranger.RangerAuthorizer",
            "configs",
            Map.of("username", "foo", "password", "root123")));

    PluginConfig authenticatorConfig = new PluginConfig();
    authorizerConfig.setName("sample-authenticator");
    authorizerConfig.setType(PluginType.AUTHENTICATOR);
    authorizerConfig.setParams(Map.of("className", "com.orrery.plugins.test.TestAuthenticator"));

    List<PluginConfig> plugins = Arrays.asList(authorizerConfig, authenticatorConfig);

    assert Config.builder().plugins(plugins).build() != null;
  }
}
