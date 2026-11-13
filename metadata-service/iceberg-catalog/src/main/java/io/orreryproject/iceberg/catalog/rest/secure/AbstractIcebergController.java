package io.orreryproject.iceberg.catalog.rest.secure;

import static com.linkedin.metadata.Constants.*;
import static io.orreryproject.iceberg.catalog.Utils.*;

import com.orrery.authentication.Authentication;
import com.orrery.authentication.AuthenticationContext;
import com.orrery.authorization.AuthUtil;
import com.orrery.authorization.EntitySpec;
import com.orrery.plugins.auth.authorization.Authorizer;
import com.linkedin.common.urn.DatasetUrn;
import com.linkedin.common.urn.Urn;
import com.linkedin.metadata.authorization.PoliciesConfig;
import com.linkedin.metadata.entity.EntityService;
import com.linkedin.metadata.search.EntitySearchService;
import com.linkedin.metadata.search.client.CacheEvictionService;
import io.orreryproject.iceberg.catalog.OrreryIcebergWarehouse;
import io.orreryproject.iceberg.catalog.OrreryRestCatalog;
import io.orreryproject.iceberg.catalog.DataOperation;
import io.orreryproject.iceberg.catalog.credentials.CredentialProvider;
import io.orreryproject.metadata.context.OperationContext;
import io.orreryproject.metadata.context.RequestContext;
import io.orreryproject.metadata.context.usage.UsageOperation;
import io.orreryproject.metadata.services.SecretService;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.iceberg.catalog.Namespace;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.exceptions.ForbiddenException;
import org.apache.iceberg.exceptions.NoSuchNamespaceException;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
public class AbstractIcebergController {
  @Autowired protected EntityService entityService;
  @Autowired private EntitySearchService searchService;
  @Autowired private SecretService secretService;
  @Autowired private CacheEvictionService cacheEvictionService;

  @Inject
  @Named("cachingCredentialProvider")
  private CredentialProvider cachingCredentialProvider;

  @Inject
  @Named("authorizerChain")
  private Authorizer authorizer;

  @Inject
  @Named("systemOperationContext")
  protected OperationContext systemOperationContext;

  protected PoliciesConfig.Privilege authorize(
      OperationContext operationContext,
      OrreryIcebergWarehouse warehouse,
      TableIdentifier tableIdentifier,
      DataOperation operation,
      boolean returnHighestPrivilege) {
    Optional<DatasetUrn> urn = warehouse.getDatasetUrn(tableIdentifier);
    if (urn.isEmpty()) {
      throw noSuchEntityException(warehouse.getPlatformInstance(), tableIdentifier);
    }

    EntitySpec entitySpec = new EntitySpec(DATASET_ENTITY_NAME, urn.get().toString());

    try {
      return authorize(
          operationContext,
          entitySpec,
          platformInstanceEntitySpec(warehouse.getPlatformInstance()),
          operation,
          returnHighestPrivilege);
    } catch (ForbiddenException e) {
      // specify table id in error message instead of dataset-urn
      throw new ForbiddenException(
          "Data operation %s not authorized on %s",
          operation, fullTableName(warehouse.getPlatformInstance(), tableIdentifier));
    }
  }

  protected PoliciesConfig.Privilege authorize(
      OperationContext operationContext,
      OrreryIcebergWarehouse warehouse,
      Namespace namespace,
      DataOperation operation,
      boolean returnHighestPrivilege) {

    if (!entityService.exists(
        operationContext, containerUrn(warehouse.getPlatformInstance(), namespace))) {
      throw new NoSuchNamespaceException("Namespace does not exist: " + namespace);
    }

    EntitySpec entitySpec =
        new EntitySpec(
            CONTAINER_ENTITY_NAME,
            containerUrn(warehouse.getPlatformInstance(), namespace).toString());
    try {
      return authorize(
          operationContext,
          entitySpec,
          platformInstanceEntitySpec(warehouse.getPlatformInstance()),
          operation,
          returnHighestPrivilege);
    } catch (ForbiddenException e) {
      // specify namespace in error message instead of container-urn
      throw new ForbiddenException("Data operation %s not authorized on %s", operation, namespace);
    }
  }

  protected PoliciesConfig.Privilege authorize(
      OperationContext operationContext,
      String platformInstance,
      DataOperation operation,
      boolean returnHighestPrivilege) {
    EntitySpec entitySpec = platformInstanceEntitySpec(platformInstance);
    return authorize(operationContext, entitySpec, entitySpec, operation, returnHighestPrivilege);
  }

  private EntitySpec platformInstanceEntitySpec(String platformInstance) {
    Urn urn = platformInstanceUrn(platformInstance);
    return new EntitySpec(DATA_PLATFORM_INSTANCE_ENTITY_NAME, urn.toString());
  }

  private PoliciesConfig.Privilege authorize(
      OperationContext operationContext,
      EntitySpec entitySpec,
      EntitySpec platformInstanceEntitySpec,
      DataOperation operation,
      boolean returnHighestPrivilege) {
    List<PoliciesConfig.Privilege> privileges =
        returnHighestPrivilege ? operation.descendingPrivileges : operation.ascendingPrivileges;

    for (PoliciesConfig.Privilege privilege : privileges) {
      if (AuthUtil.isAuthorized(operationContext, privilege, entitySpec)
          || AuthUtil.isAuthorized(operationContext, privilege, platformInstanceEntitySpec)) {
        return privilege;
      }
    }

    throw new ForbiddenException("Data operation %s not authorized on %s", operation, entitySpec);
  }

  @Data
  @AllArgsConstructor
  protected static class CatalogOperationResult<R> {
    private R response;
    private PoliciesConfig.Privilege privilege;
    private CredentialProvider.StorageProviderCredentials storageProviderCredentials;
  }

  protected <R> R catalogOperation(
      String platformInstance, Function<OrreryRestCatalog, R> function) {
    OrreryIcebergWarehouse warehouse =
        OrreryIcebergWarehouse.of(
            platformInstance,
            entityService,
            secretService,
            cacheEvictionService,
            systemOperationContext);
    return catalogOperation(warehouse, systemOperationContext, function);
  }

  protected <R> R catalogOperation(
      OrreryIcebergWarehouse warehouse,
      OperationContext operationContext,
      Function<OrreryRestCatalog, R> function) {
    OrreryRestCatalog catalog = catalog(operationContext, warehouse);
    try {
      return function.apply(catalog);
    } finally {
      try {
        catalog.close();
      } catch (IOException e) {
        log.error("Error while closing catalog", e);
      }
    }
  }

  protected OperationContext opContext(HttpServletRequest request, UsageOperation usageOperation) {
    Authentication auth = AuthenticationContext.getAuthentication();
    return OperationContext.asSession(
        systemOperationContext,
        RequestContext.builder()
            .buildOpenapi(auth.getActor().toUrnStr(), request, "icebergDataAction", "dataset")
            .withUsageOperation(usageOperation),
        authorizer,
        auth,
        true);
  }

  /** Public Iceberg endpoints use the system context (same as master). */
  protected OperationContext publicOpContext() {
    return systemOperationContext;
  }

  protected OrreryRestCatalog catalog(
      OperationContext operationContext, OrreryIcebergWarehouse warehouse) {
    OrreryRestCatalog catalog =
        new OrreryRestCatalog(
            entityService, searchService, operationContext, warehouse, cachingCredentialProvider);
    return catalog;
  }

  protected OrreryIcebergWarehouse warehouse(
      String platformInstance, OperationContext operationContext) {
    return OrreryIcebergWarehouse.of(
        platformInstance, entityService, secretService, cacheEvictionService, operationContext);
  }

  protected RuntimeException noSuchEntityException(
      String platformInstance, TableIdentifier tableIdentifier) {
    throw new UnsupportedOperationException();
  }
}
