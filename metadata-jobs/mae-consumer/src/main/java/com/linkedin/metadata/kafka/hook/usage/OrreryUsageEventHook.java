package com.linkedin.metadata.kafka.hook.usage;

import com.fasterxml.jackson.databind.JsonNode;
import com.linkedin.metadata.kafka.listener.EventHook;
import io.orreryproject.metadata.context.OperationContext;
import javax.annotation.Nonnull;

public interface OrreryUsageEventHook extends EventHook<JsonNode> {

  default OrreryUsageEventHook init(@Nonnull OperationContext systemOperationContext) {
    return this;
  }

  /** Invoke the hook when a OrreryUsageEvent is received */
  void invoke(@Nonnull OperationContext operationContext, @Nonnull JsonNode event) throws Exception;

  /**
   * Controls hook execution ordering
   *
   * @return order to execute
   */
  default int executionOrder() {
    return 100;
  }
}
