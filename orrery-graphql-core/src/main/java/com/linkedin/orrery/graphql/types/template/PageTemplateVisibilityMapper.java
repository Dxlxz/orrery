package com.linkedin.orrery.graphql.types.template;

import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.PageTemplateScope;
import com.linkedin.orrery.graphql.types.mappers.ModelMapper;
import com.linkedin.template.OrreryPageTemplateVisibility;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PageTemplateVisibilityMapper
    implements ModelMapper<
        OrreryPageTemplateVisibility,
        com.linkedin.orrery.graphql.generated.OrreryPageTemplateVisibility> {

  public static final PageTemplateVisibilityMapper INSTANCE = new PageTemplateVisibilityMapper();

  public static com.linkedin.orrery.graphql.generated.OrreryPageTemplateVisibility map(
      @Nonnull final OrreryPageTemplateVisibility visibility) {
    return INSTANCE.apply(null, visibility);
  }

  @Override
  public com.linkedin.orrery.graphql.generated.OrreryPageTemplateVisibility apply(
      @Nullable final QueryContext context,
      @Nonnull final OrreryPageTemplateVisibility visibility) {
    final com.linkedin.orrery.graphql.generated.OrreryPageTemplateVisibility result =
        new com.linkedin.orrery.graphql.generated.OrreryPageTemplateVisibility();

    if (visibility.hasScope()) {
      result.setScope(PageTemplateScope.valueOf(visibility.getScope().toString()));
    }

    return result;
  }
}
