package com.linkedin.orrery.upgrade.system.policyfields;

import com.google.common.collect.ImmutableList;
import com.linkedin.orrery.upgrade.UpgradeStep;
import com.linkedin.orrery.upgrade.system.NonBlockingSystemUpgrade;
import com.linkedin.metadata.entity.EntityService;
import com.linkedin.metadata.search.SearchService;
import io.orreryproject.metadata.context.OperationContext;
import java.util.List;

public class BackfillPolicyFields implements NonBlockingSystemUpgrade {
  private final List<UpgradeStep> _steps;

  public BackfillPolicyFields(
      OperationContext opContext,
      EntityService<?> entityService,
      SearchService searchService,
      boolean enabled,
      boolean reprocessEnabled,
      Integer batchSize) {
    if (enabled) {
      _steps =
          ImmutableList.of(
              new BackfillPolicyFieldsStep(
                  opContext, entityService, searchService, reprocessEnabled, batchSize));
    } else {
      _steps = ImmutableList.of();
    }
  }

  @Override
  public String id() {
    return "BackfillPolicyFields";
  }

  @Override
  public List<UpgradeStep> steps() {
    return _steps;
  }
}
