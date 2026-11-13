package com.linkedin.orrery.upgrade.impl;

import com.linkedin.orrery.upgrade.UpgradeReport;
import com.linkedin.orrery.upgrade.UpgradeResult;
import com.linkedin.upgrade.OrreryUpgradeState;

public class DefaultUpgradeResult implements UpgradeResult {

  private final OrreryUpgradeState _result;
  private final UpgradeReport _report;

  DefaultUpgradeResult(OrreryUpgradeState result, UpgradeReport report) {
    _result = result;
    _report = report;
  }

  @Override
  public OrreryUpgradeState result() {
    return _result;
  }

  @Override
  public UpgradeReport report() {
    return _report;
  }
}
