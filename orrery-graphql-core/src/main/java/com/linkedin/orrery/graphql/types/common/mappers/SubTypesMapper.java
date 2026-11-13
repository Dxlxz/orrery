package com.linkedin.orrery.graphql.types.common.mappers;

import com.linkedin.common.SubTypes;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.types.mappers.ModelMapper;
import java.util.ArrayList;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class SubTypesMapper
    implements ModelMapper<SubTypes, com.linkedin.orrery.graphql.generated.SubTypes> {

  public static final SubTypesMapper INSTANCE = new SubTypesMapper();

  public static com.linkedin.orrery.graphql.generated.SubTypes map(
      @Nullable QueryContext context, @Nonnull final SubTypes metadata) {
    return INSTANCE.apply(context, metadata);
  }

  @Override
  public com.linkedin.orrery.graphql.generated.SubTypes apply(
      @Nullable QueryContext context, @Nonnull final SubTypes input) {
    final com.linkedin.orrery.graphql.generated.SubTypes result =
        new com.linkedin.orrery.graphql.generated.SubTypes();
    result.setTypeNames(new ArrayList<>(input.getTypeNames()));
    return result;
  }
}
