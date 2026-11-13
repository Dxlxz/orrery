package com.linkedin.orrery.graphql.types.mappers;

import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.BrowseResultGroup;
import com.linkedin.orrery.graphql.generated.BrowseResultMetadata;
import com.linkedin.orrery.graphql.generated.BrowseResults;
import com.linkedin.orrery.graphql.generated.Entity;
import com.linkedin.orrery.graphql.types.common.mappers.UrnToEntityMapper;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class BrowseResultMapper {
  private BrowseResultMapper() {}

  public static BrowseResults map(
      @Nullable final QueryContext context, com.linkedin.metadata.browse.BrowseResult input) {
    final BrowseResults result = new BrowseResults();

    if (!input.hasFrom() || !input.hasPageSize() || !input.hasNumElements()) {
      return result;
    }

    result.setStart(input.getFrom());
    result.setCount(input.getPageSize());
    result.setTotal(input.getNumElements());

    final BrowseResultMetadata browseResultMetadata = new BrowseResultMetadata();
    browseResultMetadata.setPath(
        BrowsePathMapper.map(context, input.getMetadata().getPath()).getPath());
    browseResultMetadata.setTotalNumEntities(input.getMetadata().getTotalNumEntities());
    result.setMetadata(browseResultMetadata);

    List<Entity> entities =
        input.getEntities().stream()
            .map(entity -> UrnToEntityMapper.map(context, entity.getUrn()))
            .collect(Collectors.toList());
    result.setEntities(entities);

    List<BrowseResultGroup> groups =
        input.getGroups().stream().map(BrowseResultMapper::mapGroup).collect(Collectors.toList());
    result.setGroups(groups);

    return result;
  }

  private static BrowseResultGroup mapGroup(
      @Nonnull final com.linkedin.metadata.browse.BrowseResultGroup group) {
    final BrowseResultGroup result = new BrowseResultGroup();
    result.setName(group.getName());
    result.setCount(group.getCount());
    return result;
  }
}
