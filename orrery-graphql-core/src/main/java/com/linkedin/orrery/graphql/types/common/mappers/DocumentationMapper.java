package com.linkedin.orrery.graphql.types.common.mappers;

import com.linkedin.common.urn.Urn;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.OrreryConnection;
import com.linkedin.orrery.graphql.generated.Documentation;
import com.linkedin.orrery.graphql.generated.DocumentationAssociation;
import com.linkedin.orrery.graphql.generated.EntityType;
import com.linkedin.orrery.graphql.types.mappers.ModelMapper;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class DocumentationMapper
    implements ModelMapper<com.linkedin.common.Documentation, Documentation> {

  public static final DocumentationMapper INSTANCE = new DocumentationMapper();

  public static Documentation map(
      @Nullable final QueryContext context,
      @Nonnull final com.linkedin.common.Documentation metadata) {
    return INSTANCE.apply(context, metadata);
  }

  @Override
  public Documentation apply(
      @Nullable final QueryContext context,
      @Nonnull final com.linkedin.common.Documentation input) {
    final Documentation result = new Documentation();
    result.setDocumentations(
        input.getDocumentations().stream()
            .map(docAssociation -> mapDocAssociation(context, docAssociation))
            .collect(Collectors.toList()));
    return result;
  }

  private DocumentationAssociation mapDocAssociation(
      @Nullable final QueryContext context,
      @Nonnull final com.linkedin.common.DocumentationAssociation association) {
    final DocumentationAssociation result = new DocumentationAssociation();
    result.setDocumentation(association.getDocumentation());
    if (association.getAttribution() != null) {
      result.setAttribution(MetadataAttributionMapper.map(context, association.getAttribution()));
    }
    return result;
  }

  private OrreryConnection mapConnectionEntity(@Nonnull final Urn urn) {
    OrreryConnection connection = new OrreryConnection();
    connection.setUrn(urn.toString());
    connection.setType(EntityType.ORRERY_CONNECTION);
    return connection;
  }
}
