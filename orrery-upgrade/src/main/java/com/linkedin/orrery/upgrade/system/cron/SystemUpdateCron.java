package com.linkedin.orrery.upgrade.system.cron;

import com.linkedin.orrery.upgrade.Upgrade;
import com.linkedin.orrery.upgrade.UpgradeStep;
import java.util.List;

public class SystemUpdateCron implements Upgrade {

  private List<UpgradeStep> cronSystemUpgrades;

  public SystemUpdateCron(List<UpgradeStep> steps) {
    this.cronSystemUpgrades = steps;
  }

  @Override
  public String id() {
    return this.getClass().getSimpleName();
  }

  @Override
  public List<UpgradeStep> steps() {
    return cronSystemUpgrades;
  }
}
