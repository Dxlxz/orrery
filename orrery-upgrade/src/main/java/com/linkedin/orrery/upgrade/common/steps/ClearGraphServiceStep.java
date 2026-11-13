package com.linkedin.orrery.upgrade.common.steps;

import static com.linkedin.orrery.upgrade.common.Constants.CLEAN_ARG_NAME;

import com.linkedin.orrery.upgrade.UpgradeContext;
import com.linkedin.orrery.upgrade.UpgradeStep;
import com.linkedin.orrery.upgrade.UpgradeStepResult;
import com.linkedin.orrery.upgrade.impl.DefaultUpgradeStepResult;
import com.linkedin.metadata.graph.GraphService;
import com.linkedin.upgrade.OrreryUpgradeState;
import java.util.function.Function;

public class ClearGraphServiceStep implements UpgradeStep {

  private final String deletePattern = ".*";

  private final GraphService _graphService;
  private final boolean _alwaysRun;

  public ClearGraphServiceStep(final GraphService graphService, final boolean alwaysRun) {
    _graphService = graphService;
    _alwaysRun = alwaysRun;
  }

  @Override
  public String id() {
    return "ClearGraphServiceStep";
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
        _graphService.clear(context.opContext());
      } catch (Exception e) {
        context.report().addLine("Failed to clear graph indices", e);
        return new DefaultUpgradeStepResult(id(), OrreryUpgradeState.FAILED);
      }
      return new DefaultUpgradeStepResult(id(), OrreryUpgradeState.SUCCEEDED);
    };
  }
}
