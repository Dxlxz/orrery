package orrery.spark.conf;

import orrery.client.kafka.KafkaEmitter;
import orrery.client.kafka.KafkaEmitterConfig;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Setter
@ToString
@Getter
public class KafkaOrreryEmitterConfig implements OrreryEmitterConfig {
  final String type = "kafka";
  KafkaEmitterConfig kafkaEmitterConfig;
  String mcpTopic;

  public KafkaOrreryEmitterConfig(KafkaEmitterConfig kafkaEmitterConfig) {
    this.kafkaEmitterConfig = kafkaEmitterConfig;
    this.mcpTopic = KafkaEmitter.DEFAULT_MCP_KAFKA_TOPIC;
  }

  public KafkaOrreryEmitterConfig(KafkaEmitterConfig kafkaEmitterConfig, String mcpTopic) {
    this.kafkaEmitterConfig = kafkaEmitterConfig;
    this.mcpTopic = mcpTopic;
  }
}
