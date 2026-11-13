package com.linkedin.orrery.graphql.types.dataset.mappers;

import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.DatasetFilter;
import com.linkedin.orrery.graphql.generated.DatasetFilterType;
import com.linkedin.orrery.graphql.types.mappers.ModelMapper;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DatasetFilterMapper
    implements ModelMapper<com.linkedin.dataset.DatasetFilter, DatasetFilter> {

  public static final DatasetFilterMapper INSTANCE = new DatasetFilterMapper();

  public static DatasetFilter map(
      @Nullable QueryContext context, @Nonnull final com.linkedin.dataset.DatasetFilter metadata) {
    return INSTANCE.apply(context, metadata);
  }

  @Override
  public DatasetFilter apply(
      @Nullable QueryContext context, @Nonnull final com.linkedin.dataset.DatasetFilter input) {
    final DatasetFilter result = new DatasetFilter();
    result.setType(DatasetFilterType.valueOf(input.getType().name()));
    result.setSql(input.getSql());
    return result;
  }
}
