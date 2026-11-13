package com.linkedin.orrery.graphql.types.module;

import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.types.mappers.ModelMapper;
import com.linkedin.module.OrreryPageModuleType;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PageModuleTypeMapper
    implements ModelMapper<
        OrreryPageModuleType, com.linkedin.orrery.graphql.generated.OrreryPageModuleType> {

  public static final PageModuleTypeMapper INSTANCE = new PageModuleTypeMapper();

  public static com.linkedin.orrery.graphql.generated.OrreryPageModuleType map(
      @Nonnull final OrreryPageModuleType type) {
    return INSTANCE.apply(null, type);
  }

  @Override
  public com.linkedin.orrery.graphql.generated.OrreryPageModuleType apply(
      @Nullable final QueryContext context, @Nonnull final OrreryPageModuleType type) {
    try {
      return com.linkedin.orrery.graphql.generated.OrreryPageModuleType.valueOf(type.toString());
    } catch (IllegalArgumentException e) {
      log.warn("Encountered unexpected Orrery Page Module type", e);
      return com.linkedin.orrery.graphql.generated.OrreryPageModuleType.UNKNOWN;
    }
  }
}
