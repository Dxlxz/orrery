package com.linkedin.orrery.graphql.types.file;

import static com.linkedin.metadata.Constants.ORRERY_FILE_ENTITY_NAME;
import static com.linkedin.metadata.Constants.ORRERY_FILE_INFO_ASPECT_NAME;
import static com.linkedin.metadata.Constants.ORRERY_FILE_KEY_ASPECT_NAME;

import com.google.common.collect.ImmutableSet;
import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.OrreryFile;
import com.linkedin.orrery.graphql.generated.Entity;
import com.linkedin.orrery.graphql.generated.EntityType;
import com.linkedin.orrery.graphql.util.AspectUtils;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.client.EntityClient;
import graphql.execution.DataFetcherResult;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OrreryFileType
    implements com.linkedin.orrery.graphql.types.EntityType<OrreryFile, String> {
  public static final Set<String> ASPECTS_TO_FETCH = ImmutableSet.of(ORRERY_FILE_INFO_ASPECT_NAME);
  private final EntityClient _entityClient;

  @Override
  public EntityType type() {
    return EntityType.ORRERY_FILE;
  }

  @Override
  public Function<Entity, String> getKeyProvider() {
    return Entity::getUrn;
  }

  @Override
  public Class<OrreryFile> objectClass() {
    return OrreryFile.class;
  }

  @Override
  public List<DataFetcherResult<OrreryFile>> batchLoad(
      @Nonnull List<String> urns, @Nonnull QueryContext context) throws Exception {
    final List<Urn> fileUrns = urns.stream().map(UrnUtils::getUrn).collect(Collectors.toList());

    try {
      // Determine optimal aspects to fetch based on GraphQL field selections. orreryFileInfo is a
      // hydration-required aspect (see AspectUtils.HYDRATION_REQUIRED_ASPECTS) so it is always
      // fetched.
      Set<String> aspectsToResolve =
          AspectUtils.getOptimizedAspects(
              context, name(), ASPECTS_TO_FETCH, ORRERY_FILE_KEY_ASPECT_NAME);
      final Map<Urn, EntityResponse> entities =
          _entityClient.batchGetV2(
              context.getOperationContext(),
              ORRERY_FILE_ENTITY_NAME,
              new HashSet<>(fileUrns),
              aspectsToResolve);

      final List<EntityResponse> gmsResults = new ArrayList<>(urns.size());
      for (Urn urn : fileUrns) {
        gmsResults.add(entities.getOrDefault(urn, null));
      }
      return gmsResults.stream()
          .map(
              gmsResult ->
                  gmsResult == null
                      ? null
                      : DataFetcherResult.<OrreryFile>newResult()
                          .data(OrreryFileMapper.map(context, gmsResult))
                          .build())
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new RuntimeException("Failed to batch load Orrery Files", e);
    }
  }
}
