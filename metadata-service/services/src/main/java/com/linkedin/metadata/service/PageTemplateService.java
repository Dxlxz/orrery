package com.linkedin.metadata.service;

import com.orrery.authorization.AuthUtil;
import com.linkedin.common.AuditStamp;
import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.client.EntityClient;
import com.linkedin.metadata.Constants;
import com.linkedin.metadata.authorization.PoliciesConfig;
import com.linkedin.metadata.entity.AspectUtils;
import com.linkedin.metadata.key.OrreryPageTemplateKey;
import com.linkedin.metadata.utils.EntityKeyUtils;
import com.linkedin.mxe.MetadataChangeProposal;
import com.linkedin.template.OrreryPageTemplateAssetSummary;
import com.linkedin.template.OrreryPageTemplateProperties;
import com.linkedin.template.OrreryPageTemplateRow;
import com.linkedin.template.OrreryPageTemplateRowArray;
import com.linkedin.template.OrreryPageTemplateSurface;
import com.linkedin.template.OrreryPageTemplateVisibility;
import com.linkedin.template.PageTemplateScope;
import com.linkedin.template.PageTemplateSurfaceType;
import io.orreryproject.metadata.context.OperationContext;
import io.orreryproject.openapi.exception.UnauthorizedException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class PageTemplateService {
  private final EntityClient entityClient;
  private static final String DEFAULT_HOME_PAGE_TEMPLATE_URN =
      "urn:li:orreryPageTemplate:home_default_1";

  public PageTemplateService(@Nonnull EntityClient entityClient) {
    this.entityClient = entityClient;
  }

  public Urn upsertPageTemplate(
      @Nonnull OperationContext opContext,
      @Nullable final String urn,
      @Nonnull final List<OrreryPageTemplateRow> rows,
      @Nonnull final PageTemplateScope scope,
      @Nonnull final PageTemplateSurfaceType surfaceType) {
    return upsertPageTemplate(opContext, urn, rows, scope, surfaceType, null);
  }

  /**
   * Upserts a Orrery page template. If the page template with the provided urn already exists,
   * then it will be overwritten.
   *
   * <p>Callers must hold MANAGE_HOME_PAGE_TEMPLATES_PRIVILEGE to create or modify GLOBAL-scoped
   * templates, or to modify any template whose existing scope is GLOBAL.
   *
   * @return the URN of the new page template.
   */
  public Urn upsertPageTemplate(
      @Nonnull OperationContext opContext,
      @Nullable final String urn,
      @Nonnull final List<OrreryPageTemplateRow> rows,
      @Nonnull final PageTemplateScope scope,
      @Nonnull final PageTemplateSurfaceType surfaceType,
      @Nullable final OrreryPageTemplateAssetSummary assetSummary) {
    Objects.requireNonNull(rows, "rows must not be null");
    Objects.requireNonNull(scope, "scope must not be null");
    Objects.requireNonNull(surfaceType, "surfaceType must not be null");

    // 1. Optionally generate new page template urn
    Urn templateUrn = null;
    if (urn != null) {
      templateUrn = UrnUtils.getUrn(urn);
    } else {
      final String templateId = UUID.randomUUID().toString();
      final OrreryPageTemplateKey key = new OrreryPageTemplateKey().setId(templateId);
      templateUrn =
          EntityKeyUtils.convertEntityKeyToUrn(key, Constants.ORRERY_PAGE_TEMPLATE_ENTITY_NAME);
    }
    final AuditStamp nowAuditStamp = opContext.getAuditStamp();

    // 2. Build Page Template Properties
    OrreryPageTemplateProperties properties = new OrreryPageTemplateProperties();
    OrreryPageTemplateProperties existingProperties =
        getPageTemplateProperties(opContext, templateUrn);

    // Prevent scope-hijacking: a low-privileged user must not be able to overwrite a GLOBAL
    // template by supplying its URN with scope=PERSONAL in the request. Check the persisted scope,
    // not the caller-supplied scope.
    if (existingProperties != null
        && existingProperties.getVisibility() != null
        && PageTemplateScope.GLOBAL.equals(existingProperties.getVisibility().getScope())
        && !AuthUtil.isAuthorized(opContext, PoliciesConfig.MANAGE_HOME_PAGE_TEMPLATES_PRIVILEGE)) {
      throw new UnauthorizedException(
          String.format(
              "User is unauthorized to modify global page template with urn %s", templateUrn));
    }

    // Prevent privilege escalation: creating or re-scoping a template to GLOBAL also requires the
    // manage privilege.
    if (PageTemplateScope.GLOBAL.equals(scope)
        && !AuthUtil.isAuthorized(opContext, PoliciesConfig.MANAGE_HOME_PAGE_TEMPLATES_PRIVILEGE)) {
      throw new UnauthorizedException(
          "User is unauthorized to create or modify global page templates.");
    }

    if (existingProperties != null) {
      properties = existingProperties;
    } else {
      // if creating a new page template, set the created stamp
      properties.setCreated(nowAuditStamp);
    }

    checkModulesExistInRows(opContext, rows);

    properties.setRows(new OrreryPageTemplateRowArray(rows));

    if (assetSummary != null) {
      properties.setAssetSummary(assetSummary);
    }

    OrreryPageTemplateSurface surface = new OrreryPageTemplateSurface();
    surface.setSurfaceType(surfaceType);
    properties.setSurface(surface);

    OrreryPageTemplateVisibility visibility = new OrreryPageTemplateVisibility();
    visibility.setScope(scope);
    properties.setVisibility(visibility);

    properties.setLastModified(nowAuditStamp);

    // 4. Write changes to GMS
    try {
      final List<MetadataChangeProposal> aspectsToIngest = new ArrayList<>();
      aspectsToIngest.add(
          AspectUtils.buildMetadataChangeProposal(
              templateUrn, Constants.ORRERY_PAGE_TEMPLATE_PROPERTIES_ASPECT_NAME, properties));
      entityClient.batchIngestProposals(opContext, aspectsToIngest, false);
    } catch (Exception e) {
      throw new RuntimeException(
          String.format("Failed to upsert PageTemplate with urn %s", templateUrn), e);
    }
    return templateUrn;
  }

  @Nullable
  public OrreryPageTemplateProperties getPageTemplateProperties(
      @Nonnull OperationContext opContext, @Nonnull final Urn templateUrn) {
    Objects.requireNonNull(templateUrn, "templateUrn must not be null");
    final EntityResponse response = getPageTemplateEntityResponse(opContext, templateUrn);
    if (response != null
        && response
            .getAspects()
            .containsKey(Constants.ORRERY_PAGE_TEMPLATE_PROPERTIES_ASPECT_NAME)) {
      return new OrreryPageTemplateProperties(
          response
              .getAspects()
              .get(Constants.ORRERY_PAGE_TEMPLATE_PROPERTIES_ASPECT_NAME)
              .getValue()
              .data());
    }
    // No aspect found
    return null;
  }

  @Nullable
  public EntityResponse getPageTemplateEntityResponse(
      @Nonnull OperationContext opContext, @Nonnull final Urn templateUrn) {
    try {
      return entityClient.getV2(
          opContext, Constants.ORRERY_PAGE_TEMPLATE_ENTITY_NAME, templateUrn, null, false);
    } catch (Exception e) {
      throw new RuntimeException(
          String.format("Failed to retrieve PageTemplate with urn %s", templateUrn), e);
    }
  }

  private void checkModulesExistInRows(
      @Nonnull OperationContext opContext, @Nonnull final List<OrreryPageTemplateRow> rows) {
    rows.forEach(
        row -> {
          row.getModules()
              .forEach(
                  module -> {
                    try {
                      if (!entityClient.exists(opContext, module)) {
                        throw new RuntimeException(
                            String.format("Module with urn %s does not exist", module));
                      }
                    } catch (Exception e) {
                      throw new RuntimeException("Failed validating template rows", e);
                    }
                  });
        });
  }

  /**
   * Deletes a Orrery page template.
   *
   * @param opContext the operation context
   * @param templateUrn the URN of the page template to delete
   */
  public void deletePageTemplate(
      @Nonnull OperationContext opContext, @Nonnull final Urn templateUrn) {
    Objects.requireNonNull(templateUrn, "templateUrn must not be null");

    try {
      checkDeleteTemplatePermissions(opContext, templateUrn);

      final var references = entityClient.deleteEntityThenReferences(opContext, templateUrn);

      // Asynchronously delete all references to the entity (to return quickly)
      CompletableFuture.runAsync(
          () -> {
            try {
              references.run();
            } catch (Exception e) {
              log.error(
                  String.format(
                      "Caught exception while attempting to clear all entity references for PageTemplate with urn %s",
                      templateUrn),
                  e);
            }
          });

    } catch (Exception e) {
      throw new RuntimeException(
          String.format("Failed to delete PageTemplate with urn %s", templateUrn), e);
    }
  }

  /**
   * Ensures that a page template exists, and uses the page template properties to determine if the
   * user can delete this template. PERSONAL templates can only be deleted by the user that created
   * them. GLOBAL templates can only be deleted by those with the manage privilege.
   */
  public void checkDeleteTemplatePermissions(
      @Nonnull OperationContext opContext, @Nonnull final Urn templateUrn) {

    if (Objects.equals(templateUrn.toString(), DEFAULT_HOME_PAGE_TEMPLATE_URN)) {
      throw new UnauthorizedException("Attempted to delete the default page template");
    }

    OrreryPageTemplateProperties properties = getPageTemplateProperties(opContext, templateUrn);

    if (properties == null) {
      throw new IllegalArgumentException(
          String.format(
              "Attempted to delete a page template that does not exist with urn %s", templateUrn));
    }

    if (properties.getVisibility().getScope().equals(PageTemplateScope.GLOBAL)
        && !AuthUtil.isAuthorized(opContext, PoliciesConfig.MANAGE_HOME_PAGE_TEMPLATES_PRIVILEGE)) {
      throw new UnauthorizedException("User is unauthorized to delete global templates.");
    }

    if (properties.getVisibility().getScope().equals(PageTemplateScope.PERSONAL)
        && !properties
            .getCreated()
            .getActor()
            .toString()
            .equals(opContext.getSessionAuthentication().getActor().toUrnStr())) {
      throw new UnauthorizedException(
          "Attempted to delete personal a page template that was not created by the actor");
    }
  }
}
