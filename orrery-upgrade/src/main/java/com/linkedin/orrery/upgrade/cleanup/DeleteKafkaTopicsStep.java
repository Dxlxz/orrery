package com.linkedin.orrery.upgrade.cleanup;

import static com.linkedin.gms.factory.kafka.common.AdminClientFactory.buildKafkaAdminClient;

import com.linkedin.orrery.upgrade.UpgradeContext;
import com.linkedin.orrery.upgrade.UpgradeStep;
import com.linkedin.orrery.upgrade.UpgradeStepResult;
import com.linkedin.orrery.upgrade.impl.DefaultUpgradeStepResult;
import com.linkedin.metadata.config.kafka.KafkaConfiguration;
import com.linkedin.metadata.config.kafka.TopicsConfiguration;
import com.linkedin.upgrade.OrreryUpgradeState;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.AdminClient;
import org.springframework.boot.kafka.autoconfigure.KafkaProperties;

/** Deletes all Kafka topics that Orrery created during setup. */
@Slf4j
public class DeleteKafkaTopicsStep implements UpgradeStep {

  private final KafkaConfiguration kafkaConfiguration;
  private final KafkaProperties kafkaProperties;

  public DeleteKafkaTopicsStep(
      KafkaConfiguration kafkaConfiguration, KafkaProperties kafkaProperties) {
    this.kafkaConfiguration = kafkaConfiguration;
    this.kafkaProperties = kafkaProperties;
  }

  @Override
  public String id() {
    return "DeleteKafkaTopicsStep";
  }

  @Override
  public int retryCount() {
    return 2;
  }

  @Override
  public Function<UpgradeContext, UpgradeStepResult> executable() {
    return (context) -> {
      TopicsConfiguration topicsConfig = kafkaConfiguration.getTopics();
      if (topicsConfig == null
          || topicsConfig.getTopics() == null
          || topicsConfig.getTopics().isEmpty()) {
        log.info("No topics configured — nothing to delete");
        return new DefaultUpgradeStepResult(id(), OrreryUpgradeState.SUCCEEDED);
      }

      List<String> topicNames = new ArrayList<>();
      for (Map.Entry<String, TopicsConfiguration.TopicConfiguration> entry :
          topicsConfig.getTopics().entrySet()) {
        TopicsConfiguration.TopicConfiguration topicConfig = entry.getValue();
        if (topicConfig.getEnabled()) {
          topicNames.add(topicConfig.getName());
        }
      }

      if (topicNames.isEmpty()) {
        log.info("All configured topics are disabled — nothing to delete");
        return new DefaultUpgradeStepResult(id(), OrreryUpgradeState.SUCCEEDED);
      }

      log.info("Deleting {} Kafka topics: {}", topicNames.size(), topicNames);

      try (AdminClient adminClient = createAdminClient()) {
        // 60 s matches Kafka's own default.api.timeout.ms — topic deletion is a metadata-only
        // operation so this is ample even on large clusters.
        adminClient.deleteTopics(topicNames).all().get(60, TimeUnit.SECONDS);
        log.info("Successfully deleted {} Kafka topics", topicNames.size());
        return new DefaultUpgradeStepResult(id(), OrreryUpgradeState.SUCCEEDED);
      } catch (Exception e) {
        log.error("Failed to delete Kafka topics: {}", e.getMessage(), e);
        return new DefaultUpgradeStepResult(id(), OrreryUpgradeState.FAILED);
      }
    };
  }

  protected AdminClient createAdminClient() {
    return buildKafkaAdminClient(kafkaConfiguration, kafkaProperties, "orrery-cleanup-kafka");
  }
}
