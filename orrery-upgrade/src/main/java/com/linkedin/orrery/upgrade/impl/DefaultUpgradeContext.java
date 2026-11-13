package com.linkedin.orrery.upgrade.impl;

import com.linkedin.orrery.upgrade.Upgrade;
import com.linkedin.orrery.upgrade.UpgradeContext;
import com.linkedin.orrery.upgrade.UpgradeReport;
import com.linkedin.orrery.upgrade.UpgradeStepResult;
import com.linkedin.metadata.utils.EnvironmentUtils;
import io.orreryproject.metadata.context.OperationContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import javax.annotation.Nonnull;
import lombok.Getter;
import lombok.experimental.Accessors;

@Getter
@Accessors(fluent = true)
public class DefaultUpgradeContext implements UpgradeContext {

  private final OperationContext opContext;
  private final Upgrade upgrade;
  private final UpgradeReport report;
  private final List<UpgradeStepResult> previousStepResults;
  private final List<String> args;
  private final Map<String, Optional<String>> parsedArgs;

  private boolean scaleDownRequired;

  public DefaultUpgradeContext(
      @Nonnull OperationContext opContext,
      Upgrade upgrade,
      UpgradeReport report,
      List<UpgradeStepResult> previousStepResults,
      List<String> args) {
    this.opContext = opContext;
    this.upgrade = upgrade;
    this.report = report;
    this.previousStepResults = previousStepResults;
    this.args = args;
    this.parsedArgs = EnvironmentUtils.parseArgs(args);
  }

  @Override
  public List<UpgradeStepResult> stepResults() {
    return previousStepResults;
  }

  @Override
  public boolean isScaleDownRequired() {
    return scaleDownRequired;
  }

  @Override
  public void setScaleDownRequired(boolean scaleDownRequired) {
    this.scaleDownRequired = scaleDownRequired;
  }
}
