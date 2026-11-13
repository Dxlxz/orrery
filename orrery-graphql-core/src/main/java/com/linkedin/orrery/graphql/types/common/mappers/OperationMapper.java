package com.linkedin.orrery.graphql.types.common.mappers;

import com.linkedin.common.Operation;
import com.linkedin.common.urn.Urn;
import com.linkedin.data.template.GetMode;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.OperationSourceType;
import com.linkedin.orrery.graphql.generated.OperationType;
import com.linkedin.orrery.graphql.types.mappers.TimeSeriesAspectMapper;
import com.linkedin.metadata.aspect.EnvelopedAspect;
import com.linkedin.metadata.utils.GenericRecordUtils;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class OperationMapper
    implements TimeSeriesAspectMapper<com.linkedin.orrery.graphql.generated.Operation> {

  public static final OperationMapper INSTANCE = new OperationMapper();

  public static com.linkedin.orrery.graphql.generated.Operation map(
      @Nullable QueryContext context, @Nonnull final EnvelopedAspect envelopedAspect) {
    return INSTANCE.apply(context, envelopedAspect);
  }

  @Override
  public com.linkedin.orrery.graphql.generated.Operation apply(
      @Nullable QueryContext context, @Nonnull final EnvelopedAspect envelopedAspect) {

    Operation gmsProfile =
        GenericRecordUtils.deserializeAspect(
            envelopedAspect.getAspect().getValue(),
            envelopedAspect.getAspect().getContentType(),
            Operation.class);

    final com.linkedin.orrery.graphql.generated.Operation result =
        new com.linkedin.orrery.graphql.generated.Operation();

    result.setTimestampMillis(gmsProfile.getTimestampMillis());
    result.setLastUpdatedTimestamp(gmsProfile.getLastUpdatedTimestamp());
    if (gmsProfile.hasActor()) {
      result.setActor(gmsProfile.getActor().toString());
    }
    result.setOperationType(
        OperationType.valueOf(OperationType.class, gmsProfile.getOperationType().toString()));
    result.setCustomOperationType(gmsProfile.getCustomOperationType(GetMode.NULL));
    if (gmsProfile.hasSourceType()) {
      result.setSourceType(OperationSourceType.valueOf(gmsProfile.getSourceType().toString()));
    }
    if (gmsProfile.hasPartitionSpec()) {
      result.setPartition(gmsProfile.getPartitionSpec().getPartition(GetMode.NULL));
    }
    if (gmsProfile.hasCustomProperties()) {
      result.setCustomProperties(StringMapMapper.map(context, gmsProfile.getCustomProperties()));
    }
    if (gmsProfile.hasNumAffectedRows()) {
      result.setNumAffectedRows(gmsProfile.getNumAffectedRows());
    }
    if (gmsProfile.hasAffectedDatasets()) {
      result.setAffectedDatasets(
          gmsProfile.getAffectedDatasets().stream()
              .map(Urn::toString)
              .collect(Collectors.toList()));
    }

    return result;
  }
}
