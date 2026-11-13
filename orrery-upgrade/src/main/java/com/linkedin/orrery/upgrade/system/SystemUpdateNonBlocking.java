package com.linkedin.orrery.upgrade.system;

import com.linkedin.orrery.upgrade.system.bootstrapmcps.BootstrapMCP;
import java.util.List;
import lombok.NonNull;

public class SystemUpdateNonBlocking extends SystemUpdate {

  public SystemUpdateNonBlocking(
      @NonNull List<NonBlockingSystemUpgrade> nonBlockingSystemUpgrades,
      final BootstrapMCP bootstrapMCPNonBlocking) {
    super(List.of(), nonBlockingSystemUpgrades, null, bootstrapMCPNonBlocking);
  }
}
