package com.linkedin.orrery.graphql.types.usage;

import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.UsageAggregationMetrics;
import com.linkedin.orrery.graphql.types.mappers.ModelMapper;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class UsageAggregationMetricsMapper
    implements ModelMapper<com.linkedin.usage.UsageAggregationMetrics, UsageAggregationMetrics> {

  public static final UsageAggregationMetricsMapper INSTANCE = new UsageAggregationMetricsMapper();

  public static UsageAggregationMetrics map(
      @Nullable QueryContext context,
      @Nonnull final com.linkedin.usage.UsageAggregationMetrics usageAggregationMetrics) {
    return INSTANCE.apply(context, usageAggregationMetrics);
  }

  @Override
  public UsageAggregationMetrics apply(
      @Nullable QueryContext context,
      @Nonnull final com.linkedin.usage.UsageAggregationMetrics usageAggregationMetrics) {
    UsageAggregationMetrics result = new UsageAggregationMetrics();
    result.setTotalSqlQueries(usageAggregationMetrics.getTotalSqlQueries());
    result.setUniqueUserCount(usageAggregationMetrics.getUniqueUserCount());
    result.setTopSqlQueries(usageAggregationMetrics.getTopSqlQueries());
    if (usageAggregationMetrics.hasFields()) {
      result.setFields(
          usageAggregationMetrics.getFields().stream()
              .map(f -> FieldUsageCountsMapper.map(context, f))
              .collect(Collectors.toList()));
    }
    if (usageAggregationMetrics.hasUsers()) {
      result.setUsers(
          usageAggregationMetrics.getUsers().stream()
              .map(aggregation -> UserUsageCountsMapper.map(context, aggregation))
              .collect(Collectors.toList()));
    }

    return result;
  }
}
