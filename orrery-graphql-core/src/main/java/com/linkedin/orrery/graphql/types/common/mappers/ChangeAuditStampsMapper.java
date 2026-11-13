package com.linkedin.orrery.graphql.types.common.mappers;

import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.ChangeAuditStamps;
import com.linkedin.orrery.graphql.types.mappers.ModelMapper;
import javax.annotation.Nullable;

public class ChangeAuditStampsMapper
    implements ModelMapper<com.linkedin.common.ChangeAuditStamps, ChangeAuditStamps> {
  public static final ChangeAuditStampsMapper INSTANCE = new ChangeAuditStampsMapper();

  public static ChangeAuditStamps map(
      @Nullable QueryContext context, com.linkedin.common.ChangeAuditStamps input) {
    return INSTANCE.apply(context, input);
  }

  @Override
  public ChangeAuditStamps apply(
      @Nullable QueryContext context, com.linkedin.common.ChangeAuditStamps input) {
    ChangeAuditStamps changeAuditStamps = new ChangeAuditStamps();
    changeAuditStamps.setCreated(AuditStampMapper.map(context, input.getCreated()));
    changeAuditStamps.setLastModified(AuditStampMapper.map(context, input.getLastModified()));
    if (input.hasDeleted()) {
      changeAuditStamps.setDeleted(AuditStampMapper.map(context, input.getDeleted()));
    }

    return changeAuditStamps;
  }
}
