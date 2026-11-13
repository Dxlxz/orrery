package com.linkedin.metadata.usage.flush;

import io.orreryproject.metadata.context.usage.UsageActorClass;
import java.util.Map;

public record AdditiveUsageRow(
    String metricName, UsageActorClass actorClass, Map<String, String> dimensions, long valueSum) {}
