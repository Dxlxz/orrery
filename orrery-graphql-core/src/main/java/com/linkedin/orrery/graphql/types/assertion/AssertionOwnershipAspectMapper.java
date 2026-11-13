package com.linkedin.orrery.graphql.types.assertion;

import com.linkedin.common.Ownership;
import com.linkedin.common.urn.Urn;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.Assertion;
import com.linkedin.orrery.graphql.types.common.mappers.OwnershipMapper;
import com.linkedin.entity.EnvelopedAspect;
import com.linkedin.entity.EnvelopedAspectMap;
import com.linkedin.metadata.Constants;
import javax.annotation.Nullable;

/** Maps the ownership Pegasus aspect onto the GraphQL {@link Assertion} when present. */
final class AssertionOwnershipAspectMapper {

  private AssertionOwnershipAspectMapper() {}

  static void applyOwnershipIfPresent(
      @Nullable QueryContext context, Urn entityUrn, EnvelopedAspectMap aspects, Assertion result) {
    final EnvelopedAspect envelopedOwnership = aspects.get(Constants.OWNERSHIP_ASPECT_NAME);
    if (envelopedOwnership != null) {
      result.setOwnership(
          OwnershipMapper.map(
              context, new Ownership(envelopedOwnership.getValue().data()), entityUrn));
    }
  }
}
