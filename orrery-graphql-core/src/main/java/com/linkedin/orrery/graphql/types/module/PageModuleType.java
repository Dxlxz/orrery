package com.linkedin.orrery.graphql.types.module;

import static com.linkedin.metadata.Constants.ORRERY_PAGE_MODULE_ENTITY_NAME;
import static com.linkedin.metadata.Constants.ORRERY_PAGE_MODULE_KEY_ASPECT_NAME;
import static com.linkedin.metadata.Constants.ORRERY_PAGE_MODULE_PROPERTIES_ASPECT_NAME;

import com.google.common.collect.ImmutableSet;
import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.OrreryPageModule;
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
public class PageModuleType
    implements com.linkedin.orrery.graphql.types.EntityType<OrreryPageModule, String> {
  public static final Set<String> ASPECTS_TO_FETCH =
      ImmutableSet.of(ORRERY_PAGE_MODULE_PROPERTIES_ASPECT_NAME);
  private final EntityClient _entityClient;

  @Override
  public EntityType type() {
    return EntityType.ORRERY_PAGE_MODULE;
  }

  @Override
  public Function<Entity, String> getKeyProvider() {
    return Entity::getUrn;
  }

  @Override
  public Class<OrreryPageModule> objectClass() {
    return OrreryPageModule.class;
  }

  @Override
  public List<DataFetcherResult<OrreryPageModule>> batchLoad(
      @Nonnull List<String> urns, @Nonnull QueryContext context) throws Exception {
    final List<Urn> moduleUrns = urns.stream().map(UrnUtils::getUrn).collect(Collectors.toList());

    try {
      // Determine optimal aspects to fetch based on GraphQL field selections
      Set<String> aspectsToResolve =
          AspectUtils.getOptimizedAspects(
              context, name(), ASPECTS_TO_FETCH, ORRERY_PAGE_MODULE_KEY_ASPECT_NAME);
      final Map<Urn, EntityResponse> entities =
          _entityClient.batchGetV2(
              context.getOperationContext(),
              ORRERY_PAGE_MODULE_ENTITY_NAME,
              new HashSet<>(moduleUrns),
              aspectsToResolve);

      final List<EntityResponse> gmsResults = new ArrayList<>(urns.size());
      for (Urn urn : moduleUrns) {
        gmsResults.add(entities.getOrDefault(urn, null));
      }
      return gmsResults.stream()
          .map(
              gmsResult ->
                  gmsResult == null
                      ? null
                      : DataFetcherResult.<OrreryPageModule>newResult()
                          .data(PageModuleMapper.map(context, gmsResult))
                          .build())
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new RuntimeException("Failed to batch load Page Modules", e);
    }
  }
}
