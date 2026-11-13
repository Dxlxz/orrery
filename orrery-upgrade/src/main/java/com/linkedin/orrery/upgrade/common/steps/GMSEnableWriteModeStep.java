package com.linkedin.orrery.upgrade.common.steps;

import com.linkedin.orrery.upgrade.UpgradeContext;
import com.linkedin.orrery.upgrade.UpgradeStep;
import com.linkedin.orrery.upgrade.UpgradeStepResult;
import com.linkedin.orrery.upgrade.impl.DefaultUpgradeStepResult;
import com.linkedin.entity.client.SystemEntityClient;
import com.linkedin.upgrade.OrreryUpgradeState;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequiredArgsConstructor
public class GMSEnableWriteModeStep implements UpgradeStep {
  private final SystemEntityClient systemEntityClient;

  @Override
  public String id() {
    return "GMSEnableWriteModeStep";
  }

  @Override
  public int retryCount() {
    return 2;
  }

  @Override
  public Function<UpgradeContext, UpgradeStepResult> executable() {
    return (context) -> {
      try {
        systemEntityClient.setWritable(context.opContext(), true);
      } catch (Exception e) {
        log.error("Failed to turn write mode back on in GMS", e);
        context.report().addLine("Failed to turn write mode back on in GMS");
        return new DefaultUpgradeStepResult(id(), OrreryUpgradeState.FAILED);
      }
      return new DefaultUpgradeStepResult(id(), OrreryUpgradeState.SUCCEEDED);
    };
  }
}
