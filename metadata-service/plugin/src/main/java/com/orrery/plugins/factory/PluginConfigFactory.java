package com.orrery.plugins.factory;

import com.orrery.plugins.auth.provider.AuthenticatorPluginConfigProvider;
import com.orrery.plugins.auth.provider.AuthorizerPluginConfigProvider;
import com.orrery.plugins.common.PluginConfig;
import com.orrery.plugins.common.PluginConfigProvider;
import com.orrery.plugins.common.PluginType;
import com.orrery.plugins.configuration.Config;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

/** Create instance of config provider as per type mentioned in {@link Config} */
public class PluginConfigFactory {
  private static final Map<PluginType, PluginConfigProvider> CONFIG_PROVIDER_REGISTRY;

  static {
    CONFIG_PROVIDER_REGISTRY = new HashMap<>(2);
    CONFIG_PROVIDER_REGISTRY.put(PluginType.AUTHENTICATOR, new AuthenticatorPluginConfigProvider());
    CONFIG_PROVIDER_REGISTRY.put(PluginType.AUTHORIZER, new AuthorizerPluginConfigProvider());
  }

  private final Config _config;

  public PluginConfigFactory(@Nonnull Config config) {
    this._config = config;
  }

  @Nonnull
  public List<PluginConfig> loadPluginConfigs(@Nonnull PluginType pluginType) {
    return CONFIG_PROVIDER_REGISTRY.get(pluginType).processConfig(this._config.getPlugins());
  }
}
