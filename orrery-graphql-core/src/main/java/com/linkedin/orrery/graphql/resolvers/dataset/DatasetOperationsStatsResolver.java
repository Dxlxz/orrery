package com.linkedin.orrery.graphql.resolvers.dataset;

import static com.linkedin.orrery.graphql.resolvers.ResolverUtils.bindArgument;

import com.linkedin.common.WindowDuration;
import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.authorization.AuthorizationUtils;
import com.linkedin.orrery.graphql.concurrency.GraphQLConcurrencyUtils;
import com.linkedin.orrery.graphql.generated.Entity;
import com.linkedin.orrery.graphql.generated.OperationsQueryResult;
import com.linkedin.orrery.graphql.generated.OperationsStatsInput;
import com.linkedin.orrery.graphql.types.operations.OperationsQueryResultMapper;
import com.linkedin.metadata.timeseries.TimeseriesAspectService;
import com.linkedin.metadata.timeseries.elastic.OperationsServiceUtil;
import com.linkedin.usage.UsageTimeRange;
import graphql.schema.DataFetcher;
import graphql.schema.DataFetchingEnvironment;
import io.orreryproject.metadata.context.OperationContext;
import java.util.concurrent.CompletableFuture;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DatasetOperationsStatsResolver
    implements DataFetcher<CompletableFuture<OperationsQueryResult>> {

  private static final UsageTimeRange DEFAULT_RANGE = UsageTimeRange.MONTH;

  private final TimeseriesAspectService timeseriesAspectService;

  public DatasetOperationsStatsResolver(final TimeseriesAspectService timeseriesAspectService) {
    this.timeseriesAspectService = timeseriesAspectService;
  }

  @Override
  public CompletableFuture<OperationsQueryResult> get(DataFetchingEnvironment environment)
      throws Exception {
    final QueryContext context = environment.getContext();
    final Urn resourceUrn = UrnUtils.getUrn(((Entity) environment.getSource()).getUrn());
    final OperationsStatsInput input =
        bindArgument(environment.getArgument("input"), OperationsStatsInput.class);
    final UsageTimeRange range =
        input.getRange() != null
            ? UsageTimeRange.valueOf(input.getRange().toString())
            : DEFAULT_RANGE;
    final String timeZone = input.getTimeZone();
    final OperationContext opContext = context.getOperationContext();

    return GraphQLConcurrencyUtils.supplyAsync(
        () -> {
          try {
            if (!AuthorizationUtils.isViewDatasetOperationsAuthorized(context, resourceUrn)) {
              log.debug(
                  "User {} is not authorized to view operation information for dataset {}",
                  context.getActorUrn(),
                  resourceUrn);
              // don't cause loading the whole dataset to fail
              return new OperationsQueryResult();
            }
            com.linkedin.operations.OperationsQueryResult result =
                OperationsServiceUtil.queryRange(
                    opContext,
                    timeseriesAspectService,
                    resourceUrn.toString(),
                    WindowDuration.DAY,
                    range,
                    timeZone);
            return OperationsQueryResultMapper.map(context, result);
          } catch (Exception e) {
            log.error(
                String.format("Failed to load Operations Stats for resource %s", resourceUrn), e);
            opContext
                .getMetricUtils()
                .ifPresent(
                    metricUtils ->
                        metricUtils.increment(this.getClass(), "operations_stats_dropped", 1));
          }
          // don't cause loading the whole dataset to fail
          return new OperationsQueryResult();
        },
        this.getClass().getSimpleName(),
        "get");
  }
}
