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
import com.linkedin.metadata.key.OrreryPageModuleKey;
import com.linkedin.metadata.utils.EntityKeyUtils;
import com.linkedin.module.OrreryPageModuleParams;
import com.linkedin.module.OrreryPageModuleProperties;
import com.linkedin.module.OrreryPageModuleType;
import com.linkedin.module.OrreryPageModuleVisibility;
import com.linkedin.module.PageModuleScope;
import com.linkedin.mxe.MetadataChangeProposal;
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
public class PageModuleService {
  private final EntityClient entityClient;

  // Default module URNs that cannot be deleted. Keep this in sync with homeV3/modules/constants.ts
  private static final List<String> DEFAULT_MODULE_URNS =
      List.of(
          "urn:li:orreryPageModule:your_assets",
          "urn:li:orreryPageModule:your_subscriptions",
          "urn:li:orreryPageModule:top_domains",
          "urn:li:orreryPageModule:assets",
          "urn:li:orreryPageModule:output_ports",
          "urn:li:orreryPageModule:child_hierarchy",
          "urn:li:orreryPageModule:data_products",
          "urn:li:orreryPageModule:sub_data_products",
          "urn:li:orreryPageModule:related_terms",
          "urn:li:orreryPageModule:platforms",
          "urn:li:orreryPageModule:lineage",
          "urn:li:orreryPageModule:columns",
          "urn:li:orreryPageModule:semantic_model_datasets",
          "urn:li:orreryPageModule:semantic_model_metrics",
          "urn:li:orreryPageModule:semantic_model_relationships",
          "urn:li:orreryPageModule:semantic_model_dimensions",
          "urn:li:orreryPageModule:ai_context",
          "urn:li:orreryPageModule:sql",
          "urn:li:orreryPageModule:related_metrics");

  public PageModuleService(@Nonnull EntityClient entityClient) {
    this.entityClient = entityClient;
  }

  /**
   * Upserts a Orrery page module. If the page module with the provided urn already exists, then it
   * will be overwritten.
   *
   * <p>Callers must hold MANAGE_HOME_PAGE_TEMPLATES_PRIVILEGE to create or modify GLOBAL-scoped
   * modules, or to modify any module whose existing scope is GLOBAL.
   *
   * @return the URN of the new page module.
   */
  public Urn upsertPageModule(
      @Nonnull OperationContext opContext,
      @Nullable final String urn,
      @Nonnull final String name,
      @Nonnull final OrreryPageModuleType type,
      @Nonnull final PageModuleScope scope,
      @Nonnull final OrreryPageModuleParams params) {
    Objects.requireNonNull(name, "name must not be null");
    Objects.requireNonNull(type, "type must not be null");
    Objects.requireNonNull(scope, "scope must not be null");
    Objects.requireNonNull(params, "params must not be null");

    // 1. Optionally generate new page module urn
    Urn moduleUrn = null;
    if (urn != null) {
      moduleUrn = UrnUtils.getUrn(urn);
    } else {
      final String moduleId = UUID.randomUUID().toString();
      final OrreryPageModuleKey key = new OrreryPageModuleKey().setId(moduleId);
      moduleUrn =
          EntityKeyUtils.convertEntityKeyToUrn(key, Constants.ORRERY_PAGE_MODULE_ENTITY_NAME);
    }
    final AuditStamp nowAuditStamp = opContext.getAuditStamp();

    // 2. Build Page Module Properties
    OrreryPageModuleProperties properties = new OrreryPageModuleProperties();
    OrreryPageModuleProperties existingProperties = getPageModuleProperties(opContext, moduleUrn);

    // Prevent scope-hijacking: a low-privileged user must not be able to overwrite a GLOBAL module
    // by supplying its URN with scope=PERSONAL in the request. Check the persisted scope, not the
    // caller-supplied scope.
    if (existingProperties != null
        && existingProperties.getVisibility() != null
        && PageModuleScope.GLOBAL.equals(existingProperties.getVisibility().getScope())
        && !AuthUtil.isAuthorized(opContext, PoliciesConfig.MANAGE_HOME_PAGE_TEMPLATES_PRIVILEGE)) {
      throw new UnauthorizedException(
          String.format(
              "User is unauthorized to modify global page module with urn %s", moduleUrn));
    }

    // Prevent privilege escalation: creating or re-scoping a module to GLOBAL also requires the
    // manage privilege.
    if (PageModuleScope.GLOBAL.equals(scope)
        && !AuthUtil.isAuthorized(opContext, PoliciesConfig.MANAGE_HOME_PAGE_TEMPLATES_PRIVILEGE)) {
      throw new UnauthorizedException(
          "User is unauthorized to create or modify global page modules.");
    }

    if (existingProperties != null) {
      properties = existingProperties;
    } else {
      // if creating a new page module, set the created stamp
      properties.setCreated(nowAuditStamp);
    }

    properties.setName(name);
    properties.setType(type);

    OrreryPageModuleVisibility visibility = new OrreryPageModuleVisibility();
    visibility.setScope(scope);
    properties.setVisibility(visibility);

    properties.setParams(params);
    properties.setLastModified(nowAuditStamp);

    // 3. Write changes to GMS
    try {
      final List<MetadataChangeProposal> aspectsToIngest = new ArrayList<>();
      aspectsToIngest.add(
          AspectUtils.buildMetadataChangeProposal(
              moduleUrn, Constants.ORRERY_PAGE_MODULE_PROPERTIES_ASPECT_NAME, properties));
      entityClient.batchIngestProposals(opContext, aspectsToIngest, false);
    } catch (Exception e) {
      throw new RuntimeException(
          String.format("Failed to upsert PageModule with urn %s", moduleUrn), e);
    }
    return moduleUrn;
  }

  @Nullable
  public OrreryPageModuleProperties getPageModuleProperties(
      @Nonnull OperationContext opContext, @Nonnull final Urn moduleUrn) {
    Objects.requireNonNull(moduleUrn, "moduleUrn must not be null");
    final EntityResponse response = getPageModuleEntityResponse(opContext, moduleUrn);
    if (response != null
        && response
            .getAspects()
            .containsKey(Constants.ORRERY_PAGE_MODULE_PROPERTIES_ASPECT_NAME)) {
      return new OrreryPageModuleProperties(
          response
              .getAspects()
              .get(Constants.ORRERY_PAGE_MODULE_PROPERTIES_ASPECT_NAME)
              .getValue()
              .data());
    }
    // No aspect found
    return null;
  }

  @Nullable
  public EntityResponse getPageModuleEntityResponse(
      @Nonnull OperationContext opContext, @Nonnull final Urn moduleUrn) {
    try {
      return entityClient.getV2(
          opContext, Constants.ORRERY_PAGE_MODULE_ENTITY_NAME, moduleUrn, null, false);
    } catch (Exception e) {
      throw new RuntimeException(
          String.format("Failed to retrieve PageModule with urn %s", moduleUrn), e);
    }
  }

  /**
   * Deletes a Orrery page module.
   *
   * @param opContext the operation context
   * @param moduleUrn the URN of the page module to delete
   */
  public void deletePageModule(@Nonnull OperationContext opContext, @Nonnull final Urn moduleUrn) {
    Objects.requireNonNull(moduleUrn, "moduleUrn must not be null");

    try {
      checkDeleteModulePermissions(opContext, moduleUrn);

      final var references = entityClient.deleteEntityThenReferences(opContext, moduleUrn);

      // Asynchronously delete all references to the entity (to return quickly)
      CompletableFuture.runAsync(
          () -> {
            try {
              references.run();
            } catch (Exception e) {
              log.error(
                  String.format(
                      "Caught exception while attempting to clear all entity references for PageModule with urn %s",
                      moduleUrn),
                  e);
            }
          });

    } catch (Exception e) {
      throw new RuntimeException(
          String.format("Failed to delete PageModule with urn %s", moduleUrn), e);
    }
  }

  /**
   * Ensures that a page module exists, and uses the page module properties to determine if the user
   * can delete this module. PERSONAL modules can only be deleted by the user that created them.
   * GLOBAL modules can only be deleted by those with the manage privilege. Default modules cannot
   * be deleted.
   */
  public void checkDeleteModulePermissions(
      @Nonnull OperationContext opContext, @Nonnull final Urn moduleUrn) {
    // Check if this is a default module that cannot be deleted
    if (DEFAULT_MODULE_URNS.contains(moduleUrn.toString())) {
      throw new IllegalArgumentException(
          String.format("Cannot delete default page module with urn %s", moduleUrn));
    }

    OrreryPageModuleProperties properties = getPageModuleProperties(opContext, moduleUrn);

    if (properties == null) {
      throw new IllegalArgumentException(
          String.format(
              "Attempted to delete a page module that does not exist with urn %s", moduleUrn));
    }

    if (properties.getVisibility().getScope().equals(PageModuleScope.GLOBAL)
        && !AuthUtil.isAuthorized(opContext, PoliciesConfig.MANAGE_HOME_PAGE_TEMPLATES_PRIVILEGE)) {
      throw new UnauthorizedException("User is unauthorized to delete global modules.");
    }

    if (properties.getVisibility().getScope().equals(PageModuleScope.PERSONAL)
        && !properties
            .getCreated()
            .getActor()
            .toString()
            .equals(opContext.getSessionAuthentication().getActor().toUrnStr())) {
      throw new UnauthorizedException(
          "Attempted to delete personal a page module that was not created by the actor");
    }
  }
}
