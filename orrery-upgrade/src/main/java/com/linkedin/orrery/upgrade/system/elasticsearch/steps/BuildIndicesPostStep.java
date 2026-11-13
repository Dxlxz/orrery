package com.linkedin.orrery.upgrade.system.elasticsearch.steps;

import static com.linkedin.orrery.upgrade.system.elasticsearch.util.IndexUtils.INDEX_BLOCKS_WRITE_SETTING;
import static com.linkedin.orrery.upgrade.system.elasticsearch.util.IndexUtils.getAllReindexConfigs;

import com.google.common.collect.ImmutableMap;
import com.linkedin.common.urn.Urn;
import com.linkedin.orrery.upgrade.UpgradeContext;
import com.linkedin.orrery.upgrade.UpgradeStep;
import com.linkedin.orrery.upgrade.UpgradeStepResult;
import com.linkedin.orrery.upgrade.impl.DefaultUpgradeStepResult;
import com.linkedin.orrery.upgrade.system.elasticsearch.util.IndexUtils;
import com.linkedin.gms.factory.search.BaseElasticSearchComponentsFactory;
import com.linkedin.metadata.search.elasticsearch.indexbuilder.ReindexConfig;
import com.linkedin.metadata.shared.ElasticSearchIndexed;
import com.linkedin.metadata.utils.elasticsearch.SearchClientShim;
import com.linkedin.structured.StructuredPropertyDefinition;
import com.linkedin.upgrade.OrreryUpgradeState;
import com.linkedin.util.Pair;
import io.orreryproject.metadata.context.OperationContext;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.opensearch.action.admin.indices.settings.put.UpdateSettingsRequest;
import org.opensearch.client.RequestOptions;

@RequiredArgsConstructor
@Slf4j
public class BuildIndicesPostStep implements UpgradeStep {

  private final BaseElasticSearchComponentsFactory.BaseElasticSearchComponents esComponents;
  private final List<ElasticSearchIndexed> services;
  private final Supplier<Set<Pair<Urn, StructuredPropertyDefinition>>> structuredProperties;

  @Override
  public String id() {
    return "BuildIndicesPostStep";
  }

  @Override
  public int retryCount() {
    return 3;
  }

  @Override
  public Function<UpgradeContext, UpgradeStepResult> executable() {
    return (context) -> {
      OperationContext opContext = context.opContext();
      try {

        List<ReindexConfig> indexConfigs =
            getAllReindexConfigs(opContext, services, structuredProperties).stream()
                .filter(ReindexConfig::requiresReindex)
                .collect(Collectors.toList());

        // Reset write blocking
        for (ReindexConfig indexConfig : indexConfigs) {
          SearchClientShim<?> searchClient =
              IndexUtils.requireIndexBuilder(indexConfig.name()).getSearchClient();
          UpdateSettingsRequest request = new UpdateSettingsRequest(indexConfig.name());
          Map<String, Object> indexSettings = ImmutableMap.of(INDEX_BLOCKS_WRITE_SETTING, "false");

          request.settings(indexSettings);
          boolean ack =
              searchClient
                  .updateIndexSettings(opContext, request, RequestOptions.DEFAULT)
                  .isAcknowledged();
          log.info(
              "Updated index {} with new settings. Settings: {}, Acknowledged: {}",
              indexConfig.name(),
              indexSettings,
              ack);

          if (ack) {
            ack = IndexUtils.validateWriteBlock(opContext, searchClient, indexConfig.name(), false);
            log.info(
                "Validated index {} with new settings. Settings: {}, Acknowledged: {}",
                indexConfig.name(),
                indexSettings,
                ack);
          }

          if (!ack) {
            log.error(
                "Partial index settings update, some indices may still be blocking writes."
                    + " Please fix the error and rerun the BuildIndices upgrade job.");
            return new DefaultUpgradeStepResult(id(), OrreryUpgradeState.FAILED);
          }
        }
      } catch (Exception e) {
        log.error("BuildIndicesPostStep failed.", e);
        return new DefaultUpgradeStepResult(id(), OrreryUpgradeState.FAILED);
      }
      return new DefaultUpgradeStepResult(id(), OrreryUpgradeState.SUCCEEDED);
    };
  }
}
