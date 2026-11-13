package com.linkedin.orrery.graphql.types.template;

import static com.linkedin.orrery.graphql.authorization.AuthorizationUtils.canView;
import static com.linkedin.metadata.Constants.ORRERY_PAGE_TEMPLATE_PROPERTIES_ASPECT_NAME;

import com.linkedin.common.urn.Urn;
import com.linkedin.data.DataMap;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.authorization.AuthorizationUtils;
import com.linkedin.orrery.graphql.generated.OrreryPageModule;
import com.linkedin.orrery.graphql.generated.OrreryPageTemplate;
import com.linkedin.orrery.graphql.generated.OrreryPageTemplateAssetSummary;
import com.linkedin.orrery.graphql.generated.OrreryPageTemplateRow;
import com.linkedin.orrery.graphql.generated.EntityType;
import com.linkedin.orrery.graphql.generated.StructuredPropertyEntity;
import com.linkedin.orrery.graphql.generated.SummaryElement;
import com.linkedin.orrery.graphql.generated.SummaryElementType;
import com.linkedin.orrery.graphql.types.common.mappers.util.MappingHelper;
import com.linkedin.orrery.graphql.types.common.mappers.util.UnknownEnumValues;
import com.linkedin.orrery.graphql.types.mappers.MapperUtils;
import com.linkedin.orrery.graphql.types.mappers.ModelMapper;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.EnvelopedAspectMap;
import com.linkedin.template.OrreryPageTemplateProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PageTemplateMapper implements ModelMapper<EntityResponse, OrreryPageTemplate> {

  public static final PageTemplateMapper INSTANCE = new PageTemplateMapper();

  public static OrreryPageTemplate map(
      @Nullable final QueryContext context, @Nonnull final EntityResponse entityResponse) {
    return INSTANCE.apply(context, entityResponse);
  }

  @Override
  public OrreryPageTemplate apply(
      @Nullable final QueryContext context, @Nonnull final EntityResponse entityResponse) {
    final OrreryPageTemplate result = new OrreryPageTemplate();
    Urn entityUrn = entityResponse.getUrn();

    result.setUrn(entityResponse.getUrn().toString());
    result.setType(EntityType.ORRERY_PAGE_TEMPLATE);

    EnvelopedAspectMap aspectMap = entityResponse.getAspects();

    // Handle getting deleted template by broken reference (check if required aspect was fetched)
    if (aspectMap.get(ORRERY_PAGE_TEMPLATE_PROPERTIES_ASPECT_NAME) == null) {
      log.warn("Page Template {} doesn't have required aspects", entityUrn);
      return null;
    }

    MappingHelper<OrreryPageTemplate> mappingHelper = new MappingHelper<>(aspectMap, result);
    mappingHelper.mapToResult(
        ORRERY_PAGE_TEMPLATE_PROPERTIES_ASPECT_NAME,
        (application, dataMap) -> mapProperties(application, dataMap));

    if (context != null && !canView(context.getOperationContext(), entityUrn)) {
      return AuthorizationUtils.restrictEntity(result, OrreryPageTemplate.class);
    } else {
      return result;
    }
  }

  private void mapProperties(@Nonnull OrreryPageTemplate template, @Nonnull DataMap dataMap) {
    OrreryPageTemplateProperties gmsTemplateProperties =
        new OrreryPageTemplateProperties(dataMap);
    com.linkedin.orrery.graphql.generated.OrreryPageTemplateProperties properties =
        new com.linkedin.orrery.graphql.generated.OrreryPageTemplateProperties();

    List<OrreryPageTemplateRow> rows = new ArrayList<>();
    gmsTemplateProperties
        .getRows()
        .forEach(
            row -> {
              OrreryPageTemplateRow templateRow = new OrreryPageTemplateRow();
              List<OrreryPageModule> modules = new ArrayList<>();
              row.getModules()
                  .forEach(
                      moduleUrn -> {
                        OrreryPageModule module = new OrreryPageModule();
                        module.setUrn(moduleUrn.toString());
                        module.setType(EntityType.ORRERY_PAGE_MODULE);
                        modules.add(module);
                      });
              templateRow.setModules(modules);
              rows.add(templateRow);
            });
    properties.setRows(rows);

    if (gmsTemplateProperties.getAssetSummary() != null) {
      properties.setAssetSummary(mapAssetSummary(gmsTemplateProperties.getAssetSummary()));
    }

    if (gmsTemplateProperties.hasSurface()) {
      properties.setSurface(PageTemplateSurfaceMapper.map(gmsTemplateProperties.getSurface()));
    }

    if (gmsTemplateProperties.hasVisibility()) {
      properties.setVisibility(
          PageTemplateVisibilityMapper.map(gmsTemplateProperties.getVisibility()));
    }

    if (gmsTemplateProperties.hasCreated()) {
      properties.setCreated(
          MapperUtils.createResolvedAuditStamp(gmsTemplateProperties.getCreated()));
    }

    if (gmsTemplateProperties.hasLastModified()) {
      properties.setLastModified(
          MapperUtils.createResolvedAuditStamp(gmsTemplateProperties.getLastModified()));
    }

    template.setProperties(properties);
  }

  private OrreryPageTemplateAssetSummary mapAssetSummary(
      com.linkedin.template.OrreryPageTemplateAssetSummary input) {
    OrreryPageTemplateAssetSummary assetSummary = new OrreryPageTemplateAssetSummary();
    assetSummary.setSummaryElements(new ArrayList<>());
    if (input.getSummaryElements() != null) {

      List<SummaryElement> summaryElements =
          input.getSummaryElements().stream()
              .map(
                  el -> {
                    SummaryElement summaryElement = new SummaryElement();
                    summaryElement.setElementType(
                        UnknownEnumValues.mapOrNull(SummaryElementType.class, el.getElementType()));
                    if (el.getStructuredPropertyUrn() != null) {
                      StructuredPropertyEntity structuredProperty = new StructuredPropertyEntity();
                      structuredProperty.setUrn(el.getStructuredPropertyUrn().toString());
                      structuredProperty.setType(EntityType.STRUCTURED_PROPERTY);
                      summaryElement.setStructuredProperty(structuredProperty);
                    }
                    return summaryElement;
                  })
              // Element types only a newer version knows (e.g. after a rollback) are skipped.
              .filter(summaryElement -> summaryElement.getElementType() != null)
              .collect(Collectors.toList());

      assetSummary.setSummaryElements(summaryElements);
    }

    return assetSummary;
  }
}
