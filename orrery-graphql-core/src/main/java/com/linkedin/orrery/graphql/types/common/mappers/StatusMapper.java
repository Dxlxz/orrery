package com.linkedin.orrery.graphql.types.common.mappers;

import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.LifecycleStageType;
import com.linkedin.orrery.graphql.generated.Status;
import com.linkedin.orrery.graphql.types.mappers.ModelMapper;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class StatusMapper implements ModelMapper<com.linkedin.common.Status, Status> {

  public static final StatusMapper INSTANCE = new StatusMapper();

  public static Status map(
      @Nullable QueryContext context, @Nonnull final com.linkedin.common.Status metadata) {
    return INSTANCE.apply(context, metadata);
  }

  @Override
  public Status apply(
      @Nullable QueryContext context, @Nonnull final com.linkedin.common.Status input) {
    final Status result = new Status();
    result.setRemoved(input.isRemoved());
    if (input.hasLifecycleStage()) {
      LifecycleStageType skeleton = new LifecycleStageType();
      skeleton.setUrn(input.getLifecycleStage().toString());
      result.setLifecycleStage(skeleton);
    }
    if (input.hasLifecycleLastUpdated()) {
      result.setLifecycleLastUpdated(
          AuditStampMapper.map(context, input.getLifecycleLastUpdated()));
    }
    return result;
  }
}
