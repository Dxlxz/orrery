package orrery.spark.conf;

import orrery.client.rest.RestEmitterConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@ToString
@Getter
public class RestOrreryEmitterConfig implements OrreryEmitterConfig {
  final String type = "rest";
  orrery.client.rest.RestEmitterConfig restEmitterConfig;

  public RestOrreryEmitterConfig(RestEmitterConfig restEmitterConfig) {
    this.restEmitterConfig = restEmitterConfig;
  }
}
