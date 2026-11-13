package com.linkedin.orrery.graphql.types.relationships.mappers;

import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.EntityRelationshipLegacy;
import com.linkedin.orrery.graphql.generated.EntityWithRelationships;
import com.linkedin.orrery.graphql.types.common.mappers.AuditStampMapper;
import com.linkedin.orrery.graphql.types.common.mappers.UrnToEntityMapper;
import com.linkedin.orrery.graphql.types.mappers.ModelMapper;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class EntityRelationshipLegacyMapper
    implements ModelMapper<com.linkedin.common.EntityRelationship, EntityRelationshipLegacy> {

  public static final EntityRelationshipLegacyMapper INSTANCE =
      new EntityRelationshipLegacyMapper();

  public static EntityRelationshipLegacy map(
      @Nullable QueryContext context,
      @Nonnull final com.linkedin.common.EntityRelationship relationship) {
    return INSTANCE.apply(context, relationship);
  }

  @Override
  public EntityRelationshipLegacy apply(
      @Nullable QueryContext context,
      @Nonnull final com.linkedin.common.EntityRelationship relationship) {
    final EntityRelationshipLegacy result = new EntityRelationshipLegacy();

    EntityWithRelationships partialLineageEntity =
        (EntityWithRelationships) UrnToEntityMapper.map(context, relationship.getEntity());
    if (partialLineageEntity != null) {
      result.setEntity(partialLineageEntity);
    }
    if (relationship.hasCreated()) {
      result.setCreated(AuditStampMapper.map(context, relationship.getCreated()));
    }
    return result;
  }
}
