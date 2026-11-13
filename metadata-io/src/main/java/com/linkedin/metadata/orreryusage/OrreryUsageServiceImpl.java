package com.linkedin.metadata.orreryusage;

import static com.linkedin.metadata.Constants.ORRERY_USAGE_EVENT_INDEX;

import com.linkedin.metadata.config.search.SearchComponent;
import com.linkedin.metadata.orreryusage.event.EventSource;
import com.linkedin.metadata.orreryusage.event.LoginSource;
import com.linkedin.metadata.orreryusage.event.UsageEventResult;
import com.linkedin.metadata.search.elasticsearch.query.request.SearchAfterWrapper;
import com.linkedin.metadata.search.utils.SearchOperation;
import com.linkedin.metadata.search.utils.SearchTookMetrics;
import com.linkedin.metadata.utils.elasticsearch.IndexConvention;
import com.linkedin.metadata.utils.elasticsearch.SearchClientShim;
import com.linkedin.metadata.utils.elasticsearch.canonicalization.QueryTimeCanonicalizer.CanonicalNow;
import io.orreryproject.metadata.context.OperationContext;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.opensearch.action.search.SearchRequest;
import org.opensearch.action.search.SearchResponse;
import org.opensearch.client.RequestOptions;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.search.SearchHit;
import org.opensearch.search.SearchHits;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortBuilders;
import org.opensearch.search.sort.SortOrder;

@Slf4j
public class OrreryUsageServiceImpl implements OrreryUsageService {

  private final SearchClientShim<?> elasticClient;
  private final IndexConvention indexConvention;

  public OrreryUsageServiceImpl(
      SearchClientShim<?> elasticClient, IndexConvention indexConvention) {
    this.elasticClient = elasticClient;
    this.indexConvention = indexConvention;
  }

  @Override
  public String getUsageIndexName(@Nonnull OperationContext opContext) {
    return indexConvention.getIndexName(
        opContext, SearchComponent.USAGE, ORRERY_USAGE_EVENT_INDEX);
  }

  /** Searches the Orrery Usage index for backend tracing events */
  @Override
  public ExternalAuditEventsSearchResponse externalAuditEventsSearch(
      OperationContext opContext,
      ExternalAuditEventsSearchRequest externalAuditEventsSearchRequest) {
    BoolQueryBuilder filterQuery = QueryBuilders.boolQuery();
    filterQuery.filter(
        dateRangeQuery(
            opContext,
            externalAuditEventsSearchRequest.getStartTime(),
            externalAuditEventsSearchRequest.getEndTime()));
    if (CollectionUtils.isNotEmpty(externalAuditEventsSearchRequest.getEventTypes())) {
      filterQuery.filter(eventTypeFilterQuery(externalAuditEventsSearchRequest.getEventTypes()));
    }
    if (CollectionUtils.isNotEmpty(externalAuditEventsSearchRequest.getAspectTypes())) {
      filterQuery.filter(aspectNameFilterQuery(externalAuditEventsSearchRequest.getAspectTypes()));
    }
    if (CollectionUtils.isNotEmpty(externalAuditEventsSearchRequest.getEntityTypes())) {
      filterQuery.filter(entityTypeFilterQuery(externalAuditEventsSearchRequest.getEntityTypes()));
    }
    if (CollectionUtils.isNotEmpty(externalAuditEventsSearchRequest.getActorUrns())) {
      filterQuery.filter(actorUrnFilterQuery(externalAuditEventsSearchRequest.getActorUrns()));
    }
    filterQuery.filter(getBackendOnlyEvents());

    SearchRequest searchRequest = new SearchRequest(getUsageIndexName(opContext));
    SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
    searchSourceBuilder.size(externalAuditEventsSearchRequest.getSize());
    searchSourceBuilder.query(filterQuery);

    searchSourceBuilder.sort(
        SortBuilders.fieldSort(OrreryUsageEventConstants.TIMESTAMP).order(SortOrder.DESC));
    searchSourceBuilder.sort(
        SortBuilders.fieldSort(OrreryUsageEventConstants.TYPE).order(SortOrder.ASC));
    searchSourceBuilder.sort(
        SortBuilders.fieldSort(keywordField(OrreryUsageEventConstants.ACTOR_URN))
            .order(SortOrder.ASC));

    if (StringUtils.isNotBlank(externalAuditEventsSearchRequest.getScrollId())) {
      searchSourceBuilder.searchAfter(
          SearchAfterWrapper.fromScrollId(externalAuditEventsSearchRequest.getScrollId())
              .getSort());
    }

    searchRequest.source(searchSourceBuilder);
    SearchResponse response = executeAndExtractDocuments(opContext, searchRequest);
    return mapExternalAuditEventsSearchResponse(
        opContext, response, externalAuditEventsSearchRequest);
  }

  private ExternalAuditEventsSearchResponse mapExternalAuditEventsSearchResponse(
      OperationContext opContext,
      SearchResponse searchResponse,
      ExternalAuditEventsSearchRequest analyticsSearchRequest) {
    ExternalAuditEventsSearchResponse.ExternalAuditEventsSearchResponseBuilder response =
        ExternalAuditEventsSearchResponse.builder();
    SearchHits searchHits = searchResponse.getHits();
    response.count(searchHits.getHits().length);
    response.total((int) searchHits.getTotalHits().value);
    response.nextScrollId(
        SearchAfterWrapper.nextScrollId(searchHits.getHits(), analyticsSearchRequest.getSize()));
    List<UsageEventResult> usageEventResults = new ArrayList<>();
    for (SearchHit searchHit : searchHits.getHits()) {
      UsageEventResult usageEventResult = mapUsageEventResult(searchHit, opContext);
      usageEventResults.add(usageEventResult);
    }
    response.usageEvents(usageEventResults);
    return response.build();
  }

  @Nonnull
  private static UsageEventResult mapUsageEventResult(
      SearchHit searchHit, OperationContext opContext) {
    InternalUsageEventResult.InternalUsageEventResultBuilder usageEventResultBuilder =
        InternalUsageEventResult.builder();
    Map<String, Object> searchResultMap = searchHit.getSourceAsMap();
    usageEventResultBuilder.rawUsageEvent(new LinkedHashMap<>(searchResultMap));
    if (searchResultMap.get(OrreryUsageEventConstants.TYPE) instanceof String) {
      usageEventResultBuilder.eventType(
          (String) searchResultMap.get(OrreryUsageEventConstants.TYPE));
    }
    if (searchResultMap.get(OrreryUsageEventConstants.ACTOR_URN) instanceof String) {
      usageEventResultBuilder.actorUrn(
          (String) searchResultMap.get(OrreryUsageEventConstants.ACTOR_URN));
    }
    if (searchResultMap.get(OrreryUsageEventConstants.TIMESTAMP) instanceof Long) {
      usageEventResultBuilder.timestamp(
          (Long) searchResultMap.get(OrreryUsageEventConstants.TIMESTAMP));
    }
    if (searchResultMap.get(OrreryUsageEventConstants.SOURCE_IP) instanceof String) {
      usageEventResultBuilder.sourceIP(
          (String) searchResultMap.get(OrreryUsageEventConstants.SOURCE_IP));
    }
    if (searchResultMap.get(OrreryUsageEventConstants.EVENT_SOURCE) instanceof String) {
      usageEventResultBuilder.eventSource(
          EventSource.getSource(
              (String) searchResultMap.get(OrreryUsageEventConstants.EVENT_SOURCE)));
    }
    if (searchResultMap.get(OrreryUsageEventConstants.LOGIN_SOURCE) instanceof String) {
      usageEventResultBuilder.loginSource(
          LoginSource.getSource(
              (String) searchResultMap.get(OrreryUsageEventConstants.LOGIN_SOURCE)));
    }
    if (searchResultMap.get(OrreryUsageEventConstants.ENTITY_TYPE) instanceof String) {
      usageEventResultBuilder.entityType(
          (String) searchResultMap.get(OrreryUsageEventConstants.ENTITY_TYPE));
    }
    if (searchResultMap.get(OrreryUsageEventConstants.ENTITY_URN) instanceof String) {
      usageEventResultBuilder.entityUrn(
          (String) searchResultMap.get(OrreryUsageEventConstants.ENTITY_URN));
    }
    if (searchResultMap.get(OrreryUsageEventConstants.ASPECT_NAME) instanceof String) {
      usageEventResultBuilder.aspectName(
          (String) searchResultMap.get(OrreryUsageEventConstants.ASPECT_NAME));
    }
    if (searchResultMap.get(OrreryUsageEventConstants.TRACE_ID) instanceof String) {
      usageEventResultBuilder.telemetryTraceId(
          (String) searchResultMap.get(OrreryUsageEventConstants.TRACE_ID));
    }
    if (searchResultMap.get(OrreryUsageEventConstants.USER_AGENT) instanceof String) {
      usageEventResultBuilder.userAgent(
          (String) searchResultMap.get(OrreryUsageEventConstants.USER_AGENT));
    }
    InternalUsageEventResult usageEventResult = usageEventResultBuilder.build();
    // Identify subtype automatically so that serialization happens correctly
    return opContext.getObjectMapper().convertValue(usageEventResult, UsageEventResult.class);
  }

  private SearchResponse executeAndExtractDocuments(
      OperationContext opContext, SearchRequest searchRequest) {
    try {
      return SearchTookMetrics.time(
          opContext,
          SearchOperation.ORRERY_USAGE,
          () -> elasticClient.search(opContext, searchRequest, RequestOptions.DEFAULT));
    } catch (Exception e) {
      log.error(String.format("Search query failed: %s", e.getMessage()));
      throw new RuntimeException("Search query failed:", e);
    }
  }

  /**
   * Builds the event time window. Caller-supplied bounds are used verbatim; only the defaults,
   * which this service derives from the wall clock, are canonicalized. The clock is read once, and
   * only when a default is actually needed, so a fully-specified request neither reads it nor
   * records a canonicalization.
   */
  private QueryBuilder dateRangeQuery(@Nonnull OperationContext opContext, long start, long end) {
    if (start < 0 || end <= 0) {
      final CanonicalNow now = opContext.canonicalNow();
      if (start < 0) {
        start = Instant.ofEpochMilli(now.reference()).minus(1, ChronoUnit.DAYS).toEpochMilli();
      }
      if (end <= 0) {
        end = now.upperBound();
      }
    }
    return QueryBuilders.rangeQuery(OrreryUsageEventConstants.TIMESTAMP).gte(start).lt(end);
  }

  private QueryBuilder eventTypeFilterQuery(List<String> eventTypes) {
    return QueryBuilders.termsQuery(OrreryUsageEventConstants.TYPE, eventTypes);
  }

  private QueryBuilder aspectNameFilterQuery(List<String> aspectNames) {
    return QueryBuilders.termsQuery(
        keywordField(OrreryUsageEventConstants.ASPECT_NAME), aspectNames);
  }

  private QueryBuilder entityTypeFilterQuery(List<String> entityTypes) {
    return QueryBuilders.termsQuery(
        keywordField(OrreryUsageEventConstants.ENTITY_TYPE), entityTypes);
  }

  private QueryBuilder actorUrnFilterQuery(List<String> actorUrns) {
    return QueryBuilders.termsQuery(keywordField(OrreryUsageEventConstants.ACTOR_URN), actorUrns);
  }

  private QueryBuilder getBackendOnlyEvents() {
    return QueryBuilders.termQuery(
        keywordField(OrreryUsageEventConstants.USAGE_SOURCE),
        OrreryUsageEventConstants.BACKEND_SOURCE);
  }

  private static String keywordField(String field) {
    return field + ".keyword";
  }
}
