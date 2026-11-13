package orrery.spark.conf;

import orrery.client.s3.S3EmitterConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@ToString
@Getter
public class S3OrreryEmitterConfig implements OrreryEmitterConfig {
  final String type = "s3";
  S3EmitterConfig s3EmitterConfig;

  public S3OrreryEmitterConfig(S3EmitterConfig s3EmitterConfig) {
    this.s3EmitterConfig = s3EmitterConfig;
  }
}
