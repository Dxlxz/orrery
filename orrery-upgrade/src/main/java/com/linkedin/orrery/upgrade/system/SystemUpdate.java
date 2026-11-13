package com.linkedin.orrery.upgrade.system;

import com.linkedin.orrery.upgrade.Upgrade;
import com.linkedin.orrery.upgrade.UpgradeCleanupStep;
import com.linkedin.orrery.upgrade.UpgradeStep;
import com.linkedin.orrery.upgrade.kubernetes.ScaleDownEvaluationStep;
import com.linkedin.orrery.upgrade.system.bootstrapmcps.BootstrapMCP;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.Nullable;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

@Getter
@Slf4j
@Accessors(fluent = true)
public class SystemUpdate implements Upgrade {
  private final List<UpgradeStep> steps;
  private final List<UpgradeCleanupStep> cleanupSteps;

  public SystemUpdate(
      @NonNull final List<BlockingSystemUpgrade> blockingSystemUpgrades,
      @NonNull final List<NonBlockingSystemUpgrade> nonBlockingSystemUpgrades,
      @Nullable final BootstrapMCP bootstrapMCPBlocking,
      @Nullable final BootstrapMCP bootstrapMCPNonBlocking) {

    steps = new LinkedList<>();
    cleanupSteps = new LinkedList<>();

    // evaluate whether any blocking upgrade requires K8 scale-down, then run blocking steps
    steps.add(new ScaleDownEvaluationStep(blockingSystemUpgrades));
    steps.addAll(blockingSystemUpgrades.stream().flatMap(up -> up.steps().stream()).toList());
    cleanupSteps.addAll(
        blockingSystemUpgrades.stream().flatMap(up -> up.cleanupSteps().stream()).toList());

    // bootstrap blocking only
    if (bootstrapMCPBlocking != null) {
      steps.addAll(bootstrapMCPBlocking.steps());
      cleanupSteps.addAll(bootstrapMCPBlocking.cleanupSteps());
    }

    // bootstrap non-blocking only
    if (bootstrapMCPNonBlocking != null) {
      steps.addAll(bootstrapMCPNonBlocking.steps());
      cleanupSteps.addAll(bootstrapMCPNonBlocking.cleanupSteps());
    }

    // add non-blocking upgrades last
    steps.addAll(nonBlockingSystemUpgrades.stream().flatMap(up -> up.steps().stream()).toList());
    cleanupSteps.addAll(
        nonBlockingSystemUpgrades.stream().flatMap(up -> up.cleanupSteps().stream()).toList());
  }

  @Override
  public String id() {
    return getClass().getSimpleName();
  }
}
