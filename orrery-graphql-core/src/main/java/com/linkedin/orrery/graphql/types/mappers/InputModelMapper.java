package com.linkedin.orrery.graphql.types.mappers;

import com.linkedin.orrery.graphql.QueryContext;
import javax.annotation.Nullable;

/** Maps an input of type I to an output of type O with actor context. */
public interface InputModelMapper<I, O, A> {
  O apply(@Nullable final QueryContext context, final I input, final A actor);
}
