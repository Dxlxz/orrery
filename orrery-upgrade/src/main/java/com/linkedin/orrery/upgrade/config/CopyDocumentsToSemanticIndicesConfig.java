package com.linkedin.orrery.upgrade.config;

import com.linkedin.orrery.upgrade.conditions.SystemUpdateCondition;
import com.linkedin.orrery.upgrade.system.BlockingSystemUpgrade;
import com.linkedin.orrery.upgrade.system.semanticsearch.CopyDocumentsToSemanticIndices;
import com.linkedin.gms.factory.config.ConfigurationProvider;
import com.linkedin.metadata.entity.EntityService;
import com.linkedin.metadata.utils.elasticsearch.IndexConvention;
import io.orreryproject.metadata.context.OperationContext;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;

@Configuration
@Conditional(SystemUpdateCondition.BlockingSystemUpdateCondition.class)
public class CopyDocumentsToSemanticIndicesConfig {

  @Order(3) // After BuildIndices (order 2)
  @Bean(name = "copyDocumentsToSemanticIndices")
  public BlockingSystemUpgrade copyDocumentsToSemanticIndices(
      @Qualifier("systemOperationContext") final OperationContext opContext,
      final EntityService<?> entityService,
      final ConfigurationProvider configurationProvider,
      final IndexConvention indexConvention,
      @Value("${elasticsearch.entityIndex.semanticSearch.enabled:false}") boolean enabled) {

    return new CopyDocumentsToSemanticIndices(
        opContext,
        entityService,
        configurationProvider.getElasticSearch().getEntityIndex().getSemanticSearch(),
        indexConvention,
        enabled);
  }
}
