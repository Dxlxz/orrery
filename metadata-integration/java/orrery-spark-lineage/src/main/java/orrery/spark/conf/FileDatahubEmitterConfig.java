package orrery.spark.conf;

import orrery.client.file.FileEmitterConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@ToString
@Getter
public class FileOrreryEmitterConfig implements OrreryEmitterConfig {
  final String type = "file";
  FileEmitterConfig fileEmitterConfig;

  public FileOrreryEmitterConfig(FileEmitterConfig fileEmitterConfig) {
    this.fileEmitterConfig = fileEmitterConfig;
  }
}
