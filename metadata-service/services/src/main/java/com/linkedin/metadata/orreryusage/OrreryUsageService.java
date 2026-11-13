package com.linkedin.metadata.orreryusage;

import io.orreryproject.metadata.context.OperationContext;
import javax.annotation.Nonnull;

public interface OrreryUsageService {

  String getUsageIndexName(@Nonnull OperationContext opContext);

  ExternalAuditEventsSearchResponse externalAuditEventsSearch(
      OperationContext opContext,
      ExternalAuditEventsSearchRequest externalAuditEventsSearchRequest);
}
