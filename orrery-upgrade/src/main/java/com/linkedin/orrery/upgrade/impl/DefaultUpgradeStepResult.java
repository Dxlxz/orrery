package com.linkedin.orrery.upgrade.impl;

import com.linkedin.orrery.upgrade.UpgradeStepResult;
import com.linkedin.upgrade.OrreryUpgradeState;

public class DefaultUpgradeStepResult implements UpgradeStepResult {

  private final String _stepId;
  private final OrreryUpgradeState _result;
  private final Action _action;

  public DefaultUpgradeStepResult(String stepId, OrreryUpgradeState result) {
    this(stepId, result, Action.CONTINUE);
  }

  public DefaultUpgradeStepResult(String stepId, OrreryUpgradeState result, Action action) {
    _stepId = stepId;
    _result = result;
    _action = action;
  }

  @Override
  public String stepId() {
    return _stepId;
  }

  @Override
  public OrreryUpgradeState result() {
    return _result;
  }

  @Override
  public Action action() {
    return _action;
  }
}
