package com.linkedin.orrery.upgrade.kubernetes;

import com.linkedin.orrery.upgrade.UpgradeContext;
import com.linkedin.orrery.upgrade.UpgradeStep;
import com.linkedin.orrery.upgrade.UpgradeStepResult;
import com.linkedin.orrery.upgrade.impl.DefaultUpgradeStepResult;
import com.linkedin.orrery.upgrade.system.BlockingSystemUpgrade;
import com.linkedin.upgrade.OrreryUpgradeState;
import java.util.List;
import java.util.function.Function;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Runs before KubernetesScaleDownStep; asks each blocking upgrade (except KubernetesScaleDown)
 * whether K8 scale-down is required. Sets context.scaleDownRequired if any vote true so the
 * scale-down step can skip when not needed. No saved state—each job/step manages its own.
 */
@Slf4j
@RequiredArgsConstructor
public class ScaleDownEvaluationStep implements UpgradeStep {

  private static final String STEP_ID = "ScaleDownEvaluationStep";
  private static final String KUBERNETES_SCALE_DOWN_ID = "KubernetesScaleDown";

  private final List<BlockingSystemUpgrade> blockingUpgrades;

  @Override
  public String id() {
    return STEP_ID;
  }

  @Override
  public Function<UpgradeContext, UpgradeStepResult> executable() {
    return context -> {
      for (BlockingSystemUpgrade upgrade : blockingUpgrades) {
        if (KUBERNETES_SCALE_DOWN_ID.equals(upgrade.id())) {
          continue;
        }
        try {
          if (upgrade.requiresK8ScaleDown(context)) {
            log.info("Blocking upgrade {} voted that K8 scale-down is required", upgrade.id());
            context.setScaleDownRequired(true);
            return new DefaultUpgradeStepResult(id(), OrreryUpgradeState.SUCCEEDED);
          }
        } catch (Exception e) {
          log.warn(
              "Blocking upgrade {} failed during scale-down evaluation: {}",
              upgrade.id(),
              e.getMessage());
        }
      }
      context.setScaleDownRequired(false);
      log.debug("No blocking upgrade required K8 scale-down");
      return new DefaultUpgradeStepResult(id(), OrreryUpgradeState.SUCCEEDED);
    };
  }
}
