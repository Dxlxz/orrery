package com.orrery.plugins.common;

import java.util.List;

public interface PluginConfigProvider<T extends PluginConfig> {
  List<T> processConfig(List<com.orrery.plugins.configuration.PluginConfig> pluginConfigConfigs);
}
