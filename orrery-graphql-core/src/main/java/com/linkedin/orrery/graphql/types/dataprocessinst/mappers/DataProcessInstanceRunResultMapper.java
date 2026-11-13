package com.linkedin.orrery.graphql.types.dataprocessinst.mappers;

import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.DataProcessInstanceRunResultType;
import com.linkedin.orrery.graphql.types.mappers.ModelMapper;
import com.linkedin.dataprocess.DataProcessInstanceRunResult;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DataProcessInstanceRunResultMapper
    implements ModelMapper<
        DataProcessInstanceRunResult,
        com.linkedin.orrery.graphql.generated.DataProcessInstanceRunResult> {

  public static final DataProcessInstanceRunResultMapper INSTANCE =
      new DataProcessInstanceRunResultMapper();

  public static com.linkedin.orrery.graphql.generated.DataProcessInstanceRunResult map(
      @Nullable QueryContext context, @Nonnull final DataProcessInstanceRunResult input) {
    return INSTANCE.apply(context, input);
  }

  @Override
  public com.linkedin.orrery.graphql.generated.DataProcessInstanceRunResult apply(
      @Nullable QueryContext context, @Nonnull final DataProcessInstanceRunResult input) {

    final com.linkedin.orrery.graphql.generated.DataProcessInstanceRunResult result =
        new com.linkedin.orrery.graphql.generated.DataProcessInstanceRunResult();

    if (input.hasType()) {
      result.setResultType(DataProcessInstanceRunResultType.valueOf(input.getType().toString()));
    }

    if (input.hasNativeResultType()) {
      result.setNativeResultType(input.getNativeResultType());
    }

    return result;
  }
}
