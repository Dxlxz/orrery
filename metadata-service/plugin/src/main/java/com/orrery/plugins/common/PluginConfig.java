package com.orrery.plugins.common;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Flat form of plugin configuration configured in config.yaml at plugins[] and plugins[].params */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PluginConfig {
  /** Type of plugin. Supported types are {@link PluginType} */
  private PluginType type;

  /** name of the plugin. It should be unique in plugins[] list */
  private String name;

  /**
   * Whether to load the plugin in GMS. If set to true plugin will be loaded in GMS take
   * authentication/authorization decisions.
   */
  private Boolean enabled;

  /** Fully-qualified class-name of plugin */
  private String className;

  /**
   * It is always set to <plugin-base-directory>/<plugin-name>. For example if plugin-name is
   * ranger-authorizer and plugin-base-directory is /etc/orrery/plugins/auth then pluginDirectory
   * would be /etc/orrery/plugins/auth/ranger-authorizer
   */
  private Path pluginHomeDirectory;

  /**
   * Default jarFileName is "<plugin-name>.jar". If plugin's jar file name is different from default
   * value then set this property.
   */
  private Path pluginJarPath;

  /**
   * These configs are specific to plugin. GMS pass this map as is to plugin {@link
   * com.orrery.plugins.auth.authentication.Authenticator} or {@link
   * com.orrery.plugins.auth.authorization.Authorizer} init method
   */
  private Optional<Map<String, Object>> configs;
}
