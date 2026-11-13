package com.linkedin.orrery.graphql.types.assertion;

import com.linkedin.data.template.GetMode;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.FreshnessAssertionInfo;
import com.linkedin.orrery.graphql.generated.FreshnessAssertionSchedule;
import com.linkedin.orrery.graphql.generated.FreshnessAssertionScheduleType;
import com.linkedin.orrery.graphql.generated.FreshnessAssertionType;
import com.linkedin.orrery.graphql.generated.FreshnessCronSchedule;
import com.linkedin.orrery.graphql.types.dataset.mappers.DatasetFilterMapper;
import javax.annotation.Nullable;

public class FreshnessAssertionMapper extends AssertionMapper {

  public static FreshnessAssertionInfo mapFreshnessAssertionInfo(
      @Nullable final QueryContext context,
      final com.linkedin.assertion.FreshnessAssertionInfo gmsFreshnessAssertionInfo) {
    FreshnessAssertionInfo freshnessAssertionInfo = new FreshnessAssertionInfo();
    freshnessAssertionInfo.setEntityUrn(gmsFreshnessAssertionInfo.getEntity().toString());
    freshnessAssertionInfo.setType(
        FreshnessAssertionType.valueOf(gmsFreshnessAssertionInfo.getType().name()));
    if (gmsFreshnessAssertionInfo.hasSchedule()) {
      freshnessAssertionInfo.setSchedule(
          mapFreshnessAssertionSchedule(gmsFreshnessAssertionInfo.getSchedule()));
    }
    if (gmsFreshnessAssertionInfo.hasFilter()) {
      freshnessAssertionInfo.setFilter(
          DatasetFilterMapper.map(context, gmsFreshnessAssertionInfo.getFilter()));
    }
    return freshnessAssertionInfo;
  }

  private static FreshnessCronSchedule mapFreshnessCronSchedule(
      final com.linkedin.assertion.FreshnessCronSchedule gmsCronSchedule) {
    FreshnessCronSchedule cronSchedule = new FreshnessCronSchedule();
    cronSchedule.setCron(gmsCronSchedule.getCron());
    cronSchedule.setTimezone(gmsCronSchedule.getTimezone());
    cronSchedule.setWindowStartOffsetMs(gmsCronSchedule.getWindowStartOffsetMs(GetMode.NULL));
    return cronSchedule;
  }

  private static FreshnessAssertionSchedule mapFreshnessAssertionSchedule(
      final com.linkedin.assertion.FreshnessAssertionSchedule gmsFreshnessAssertionSchedule) {
    FreshnessAssertionSchedule freshnessAssertionSchedule = new FreshnessAssertionSchedule();
    freshnessAssertionSchedule.setType(
        FreshnessAssertionScheduleType.valueOf(gmsFreshnessAssertionSchedule.getType().name()));
    if (gmsFreshnessAssertionSchedule.hasCron()) {
      freshnessAssertionSchedule.setCron(
          mapFreshnessCronSchedule(gmsFreshnessAssertionSchedule.getCron()));
    }
    if (gmsFreshnessAssertionSchedule.hasFixedInterval()) {
      freshnessAssertionSchedule.setFixedInterval(
          mapFixedIntervalSchedule(gmsFreshnessAssertionSchedule.getFixedInterval()));
    }
    return freshnessAssertionSchedule;
  }

  private FreshnessAssertionMapper() {}
}
