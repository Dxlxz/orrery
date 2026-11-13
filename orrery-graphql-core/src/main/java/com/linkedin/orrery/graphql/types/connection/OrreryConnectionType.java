package com.linkedin.orrery.graphql.types.connection;

import com.google.common.collect.ImmutableSet;
import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.OrreryConnection;
import com.linkedin.orrery.graphql.generated.Entity;
import com.linkedin.orrery.graphql.generated.EntityType;
import com.linkedin.orrery.graphql.resolvers.connection.ConnectionMapper;
import com.linkedin.orrery.graphql.util.AspectUtils;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.client.EntityClient;
import com.linkedin.metadata.Constants;
import graphql.execution.DataFetcherResult;
import io.orreryproject.metadata.services.SecretService;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

public class OrreryConnectionType
    implements com.linkedin.orrery.graphql.types.EntityType<OrreryConnection, String> {

  static final Set<String> ASPECTS_TO_FETCH =
      ImmutableSet.of(
          Constants.ORRERY_CONNECTION_DETAILS_ASPECT_NAME,
          Constants.DATA_PLATFORM_INSTANCE_ASPECT_NAME);
  private final EntityClient _entityClient;
  private final SecretService _secretService;

  public OrreryConnectionType(
      @Nonnull final EntityClient entityClient, @Nonnull final SecretService secretService) {
    _entityClient = Objects.requireNonNull(entityClient, "entityClient must not be null");
    _secretService = Objects.requireNonNull(secretService, "secretService must not be null");
  }

  @Override
  public EntityType type() {
    return EntityType.ORRERY_CONNECTION;
  }

  @Override
  public Function<Entity, String> getKeyProvider() {
    return Entity::getUrn;
  }

  @Override
  public Class<OrreryConnection> objectClass() {
    return OrreryConnection.class;
  }

  @Override
  public List<DataFetcherResult<OrreryConnection>> batchLoad(
      @Nonnull List<String> urns, @Nonnull QueryContext context) throws Exception {
    final List<Urn> connectionUrns =
        urns.stream().map(UrnUtils::getUrn).collect(Collectors.toList());
    try {
      // Determine optimal aspects to fetch based on GraphQL field selections
      Set<String> aspectsToResolve =
          AspectUtils.getOptimizedAspects(
              context, name(), ASPECTS_TO_FETCH, Constants.ORRERY_CONNECTION_KEY_ASPECT_NAME);
      final Map<Urn, EntityResponse> entities =
          _entityClient.batchGetV2(
              context.getOperationContext(),
              Constants.ORRERY_CONNECTION_ENTITY_NAME,
              new HashSet<>(connectionUrns),
              aspectsToResolve);

      final List<EntityResponse> gmsResults = new ArrayList<>();
      for (Urn urn : connectionUrns) {
        gmsResults.add(entities.getOrDefault(urn, null));
      }
      return gmsResults.stream()
          .map(
              gmsResult ->
                  gmsResult == null
                      ? null
                      : DataFetcherResult.<OrreryConnection>newResult()
                          .data(ConnectionMapper.map(context, gmsResult, _secretService))
                          .build())
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new RuntimeException("Failed to batch load Connections", e);
    }
  }
}
