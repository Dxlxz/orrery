package io.orreryproject.openlineage.dataset;

import com.linkedin.common.urn.DatasetUrn;
import com.linkedin.dataset.UpstreamLineage;
import com.linkedin.schema.SchemaMetadata;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Builder
@Setter
@ToString
public class OrreryDataset {
  DatasetUrn urn;
  SchemaMetadata schemaMetadata;
  UpstreamLineage lineage;
}
