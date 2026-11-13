package com.orrery.graphql;

import com.orrery.authentication.Authentication;
import com.orrery.plugins.auth.authorization.Authorizer;
import com.linkedin.common.urn.Urn;
import com.linkedin.orrery.graphql.AspectLoadContext;
import com.linkedin.orrery.graphql.AspectMappingRegistry;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.context.RelationshipTraversalContext;
import com.linkedin.metadata.config.OrreryAppConfiguration;
import com.linkedin.metadata.config.GraphQLConfiguration;
import com.linkedin.metadata.config.graphql.GraphQLQueryConfiguration;
import com.linkedin.metadata.ratelimit.GraphqlDocumentMetadata;
import io.orreryproject.metadata.context.OperationContext;
import io.orreryproject.metadata.context.RequestContext;
import io.orreryproject.metadata.context.graphql.GraphqlUsageClassificationRegistry;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import lombok.Getter;

@Getter
public class SpringQueryContext implements QueryContext {

  private final boolean isAuthenticated;
  private final Authentication authentication;
  private final Authorizer authorizer;
  @Getter private final String queryName;
  @Nonnull private final OperationContext operationContext;
  @Nonnull private final OrreryAppConfiguration orreryAppConfig;
  @Nonnull private final RelationshipTraversalContext relationshipTraversalContext;
  private final int maxParentDepth;

  @Nullable private AspectMappingRegistry aspectMappingRegistry;
  private final ConcurrentHashMap<String, AspectLoadContext> aspectLoadContexts =
      new ConcurrentHashMap<>();
  private final ConcurrentHashMap<Urn, Boolean> domainExistenceCache = new ConcurrentHashMap<>();

  public SpringQueryContext(
      final boolean isAuthenticated,
      final Authentication authentication,
      final Authorizer authorizer,
      @Nonnull final OperationContext systemOperationContext,
      @Nonnull final OrreryAppConfiguration orreryAppConfig,
      @Nonnull final HttpServletRequest request,
      @Nonnull final GraphqlDocumentMetadata documentMetadata,
      Map<String, Object> variables,
      @Nonnull GraphqlUsageClassificationRegistry graphqlUsageClassificationRegistry) {
    this.isAuthenticated = isAuthenticated;
    this.authentication = authentication;
    this.authorizer = authorizer;
    this.queryName = documentMetadata.resolvedOperationName();

    GraphQLConfiguration graphQL =
        Objects.requireNonNull(
            orreryAppConfig.getGraphQL(),
            "graphQL configuration is required; define graphQL in application.yaml");
    GraphQLQueryConfiguration queryConfig =
        Objects.requireNonNull(
            graphQL.getQuery(),
            "graphQL.query configuration is required; define graphQL.query in application.yaml");
    this.relationshipTraversalContext =
        new RelationshipTraversalContext(queryConfig.getMaxVisitedUrns());
    this.maxParentDepth = queryConfig.getMaxParentDepth();

    GraphqlRequestUsageClassifier.Result classification =
        GraphqlRequestUsageClassifier.classify(
            documentMetadata, graphqlUsageClassificationRegistry);

    this.operationContext =
        OperationContext.asSession(
            systemOperationContext,
            RequestContext.builder()
                .buildGraphql(authentication.getActor().toUrnStr(), request, queryName, variables)
                .withUsageOperation(classification.usageOperation())
                .graphqlOperationKind(classification.kind()),
            authorizer,
            authentication,
            true);

    this.orreryAppConfig = orreryAppConfig;
  }

  @Override
  public int getMaxParentDepth() {
    return maxParentDepth;
  }

  @Override
  public Optional<RelationshipTraversalContext> getRelationshipTraversalContext() {
    return Optional.of(relationshipTraversalContext);
  }

  @Override
  @Nullable
  public AspectMappingRegistry getAspectMappingRegistry() {
    return aspectMappingRegistry;
  }

  @Override
  public void setAspectMappingRegistry(@Nullable AspectMappingRegistry aspectMappingRegistry) {
    this.aspectMappingRegistry = aspectMappingRegistry;
  }

  @Override
  public void mergeAspectLoadContext(
      @Nonnull String entityTypeName, @Nonnull AspectLoadContext loadContext) {
    aspectLoadContexts.merge(entityTypeName, loadContext, AspectLoadContext::union);
  }

  @Override
  @Nullable
  public AspectLoadContext getAspectLoadContext(@Nonnull String entityTypeName) {
    return aspectLoadContexts.get(entityTypeName);
  }

  @Override
  public Map<Urn, Boolean> getDomainExistenceCache() {
    return domainExistenceCache;
  }
}
