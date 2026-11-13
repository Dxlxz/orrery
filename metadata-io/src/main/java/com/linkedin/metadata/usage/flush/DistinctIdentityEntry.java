package com.linkedin.metadata.usage.flush;

import io.orreryproject.metadata.context.usage.AttributionType;

public record DistinctIdentityEntry(String usageIdentity, AttributionType attributionType) {}
