package com.linkedin.orrery.upgrade;

import com.linkedin.upgrade.OrreryUpgradeState;

/** Represents the result of executing an {@link Upgrade} */
public interface UpgradeResult {

  /** Returns the {@link OrreryUpgradeState} of executing an {@link Upgrade} */
  OrreryUpgradeState result();

  /** Returns the {@link UpgradeReport} associated with the completed {@link Upgrade}. */
  UpgradeReport report();
}
