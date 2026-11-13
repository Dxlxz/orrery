package com.linkedin.orrery.graphql.types.mlmodel.mappers;

import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.BooleanBox;
import com.linkedin.orrery.graphql.generated.FloatBox;
import com.linkedin.orrery.graphql.generated.HyperParameterValueType;
import com.linkedin.orrery.graphql.generated.IntBox;
import com.linkedin.orrery.graphql.generated.StringBox;
import com.linkedin.orrery.graphql.types.mappers.ModelMapper;
import javax.annotation.Nullable;
import lombok.NonNull;

public class HyperParameterValueTypeMapper
    implements ModelMapper<
        com.linkedin.ml.metadata.HyperParameterValueType, HyperParameterValueType> {

  public static final HyperParameterValueTypeMapper INSTANCE = new HyperParameterValueTypeMapper();

  public static HyperParameterValueType map(
      @Nullable QueryContext context,
      @NonNull final com.linkedin.ml.metadata.HyperParameterValueType input) {
    return INSTANCE.apply(context, input);
  }

  @Override
  public HyperParameterValueType apply(
      @Nullable QueryContext context,
      @NonNull final com.linkedin.ml.metadata.HyperParameterValueType input) {
    HyperParameterValueType result = null;

    if (input.isString()) {
      result = new StringBox(input.getString());
    } else if (input.isBoolean()) {
      result = new BooleanBox(input.getBoolean());
    } else if (input.isInt()) {
      result = new IntBox(input.getInt());
    } else if (input.isDouble()) {
      result = new FloatBox(input.getDouble());
    } else if (input.isFloat()) {
      result = new FloatBox(Double.valueOf(input.getFloat()));
    } else {
      throw new RuntimeException("Type is not one of the Union Types, Type: " + input.toString());
    }
    return result;
  }
}
