package com.linkedin.orrery.graphql.types.template;

import static com.linkedin.orrery.graphql.TestUtils.getMockAllowContext;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

import com.linkedin.common.AuditStamp;
import com.linkedin.common.UrnArray;
import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.OrreryPageTemplate;
import com.linkedin.orrery.graphql.generated.EntityType;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.EnvelopedAspect;
import com.linkedin.entity.EnvelopedAspectMap;
import com.linkedin.entity.client.EntityClient;
import com.linkedin.r2.RemoteInvocationException;
import com.linkedin.template.OrreryPageTemplateProperties;
import com.linkedin.template.OrreryPageTemplateRow;
import com.linkedin.template.OrreryPageTemplateRowArray;
import com.linkedin.template.OrreryPageTemplateSurface;
import com.linkedin.template.OrreryPageTemplateVisibility;
import com.linkedin.template.PageTemplateScope;
import com.linkedin.template.PageTemplateSurfaceType;
import graphql.execution.DataFetcherResult;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PageTemplateTypeTest {

  private PageTemplateType pageTemplateType;
  private EntityClient mockEntityClient;
  private QueryContext mockQueryContext;

  @BeforeMethod
  public void setUp() {
    mockEntityClient = mock(EntityClient.class);
    mockQueryContext = getMockAllowContext();
    pageTemplateType = new PageTemplateType(mockEntityClient);
  }

  @Test
  public void testType() {
    assertEquals(pageTemplateType.type(), EntityType.ORRERY_PAGE_TEMPLATE);
  }

  @Test
  public void testObjectClass() {
    assertEquals(pageTemplateType.objectClass(), OrreryPageTemplate.class);
  }

  @Test
  public void testGetKeyProvider() {
    // Create a mock entity
    OrreryPageTemplate mockEntity = new OrreryPageTemplate();
    mockEntity.setUrn("urn:li:orreryPageTemplate:test");

    String result = pageTemplateType.getKeyProvider().apply(mockEntity);
    assertEquals(result, "urn:li:orreryPageTemplate:test");
  }

  @Test
  public void testBatchLoadSuccess() throws Exception {
    // Create test URNs
    String urn1 = "urn:li:orreryPageTemplate:test-template-1";
    String urn2 = "urn:li:orreryPageTemplate:test-template-2";
    List<String> urns = Arrays.asList(urn1, urn2);

    // Create mock entity responses
    EntityResponse response1 = createMockEntityResponse(urn1, "Test Template 1");
    EntityResponse response2 = createMockEntityResponse(urn2, "Test Template 2");

    Map<Urn, EntityResponse> entityMap = new HashMap<>();
    entityMap.put(UrnUtils.getUrn(urn1), response1);
    entityMap.put(UrnUtils.getUrn(urn2), response2);

    // Mock the entity client response
    when(mockEntityClient.batchGetV2(
            any(), eq("orreryPageTemplate"), any(), eq(PageTemplateType.ASPECTS_TO_FETCH)))
        .thenReturn(entityMap);

    // Execute batch load
    List<DataFetcherResult<OrreryPageTemplate>> results =
        pageTemplateType.batchLoad(urns, mockQueryContext);

    // Verify results
    assertEquals(results.size(), 2);

    DataFetcherResult<OrreryPageTemplate> result1 = results.get(0);
    assertNotNull(result1);
    assertNotNull(result1.getData());
    assertEquals(result1.getData().getUrn(), urn1);
    assertEquals(result1.getData().getType(), EntityType.ORRERY_PAGE_TEMPLATE);
    assertEquals(result1.getData().getProperties().getRows().size(), 1);

    DataFetcherResult<OrreryPageTemplate> result2 = results.get(1);
    assertNotNull(result2);
    assertNotNull(result2.getData());
    assertEquals(result2.getData().getUrn(), urn2);
    assertEquals(result2.getData().getType(), EntityType.ORRERY_PAGE_TEMPLATE);
    assertEquals(result2.getData().getProperties().getRows().size(), 1);
  }

  @Test
  public void testBatchLoadWithMissingEntities() throws Exception {
    // Create test URNs
    String urn1 = "urn:li:orreryPageTemplate:test-template-1";
    String urn2 = "urn:li:orreryPageTemplate:test-template-2";
    List<String> urns = Arrays.asList(urn1, urn2);

    // Create mock entity response for only one entity
    EntityResponse response1 = createMockEntityResponse(urn1, "Test Template 1");

    Map<Urn, EntityResponse> entityMap = new HashMap<>();
    entityMap.put(UrnUtils.getUrn(urn1), response1);
    // Note: urn2 is missing from the map

    // Mock the entity client response
    when(mockEntityClient.batchGetV2(
            any(), eq("orreryPageTemplate"), any(), eq(PageTemplateType.ASPECTS_TO_FETCH)))
        .thenReturn(entityMap);

    // Execute batch load
    List<DataFetcherResult<OrreryPageTemplate>> results =
        pageTemplateType.batchLoad(urns, mockQueryContext);

    // Verify results
    assertEquals(results.size(), 2);

    DataFetcherResult<OrreryPageTemplate> result1 = results.get(0);
    assertNotNull(result1);
    assertNotNull(result1.getData());
    assertEquals(result1.getData().getUrn(), urn1);

    DataFetcherResult<OrreryPageTemplate> result2 = results.get(1);
    assertNull(result2); // Missing entity should return null
  }

  @Test
  public void testBatchLoadEmptyList() throws Exception {
    List<String> urns = Collections.emptyList();

    // Mock the entity client response
    when(mockEntityClient.batchGetV2(
            any(), eq("orreryPageTemplate"), any(), eq(PageTemplateType.ASPECTS_TO_FETCH)))
        .thenReturn(Collections.emptyMap());

    // Execute batch load
    List<DataFetcherResult<OrreryPageTemplate>> results =
        pageTemplateType.batchLoad(urns, mockQueryContext);

    // Verify results
    assertEquals(results.size(), 0);
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void testBatchLoadException() throws Exception {
    // Create test URNs
    String urn1 = "urn:li:orreryPageTemplate:test-template-1";
    List<String> urns = Arrays.asList(urn1);

    // Mock the entity client to throw an exception
    when(mockEntityClient.batchGetV2(
            any(), eq("orreryPageTemplate"), any(), eq(PageTemplateType.ASPECTS_TO_FETCH)))
        .thenThrow(new RemoteInvocationException("Test exception"));

    // Execute batch load - should throw RuntimeException
    pageTemplateType.batchLoad(urns, mockQueryContext);
  }

  @Test
  public void testAspectsToFetch() {
    assertEquals(PageTemplateType.ASPECTS_TO_FETCH.size(), 1);
    assertTrue(PageTemplateType.ASPECTS_TO_FETCH.contains("orreryPageTemplateProperties"));
  }

  @Test
  public void testBatchLoadWithComplexTemplate() throws Exception {
    // Create test URNs
    String urn1 = "urn:li:orreryPageTemplate:complex-template";
    List<String> urns = Arrays.asList(urn1);

    // Create mock entity response with complex template data
    EntityResponse response1 = createComplexMockEntityResponse(urn1);

    Map<Urn, EntityResponse> entityMap = new HashMap<>();
    entityMap.put(UrnUtils.getUrn(urn1), response1);

    // Mock the entity client response
    when(mockEntityClient.batchGetV2(
            any(), eq("orreryPageTemplate"), any(), eq(PageTemplateType.ASPECTS_TO_FETCH)))
        .thenReturn(entityMap);

    // Execute batch load
    List<DataFetcherResult<OrreryPageTemplate>> results =
        pageTemplateType.batchLoad(urns, mockQueryContext);

    // Verify results
    assertEquals(results.size(), 1);

    DataFetcherResult<OrreryPageTemplate> result1 = results.get(0);
    assertNotNull(result1);
    assertNotNull(result1.getData());
    assertEquals(result1.getData().getUrn(), urn1);
    assertEquals(result1.getData().getType(), EntityType.ORRERY_PAGE_TEMPLATE);

    // Verify complex properties
    assertNotNull(result1.getData().getProperties().getSurface());
    assertEquals(
        result1.getData().getProperties().getSurface().getSurfaceType(),
        com.linkedin.orrery.graphql.generated.PageTemplateSurfaceType.HOME_PAGE);

    assertNotNull(result1.getData().getProperties().getVisibility());
    assertEquals(
        result1.getData().getProperties().getVisibility().getScope(),
        com.linkedin.orrery.graphql.generated.PageTemplateScope.GLOBAL);

    assertNotNull(result1.getData().getProperties().getCreated());
    assertNotNull(result1.getData().getProperties().getLastModified());
  }

  private EntityResponse createMockEntityResponse(String urn, String name) {
    // Create GMS properties
    OrreryPageTemplateProperties gmsProperties = new OrreryPageTemplateProperties();

    // Create rows with modules
    OrreryPageTemplateRow row = new OrreryPageTemplateRow();
    Urn moduleUrn = UrnUtils.getUrn("urn:li:orreryPageModule:test-module");
    UrnArray moduleUrns = new UrnArray();
    moduleUrns.add(moduleUrn);
    row.setModules(moduleUrns);

    OrreryPageTemplateRowArray rows = new OrreryPageTemplateRowArray();
    rows.add(row);
    gmsProperties.setRows(rows);

    // Create surface
    OrreryPageTemplateSurface surface = new OrreryPageTemplateSurface();
    surface.setSurfaceType(PageTemplateSurfaceType.HOME_PAGE);
    gmsProperties.setSurface(surface);

    // Create visibility
    OrreryPageTemplateVisibility visibility = new OrreryPageTemplateVisibility();
    visibility.setScope(PageTemplateScope.GLOBAL);
    gmsProperties.setVisibility(visibility);

    // Create audit stamps
    AuditStamp created = new AuditStamp();
    created.setTime(System.currentTimeMillis());
    created.setActor(UrnUtils.getUrn("urn:li:corpuser:test-user"));
    gmsProperties.setCreated(created);

    AuditStamp lastModified = new AuditStamp();
    lastModified.setTime(System.currentTimeMillis());
    lastModified.setActor(UrnUtils.getUrn("urn:li:corpuser:test-user"));
    gmsProperties.setLastModified(lastModified);

    // Create entity response
    EntityResponse entityResponse = new EntityResponse();
    entityResponse.setUrn(UrnUtils.getUrn(urn));

    EnvelopedAspectMap aspectMap = new EnvelopedAspectMap();
    EnvelopedAspect aspect = new EnvelopedAspect();
    aspect.setValue(new com.linkedin.entity.Aspect(gmsProperties.data()));
    aspectMap.put("orreryPageTemplateProperties", aspect);
    entityResponse.setAspects(aspectMap);

    return entityResponse;
  }

  private EntityResponse createComplexMockEntityResponse(String urn) {
    // Create GMS properties with more complex data
    OrreryPageTemplateProperties gmsProperties = new OrreryPageTemplateProperties();

    // Create multiple rows with multiple modules
    OrreryPageTemplateRowArray rows = new OrreryPageTemplateRowArray();

    // First row with one module
    OrreryPageTemplateRow row1 = new OrreryPageTemplateRow();
    UrnArray moduleUrns1 = new UrnArray();
    moduleUrns1.add(UrnUtils.getUrn("urn:li:orreryPageModule:module-1"));
    row1.setModules(moduleUrns1);
    rows.add(row1);

    // Second row with two modules
    OrreryPageTemplateRow row2 = new OrreryPageTemplateRow();
    UrnArray moduleUrns2 = new UrnArray();
    moduleUrns2.add(UrnUtils.getUrn("urn:li:orreryPageModule:module-2"));
    moduleUrns2.add(UrnUtils.getUrn("urn:li:orreryPageModule:module-3"));
    row2.setModules(moduleUrns2);
    rows.add(row2);

    gmsProperties.setRows(rows);

    // Create surface
    OrreryPageTemplateSurface surface = new OrreryPageTemplateSurface();
    surface.setSurfaceType(PageTemplateSurfaceType.HOME_PAGE);
    gmsProperties.setSurface(surface);

    // Create visibility
    OrreryPageTemplateVisibility visibility = new OrreryPageTemplateVisibility();
    visibility.setScope(PageTemplateScope.GLOBAL);
    gmsProperties.setVisibility(visibility);

    // Create audit stamps
    AuditStamp created = new AuditStamp();
    created.setTime(System.currentTimeMillis());
    created.setActor(UrnUtils.getUrn("urn:li:corpuser:test-user"));
    gmsProperties.setCreated(created);

    AuditStamp lastModified = new AuditStamp();
    lastModified.setTime(System.currentTimeMillis());
    lastModified.setActor(UrnUtils.getUrn("urn:li:corpuser:test-user"));
    gmsProperties.setLastModified(lastModified);

    // Create entity response
    EntityResponse entityResponse = new EntityResponse();
    entityResponse.setUrn(UrnUtils.getUrn(urn));

    EnvelopedAspectMap aspectMap = new EnvelopedAspectMap();
    EnvelopedAspect aspect = new EnvelopedAspect();
    aspect.setValue(new com.linkedin.entity.Aspect(gmsProperties.data()));
    aspectMap.put("orreryPageTemplateProperties", aspect);
    entityResponse.setAspects(aspectMap);

    return entityResponse;
  }
}
