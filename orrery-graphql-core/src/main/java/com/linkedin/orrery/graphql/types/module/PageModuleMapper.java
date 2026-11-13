package com.linkedin.orrery.graphql.types.module;

import static com.linkedin.orrery.graphql.authorization.AuthorizationUtils.canView;
import static com.linkedin.metadata.Constants.ORRERY_PAGE_MODULE_PROPERTIES_ASPECT_NAME;

import com.linkedin.common.urn.Urn;
import com.linkedin.data.DataMap;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.authorization.AuthorizationUtils;
import com.linkedin.orrery.graphql.generated.*;
import com.linkedin.orrery.graphql.types.common.mappers.util.MappingHelper;
import com.linkedin.orrery.graphql.types.mappers.MapperUtils;
import com.linkedin.orrery.graphql.types.mappers.ModelMapper;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.EnvelopedAspectMap;
import com.linkedin.module.OrreryPageModuleProperties;
import java.time.Instant;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class PageModuleMapper implements ModelMapper<EntityResponse, OrreryPageModule> {

  public static final PageModuleMapper INSTANCE = new PageModuleMapper();

  public static OrreryPageModule map(
      @Nullable final QueryContext context, @Nonnull final EntityResponse entityResponse) {
    return INSTANCE.apply(context, entityResponse);
  }

  @Override
  public OrreryPageModule apply(
      @Nullable final QueryContext context, @Nonnull final EntityResponse entityResponse) {
    final OrreryPageModule result = new OrreryPageModule();
    Urn entityUrn = entityResponse.getUrn();

    result.setUrn(entityResponse.getUrn().toString());
    result.setType(EntityType.ORRERY_PAGE_MODULE);

    setDefaultModule(result);

    EnvelopedAspectMap aspectMap = entityResponse.getAspects();
    MappingHelper<OrreryPageModule> mappingHelper = new MappingHelper<>(aspectMap, result);
    mappingHelper.mapToResult(
        ORRERY_PAGE_MODULE_PROPERTIES_ASPECT_NAME,
        (module, dataMap) -> mapProperties(module, dataMap));

    if (context != null && !canView(context.getOperationContext(), entityUrn)) {
      return AuthorizationUtils.restrictEntity(result, OrreryPageModule.class);
    } else {
      return result;
    }
  }

  private void mapProperties(@Nonnull OrreryPageModule module, @Nonnull DataMap dataMap) {
    OrreryPageModuleProperties gmsModuleProperties = new OrreryPageModuleProperties(dataMap);
    com.linkedin.orrery.graphql.generated.OrreryPageModuleProperties properties =
        new com.linkedin.orrery.graphql.generated.OrreryPageModuleProperties();

    if (gmsModuleProperties.hasName()) {
      properties.setName(gmsModuleProperties.getName());
    }

    if (gmsModuleProperties.hasType()) {
      properties.setType(PageModuleTypeMapper.map(gmsModuleProperties.getType()));
    }

    if (gmsModuleProperties.hasVisibility()) {
      properties.setVisibility(PageModuleVisibilityMapper.map(gmsModuleProperties.getVisibility()));
    }

    if (gmsModuleProperties.hasParams()) {
      properties.setParams(PageModuleParamsMapper.map(gmsModuleProperties.getParams()));
    }

    if (gmsModuleProperties.hasCreated()) {
      properties.setCreated(MapperUtils.createResolvedAuditStamp(gmsModuleProperties.getCreated()));
    }

    if (gmsModuleProperties.hasLastModified()) {
      properties.setLastModified(
          MapperUtils.createResolvedAuditStamp(gmsModuleProperties.getLastModified()));
    }

    module.setProperties(properties);
  }

  /*
   * In the case that a module is deleted and the references haven't been cleaned up yet (this process is async)
   * set a default module to prevent APIs breaking. The UI queries for whether the entity exists and it will
   * be filtered out.
   */
  private void setDefaultModule(final OrreryPageModule result) {
    com.linkedin.orrery.graphql.generated.OrreryPageModuleProperties properties =
        new com.linkedin.orrery.graphql.generated.OrreryPageModuleProperties();
    properties.setName("");
    properties.setType(OrreryPageModuleType.OWNED_ASSETS);
    properties.setParams(new OrreryPageModuleParams());

    OrreryPageModuleVisibility visibility = new OrreryPageModuleVisibility();
    visibility.setScope(PageModuleScope.GLOBAL);
    properties.setVisibility(visibility);

    ResolvedAuditStamp auditStamp = new ResolvedAuditStamp();
    auditStamp.setTime(Instant.now().toEpochMilli());
    auditStamp.setActor(new CorpUser());
    properties.setCreated(auditStamp);
    properties.setLastModified(auditStamp);

    result.setProperties(properties);
  }
}
