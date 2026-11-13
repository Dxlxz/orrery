package com.linkedin.metadata.entity;

import com.linkedin.mxe.MetadataChangeLog;
import io.orreryproject.metadata.context.OperationContext;
import java.util.Collection;
import javax.annotation.Nonnull;

public interface SearchIndicesService {
  void handleChangeEvent(
      @Nonnull OperationContext opContext, @Nonnull MetadataChangeLog metadataChangeLog);

  void handleChangeEvents(
      @Nonnull OperationContext opContext, @Nonnull Collection<MetadataChangeLog> events);
}
