package com.linkedin.orrery.graphql.types.entitytype;

import static com.linkedin.metadata.utils.UnknownDataHandlings.handling;

import com.google.common.collect.ImmutableMap;
import com.linkedin.orrery.graphql.generated.EntityType;
import com.linkedin.metadata.Constants;
import java.util.Map;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;

/**
 * In this class we statically map "well-supported" entity types into a more usable Enum class
 * served by our GraphQL API.
 *
 * <p>When we add new entity types / entity urns, we MAY NEED to update this.
 *
 * <p>Note that we currently do not support mapping entities that fall outside of this set. If you
 * try to map an entity type without a corresponding enum symbol, the mapping WILL FAIL.
 */
public class EntityTypeUrnMapper {

  static final Map<String, String> ENTITY_NAME_TO_ENTITY_TYPE_URN =
      ImmutableMap.<String, String>builder()
          .put(Constants.DOMAIN_ENTITY_NAME, "urn:li:entityType:orrery.domain")
          .put(Constants.DATASET_ENTITY_NAME, "urn:li:entityType:orrery.dataset")
          .put(Constants.CORP_USER_ENTITY_NAME, "urn:li:entityType:orrery.corpuser")
          .put(Constants.CORP_GROUP_ENTITY_NAME, "urn:li:entityType:orrery.corpGroup")
          .put(Constants.DATA_PLATFORM_ENTITY_NAME, "urn:li:entityType:orrery.dataPlatform")
          .put(
              Constants.ER_MODEL_RELATIONSHIP_ENTITY_NAME,
              "urn:li:entityType:orrery.erModelRelationship")
          .put(Constants.DASHBOARD_ENTITY_NAME, "urn:li:entityType:orrery.dashboard")
          .put(Constants.NOTEBOOK_ENTITY_NAME, "urn:li:entityType:orrery.notebook")
          .put(Constants.CHART_ENTITY_NAME, "urn:li:entityType:orrery.chart")
          .put(Constants.DATA_CONTRACT_ENTITY_NAME, "urn:li:entityType:orrery.dataContract")
          .put(Constants.DATA_FLOW_ENTITY_NAME, "urn:li:entityType:orrery.dataFlow")
          .put(Constants.DATA_JOB_ENTITY_NAME, "urn:li:entityType:orrery.dataJob")
          .put(Constants.TAG_ENTITY_NAME, "urn:li:entityType:orrery.tag")
          .put(Constants.GLOSSARY_TERM_ENTITY_NAME, "urn:li:entityType:orrery.glossaryTerm")
          .put(Constants.GLOSSARY_NODE_ENTITY_NAME, "urn:li:entityType:orrery.glossaryNode")
          .put(Constants.CONTAINER_ENTITY_NAME, "urn:li:entityType:orrery.container")
          .put(Constants.ML_MODEL_ENTITY_NAME, "urn:li:entityType:orrery.mlModel")
          .put(Constants.ML_MODEL_GROUP_ENTITY_NAME, "urn:li:entityType:orrery.mlModelGroup")
          .put(Constants.ML_FEATURE_TABLE_ENTITY_NAME, "urn:li:entityType:orrery.mlFeatureTable")
          .put(Constants.ML_FEATURE_ENTITY_NAME, "urn:li:entityType:orrery.mlFeature")
          .put(Constants.ML_PRIMARY_KEY_ENTITY_NAME, "urn:li:entityType:orrery.mlPrimaryKey")
          .put(
              Constants.INGESTION_SOURCE_ENTITY_NAME,
              "urn:li:entityType:orrery.orreryIngestionSource")
          .put(
              Constants.EXECUTION_REQUEST_ENTITY_NAME,
              "urn:li:entityType:orrery.orreryExecutionRequest")
          .put(Constants.ASSERTION_ENTITY_NAME, "urn:li:entityType:orrery.assertion")
          .put(
              Constants.DATA_PROCESS_INSTANCE_ENTITY_NAME,
              "urn:li:entityType:orrery.dataProcessInstance")
          .put(
              Constants.DATA_PLATFORM_INSTANCE_ENTITY_NAME,
              "urn:li:entityType:orrery.dataPlatformInstance")
          .put(Constants.ACCESS_TOKEN_ENTITY_NAME, "urn:li:entityType:orrery.orreryAccessToken")
          .put(Constants.TEST_ENTITY_NAME, "urn:li:entityType:orrery.test")
          .put(Constants.POLICY_ENTITY_NAME, "urn:li:entityType:orrery.orreryPolicy")
          .put(Constants.ORRERY_ROLE_ENTITY_NAME, "urn:li:entityType:orrery.orreryRole")
          .put(Constants.POST_ENTITY_NAME, "urn:li:entityType:orrery.post")
          .put(Constants.SCHEMA_FIELD_ENTITY_NAME, "urn:li:entityType:orrery.schemaField")
          .put(Constants.ORRERY_VIEW_ENTITY_NAME, "urn:li:entityType:orrery.orreryView")
          .put(Constants.QUERY_ENTITY_NAME, "urn:li:entityType:orrery.query")
          .put(Constants.DATA_PRODUCT_ENTITY_NAME, "urn:li:entityType:orrery.dataProduct")
          .put(Constants.OWNERSHIP_TYPE_ENTITY_NAME, "urn:li:entityType:orrery.ownershipType")
          .put(Constants.INCIDENT_ENTITY_NAME, "urn:li:entityType:orrery.incident")
          .put(Constants.ROLE_ENTITY_NAME, "urn:li:entityType:orrery.role")
          .put(
              Constants.STRUCTURED_PROPERTY_ENTITY_NAME,
              "urn:li:entityType:orrery.structuredProperty")
          .put(Constants.FORM_ENTITY_NAME, "urn:li:entityType:orrery.form")
          .put(Constants.DATA_TYPE_ENTITY_NAME, "urn:li:entityType:orrery.dataType")
          .put(Constants.ENTITY_TYPE_ENTITY_NAME, "urn:li:entityType:orrery.entityType")
          .put(Constants.RESTRICTED_ENTITY_NAME, "urn:li:entityType:orrery.restricted")
          .put(
              Constants.BUSINESS_ATTRIBUTE_ENTITY_NAME,
              "urn:li:entityType:orrery.businessAttribute")
          .put(Constants.APPLICATION_ENTITY_NAME, "urn:li:entityType:orrery.application")
          .put(Constants.API_ENTITY_NAME, "urn:li:entityType:orrery.api")
          .put(Constants.REPOSITORY_ENTITY_NAME, "urn:li:entityType:orrery.repository")
          .put(Constants.DOCUMENT_ENTITY_NAME, "urn:li:entityType:orrery.document")
          .put(Constants.METRIC_ENTITY_NAME, "urn:li:entityType:orrery.metric")
          .put(Constants.SEMANTIC_MODEL_ENTITY_NAME, "urn:li:entityType:orrery.semanticModel")
          .build();

  private static final Map<String, String> ENTITY_TYPE_URN_TO_NAME =
      ENTITY_NAME_TO_ENTITY_TYPE_URN.entrySet().stream()
          .collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));

  private EntityTypeUrnMapper() {}

  public static String getName(String entityTypeUrn) {
    if (!ENTITY_TYPE_URN_TO_NAME.containsKey(entityTypeUrn)) {
      throw new IllegalArgumentException("Unknown entityTypeUrn: " + entityTypeUrn);
    }
    return ENTITY_TYPE_URN_TO_NAME.get(entityTypeUrn);
  }

  /*
   * Takes in a entityTypeUrn and returns a GraphQL EntityType by first mapping
   * the urn to the entity name, and then mapping the entity name to EntityType.
   */
  public static EntityType getEntityType(String entityTypeUrn) {
    if (!ENTITY_TYPE_URN_TO_NAME.containsKey(entityTypeUrn)) {
      throw new IllegalArgumentException("Unknown entityTypeUrn: " + entityTypeUrn);
    }
    final String entityName = ENTITY_TYPE_URN_TO_NAME.get(entityTypeUrn);
    return EntityTypeMapper.getType(entityName);
  }

  /**
   * Like {@link #getEntityType}, but returns {@link EntityType#OTHER} for an entity type urn this
   * build does not know. Use it when mapping stored data, which after a version rollback can name
   * entity types only a newer build registered. When unknown data isn't tolerated it is exactly
   * {@link #getEntityType}.
   */
  @Nonnull
  public static EntityType getEntityTypeOrOther(String entityTypeUrn) {
    if (!handling().toleratesUnknownEntityAspect()) {
      return getEntityType(entityTypeUrn);
    }
    return isValidEntityType(entityTypeUrn) ? getEntityType(entityTypeUrn) : EntityType.OTHER;
  }

  @Nonnull
  public static String getEntityTypeUrn(String name) {
    if (!ENTITY_NAME_TO_ENTITY_TYPE_URN.containsKey(name)) {
      throw new IllegalArgumentException("Unknown entity name: " + name);
    }
    return ENTITY_NAME_TO_ENTITY_TYPE_URN.get(name);
  }

  public static boolean isValidEntityType(String entityTypeUrn) {
    return ENTITY_TYPE_URN_TO_NAME.containsKey(entityTypeUrn);
  }
}
