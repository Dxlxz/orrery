package com.linkedin.orrery.graphql.types.template;

import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.PageTemplateSurfaceType;
import com.linkedin.orrery.graphql.types.mappers.ModelMapper;
import com.linkedin.template.OrreryPageTemplateSurface;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PageTemplateSurfaceMapper
    implements ModelMapper<
        OrreryPageTemplateSurface,
        com.linkedin.orrery.graphql.generated.OrreryPageTemplateSurface> {

  public static final PageTemplateSurfaceMapper INSTANCE = new PageTemplateSurfaceMapper();

  public static com.linkedin.orrery.graphql.generated.OrreryPageTemplateSurface map(
      @Nonnull final OrreryPageTemplateSurface surface) {
    return INSTANCE.apply(null, surface);
  }

  @Override
  public com.linkedin.orrery.graphql.generated.OrreryPageTemplateSurface apply(
      @Nullable final QueryContext context, @Nonnull final OrreryPageTemplateSurface surface) {
    final com.linkedin.orrery.graphql.generated.OrreryPageTemplateSurface result =
        new com.linkedin.orrery.graphql.generated.OrreryPageTemplateSurface();

    if (surface.hasSurfaceType()) {
      result.setSurfaceType(PageTemplateSurfaceType.valueOf(surface.getSurfaceType().toString()));
    }

    return result;
  }
}
