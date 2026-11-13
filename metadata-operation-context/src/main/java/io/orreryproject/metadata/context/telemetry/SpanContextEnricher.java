package io.orreryproject.metadata.context.telemetry;

import io.orreryproject.metadata.context.OperationContext;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import javax.annotation.Nonnull;

public interface SpanContextEnricher {
  void enrich(@Nonnull ReadWriteSpan span, @Nonnull OperationContext operationContext);
}
