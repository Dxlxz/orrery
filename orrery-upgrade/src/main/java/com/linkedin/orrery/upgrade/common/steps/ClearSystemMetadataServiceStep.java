package com.linkedin.orrery.upgrade.common.steps;

import static com.linkedin.orrery.upgrade.common.Constants.CLEAN_ARG_NAME;

import com.linkedin.orrery.upgrade.UpgradeContext;
import com.linkedin.orrery.upgrade.UpgradeStep;
import com.linkedin.orrery.upgrade.UpgradeStepResult;
import com.linkedin.orrery.upgrade.impl.DefaultUpgradeStepResult;
import com.linkedin.metadata.systemmetadata.SystemMetadataService;
import com.linkedin.upgrade.OrreryUpgradeState;
import java.util.function.Function;

public class ClearSystemMetadataServiceStep implements UpgradeStep {

  private final SystemMetadataService _systemMetadataService;
  private final boolean _alwaysRun;

  public ClearSystemMetadataServiceStep(
      final SystemMetadataService systemMetadataService, final boolean alwaysRun) {
    _systemMetadataService = systemMetadataService;
    _alwaysRun = alwaysRun;
  }

  @Override
  public String id() {
    return "ClearSystemMetadataServiceStep";
  }

  @Override
  public boolean skip(UpgradeContext context) {
    if (_alwaysRun) {
      return false;
    }
    if (context.parsedArgs().containsKey(CLEAN_ARG_NAME)) {
      return false;
    }
    context.report().addLine("Cleanup has not been requested.");
    return true;
  }

  @Override
  public int retryCount() {
    return 1;
  }

  @Override
  public Function<UpgradeContext, UpgradeStepResult> executable() {
    return (context) -> {
      try {
        _systemMetadataService.clear(context.opContext());
      } catch (Exception e) {
        context.report().addLine("Failed to clear system metadata service", e);
        return new DefaultUpgradeStepResult(id(), OrreryUpgradeState.FAILED);
      }
      return new DefaultUpgradeStepResult(id(), OrreryUpgradeState.SUCCEEDED);
    };
  }
}
