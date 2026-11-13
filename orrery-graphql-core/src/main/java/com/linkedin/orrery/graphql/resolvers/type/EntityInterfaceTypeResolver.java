package com.linkedin.orrery.graphql.resolvers.type;

import com.google.common.collect.Iterables;
import com.linkedin.orrery.graphql.types.EntityType;
import com.linkedin.orrery.graphql.types.LoadableType;
import graphql.TypeResolutionEnvironment;
import graphql.schema.GraphQLObjectType;
import graphql.schema.TypeResolver;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Responsible for resolving the {@link com.linkedin.orrery.graphql.generated.Entity} interface
 * type.
 */
public class EntityInterfaceTypeResolver implements TypeResolver {

  private final List<EntityType<?, ?>> _entities;

  public EntityInterfaceTypeResolver(final List<EntityType<?, ?>> entities) {
    _entities = entities;
  }

  @Override
  public GraphQLObjectType getType(TypeResolutionEnvironment env) {
    Object javaObject = env.getObject();
    final LoadableType<?, ?> filteredEntity =
        Iterables.getOnlyElement(
            _entities.stream()
                .filter(entity -> javaObject.getClass().isAssignableFrom(entity.objectClass()))
                .collect(Collectors.toList()));
    return env.getSchema().getObjectType(filteredEntity.objectClass().getSimpleName());
  }
}
