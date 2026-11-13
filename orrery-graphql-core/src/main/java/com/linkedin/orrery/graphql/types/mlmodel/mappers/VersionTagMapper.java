package com.linkedin.orrery.graphql.types.mlmodel.mappers;

import com.linkedin.common.VersionTag;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.types.mappers.ModelMapper;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class VersionTagMapper
    implements ModelMapper<VersionTag, com.linkedin.orrery.graphql.generated.VersionTag> {
  public static final VersionTagMapper INSTANCE = new VersionTagMapper();

  public static com.linkedin.orrery.graphql.generated.VersionTag map(
      @Nullable QueryContext context, @Nonnull final VersionTag versionTag) {
    return INSTANCE.apply(context, versionTag);
  }

  @Override
  public com.linkedin.orrery.graphql.generated.VersionTag apply(
      @Nullable QueryContext context, @Nonnull final VersionTag input) {
    final com.linkedin.orrery.graphql.generated.VersionTag result =
        new com.linkedin.orrery.graphql.generated.VersionTag();
    result.setVersionTag(input.getVersionTag());
    return result;
  }
}
