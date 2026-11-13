package com.linkedin.orrery.graphql.types.mappers;

import com.linkedin.orrery.graphql.generated.TimeSeriesAspect;
import com.linkedin.metadata.aspect.EnvelopedAspect;

public interface TimeSeriesAspectMapper<T extends TimeSeriesAspect>
    extends ModelMapper<EnvelopedAspect, T> {}
