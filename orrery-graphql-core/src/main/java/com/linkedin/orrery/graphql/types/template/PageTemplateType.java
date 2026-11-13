package com.linkedin.orrery.graphql.types.template;

import static com.linkedin.metadata.Constants.ORRERY_PAGE_TEMPLATE_ENTITY_NAME;
import static com.linkedin.metadata.Constants.ORRERY_PAGE_TEMPLATE_KEY_ASPECT_NAME;
import static com.linkedin.metadata.Constants.ORRERY_PAGE_TEMPLATE_PROPERTIES_ASPECT_NAME;

import com.google.common.collect.ImmutableSet;
import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.OrreryPageTemplate;
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
public class PageTemplateType
    implements com.linkedin.orrery.graphql.types.EntityType<OrreryPageTemplate, String> {
  public static final Set<String> ASPECTS_TO_FETCH =
      ImmutableSet.of(ORRERY_PAGE_TEMPLATE_PROPERTIES_ASPECT_NAME);
  private final EntityClient _entityClient;

  @Override
  public EntityType type() {
    return EntityType.ORRERY_PAGE_TEMPLATE;
  }

  @Override
  public Function<Entity, String> getKeyProvider() {
    return Entity::getUrn;
  }

  @Override
  public Class<OrreryPageTemplate> objectClass() {
    return OrreryPageTemplate.class;
  }

  @Override
  public List<DataFetcherResult<OrreryPageTemplate>> batchLoad(
      @Nonnull List<String> urns, @Nonnull QueryContext context) throws Exception {
    final List<Urn> applicationUrns =
        urns.stream().map(UrnUtils::getUrn).collect(Collectors.toList());

    try {
      // Determine optimal aspects to fetch based on GraphQL field selections
      Set<String> aspectsToResolve =
          // orreryPageTemplateProperties is a hydration-required aspect (see
          // AspectUtils.HYDRATION_REQUIRED_ASPECTS) so it is always fetched.
          AspectUtils.getOptimizedAspects(
              context, name(), ASPECTS_TO_FETCH, ORRERY_PAGE_TEMPLATE_KEY_ASPECT_NAME);
      final Map<Urn, EntityResponse> entities =
          _entityClient.batchGetV2(
              context.getOperationContext(),
              ORRERY_PAGE_TEMPLATE_ENTITY_NAME,
              new HashSet<>(applicationUrns),
              aspectsToResolve);

      final List<EntityResponse> gmsResults = new ArrayList<>(urns.size());
      for (Urn urn : applicationUrns) {
        gmsResults.add(entities.getOrDefault(urn, null));
      }
      return gmsResults.stream()
          .map(
              gmsResult ->
                  gmsResult == null
                      ? null
                      : DataFetcherResult.<OrreryPageTemplate>newResult()
                          .data(PageTemplateMapper.map(context, gmsResult))
                          .build())
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw new RuntimeException("Failed to batch load Applications", e);
    }
  }
}
