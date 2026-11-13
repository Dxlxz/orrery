package com.linkedin.orrery.graphql.types.common.mappers;

import com.linkedin.common.AuditStamp;
import com.linkedin.common.InstitutionalMemoryMetadata;
import com.linkedin.common.url.Url;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.InstitutionalMemoryMetadataUpdate;
import com.linkedin.orrery.graphql.types.corpuser.CorpUserUtils;
import com.linkedin.orrery.graphql.types.mappers.ModelMapper;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class InstitutionalMemoryMetadataUpdateMapper
    implements ModelMapper<InstitutionalMemoryMetadataUpdate, InstitutionalMemoryMetadata> {

  private static final InstitutionalMemoryMetadataUpdateMapper INSTANCE =
      new InstitutionalMemoryMetadataUpdateMapper();

  public static InstitutionalMemoryMetadata map(
      @Nullable QueryContext context, @Nonnull final InstitutionalMemoryMetadataUpdate input) {
    return INSTANCE.apply(context, input);
  }

  @Override
  public InstitutionalMemoryMetadata apply(
      @Nullable QueryContext context, @Nonnull final InstitutionalMemoryMetadataUpdate input) {
    final InstitutionalMemoryMetadata metadata = new InstitutionalMemoryMetadata();
    metadata.setDescription(input.getDescription());
    metadata.setUrl(new Url(input.getUrl()));
    metadata.setCreateStamp(
        new AuditStamp()
            .setActor(CorpUserUtils.getCorpUserUrn(input.getAuthor()))
            .setTime(
                input.getCreatedAt() == null ? System.currentTimeMillis() : input.getCreatedAt()));
    return metadata;
  }
}
