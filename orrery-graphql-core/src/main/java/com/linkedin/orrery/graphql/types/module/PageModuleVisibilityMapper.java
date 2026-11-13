package com.linkedin.orrery.graphql.types.module;

import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.PageModuleScope;
import com.linkedin.orrery.graphql.types.mappers.ModelMapper;
import com.linkedin.module.OrreryPageModuleVisibility;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PageModuleVisibilityMapper
    implements ModelMapper<
        OrreryPageModuleVisibility,
        com.linkedin.orrery.graphql.generated.OrreryPageModuleVisibility> {

  public static final PageModuleVisibilityMapper INSTANCE = new PageModuleVisibilityMapper();

  public static com.linkedin.orrery.graphql.generated.OrreryPageModuleVisibility map(
      @Nonnull final OrreryPageModuleVisibility visibility) {
    return INSTANCE.apply(null, visibility);
  }

  @Override
  public com.linkedin.orrery.graphql.generated.OrreryPageModuleVisibility apply(
      @Nullable final QueryContext context, @Nonnull final OrreryPageModuleVisibility visibility) {
    final com.linkedin.orrery.graphql.generated.OrreryPageModuleVisibility result =
        new com.linkedin.orrery.graphql.generated.OrreryPageModuleVisibility();

    if (visibility.hasScope()) {
      result.setScope(PageModuleScope.valueOf(visibility.getScope().toString()));
    }

    return result;
  }
}
