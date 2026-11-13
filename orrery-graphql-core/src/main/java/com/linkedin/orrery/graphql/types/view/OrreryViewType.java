package com.linkedin.orrery.graphql.types.view;

import static com.linkedin.orrery.graphql.authorization.AuthorizationUtils.canView;
import static com.linkedin.metadata.Constants.*;
import static com.linkedin.metadata.Constants.ORRERY_VIEW_KEY_ASPECT_NAME;

import com.google.common.collect.ImmutableSet;
import com.linkedin.common.urn.Urn;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.OrreryView;
import com.linkedin.orrery.graphql.generated.Entity;
import com.linkedin.orrery.graphql.generated.EntityType;
import com.linkedin.orrery.graphql.util.AspectUtils;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.client.EntityClient;
import graphql.execution.DataFetcherResult;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OrreryViewType
    implements com.linkedin.orrery.graphql.types.EntityType<OrreryView, String> {
  public static final Set<String> ASPECTS_TO_FETCH = ImmutableSet.of(ORRERY_VIEW_INFO_ASPECT_NAME);
  private final EntityClient _entityClient;

  @Override
  public EntityType type() {
    return EntityType.ORRERY_VIEW;
  }

  @Override
  public Function<Entity, String> getKeyProvider() {
    return Entity::getUrn;
  }

  @Override
  public Class<OrreryView> objectClass() {
    return OrreryView.class;
  }

  @Override
  public List<DataFetcherResult<OrreryView>> batchLoad(
      @Nonnull List<String> urns, @Nonnull QueryContext context) throws Exception {
    final List<Urn> viewUrns = urns.stream().map(this::getUrn).collect(Collectors.toList());

    try {
      // Determine optimal aspects to fetch based on GraphQL field selections
      Set<String> aspectsToResolve =
          AspectUtils.getOptimizedAspects(
              context, name(), ASPECTS_TO_FETCH, ORRERY_VIEW_KEY_ASPECT_NAME);
      final Map<Urn, EntityResponse> entities =
          _entityClient.batchGetV2(
              context.getOperationContext(),
              ORRERY_VIEW_ENTITY_NAME,
              viewUrns.stream()
                  .filter(urn -> canView(context.getOperationContext(), urn))
                  .collect(Collectors.toSet()),
              aspectsToResolve);

      final List<EntityResponse> gmsResults = new ArrayList<>();
      for (Urn urn : viewUrns) {
        gmsResults.add(entities.getOrDefault(urn, null));
      }
      return gmsResults.stream()
          .map(
              gmsResult ->
                  gmsResult == null
                      ? null
                      : DataFetcherResult.<OrreryView>newResult()
                          .data(OrreryViewMapper.map(context, gmsResult))
                          .build())
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new RuntimeException("Failed to batch load Views", e);
    }
  }

  private Urn getUrn(final String urnStr) {
    try {
      return Urn.createFromString(urnStr);
    } catch (URISyntaxException e) {
      throw new RuntimeException(String.format("Failed to convert urn string %s into Urn", urnStr));
    }
  }
}
