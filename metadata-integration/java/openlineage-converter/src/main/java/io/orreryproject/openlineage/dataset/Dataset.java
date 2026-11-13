package io.orreryproject.openlineage.dataset;

import com.linkedin.common.FabricType;
import com.linkedin.common.urn.DatasetUrn;
import io.orreryproject.openlineage.config.OrreryOpenlineageConfig;
import io.orreryproject.openlineage.utils.OrreryUtils;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public abstract class Dataset {

  private final DatasetUrn urn;

  public Dataset(String platform, String platformInstance, String name, FabricType fabricType) {
    super();
    this.urn = OrreryUtils.createDatasetUrn(platform, platformInstance, name, fabricType);
  }

  public Dataset(String platform, String name, OrreryOpenlineageConfig orreryConfig) {
    super();
    this.urn =
        OrreryUtils.createDatasetUrn(
            platform, orreryConfig.getPlatformInstance(), name, orreryConfig.getFabricType());
  }

  public DatasetUrn urn() {
    return urn;
  }
}
