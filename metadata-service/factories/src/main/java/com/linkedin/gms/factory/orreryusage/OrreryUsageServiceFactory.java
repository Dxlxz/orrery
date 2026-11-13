package com.linkedin.gms.factory.orreryusage;

import com.linkedin.gms.factory.search.SearchClusterRegistry;
import com.linkedin.metadata.config.search.SearchComponent;
import com.linkedin.metadata.orreryusage.OrreryUsageService;
import com.linkedin.metadata.orreryusage.OrreryUsageServiceImpl;
import com.linkedin.metadata.utils.elasticsearch.IndexConvention;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class OrreryUsageServiceFactory {

  @Bean
  public OrreryUsageService orreryUsageService(
      IndexConvention indexConvention, SearchClusterRegistry searchClusterRegistry) {
    return new OrreryUsageServiceImpl(
        searchClusterRegistry.clientFor(SearchComponent.USAGE), indexConvention);
  }
}
