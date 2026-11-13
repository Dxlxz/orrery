package com.orrery.authorization.fieldresolverprovider;

import com.orrery.authorization.EntityFieldType;
import com.orrery.authorization.EntitySpec;
import com.orrery.authorization.FieldResolver;
import com.datastax.oss.driver.shaded.guava.common.collect.ImmutableList;
import io.orreryproject.metadata.context.OperationContext;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nonnull;

/** Provides field resolver for entity type given entitySpec */
public class EntityTypeFieldResolverProvider implements EntityFieldResolverProvider {

  @Override
  public List<EntityFieldType> getFieldTypes() {
    return ImmutableList.of(EntityFieldType.TYPE, EntityFieldType.RESOURCE_TYPE);
  }

  @Override
  public FieldResolver getFieldResolver(
      @Nonnull OperationContext opContext, EntitySpec entitySpec) {
    return FieldResolver.getResolverFromValues(Collections.singleton(entitySpec.getType()));
  }
}
