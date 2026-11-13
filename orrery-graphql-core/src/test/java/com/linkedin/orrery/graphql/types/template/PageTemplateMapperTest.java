package com.linkedin.orrery.graphql.types.template;

import static org.testng.Assert.*;

import com.linkedin.common.AuditStamp;
import com.linkedin.common.UrnArray;
import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.orrery.graphql.generated.OrreryPageModule;
import com.linkedin.orrery.graphql.generated.OrreryPageTemplate;
import com.linkedin.orrery.graphql.generated.OrreryPageTemplateAssetSummary;
import com.linkedin.orrery.graphql.generated.EntityType;
import com.linkedin.orrery.graphql.generated.StructuredPropertyEntity;
import com.linkedin.orrery.graphql.generated.SummaryElement;
import com.linkedin.orrery.graphql.generated.SummaryElementType;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.EnvelopedAspect;
import com.linkedin.entity.EnvelopedAspectMap;
import com.linkedin.metadata.utils.TolerantHandling;
import com.linkedin.metadata.utils.UnknownDataHandlingsForTests;
import com.linkedin.template.OrreryPageTemplateProperties;
import com.linkedin.template.OrreryPageTemplateRow;
import com.linkedin.template.OrreryPageTemplateRowArray;
import com.linkedin.template.OrreryPageTemplateSurface;
import com.linkedin.template.OrreryPageTemplateVisibility;
import com.linkedin.template.SummaryElementArray;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PageTemplateMapperTest {

  @BeforeMethod
  public void tolerateUnknownDataByDefault() {
    UnknownDataHandlingsForTests.use(TolerantHandling.INSTANCE);
  }

  @AfterMethod
  public void resetUnknownDataHandling() {
    UnknownDataHandlingsForTests.reset();
  }

  @Test
  public void testPageTemplateMapper() {
    // Create test data
    Urn templateUrn = UrnUtils.getUrn("urn:li:orreryPageTemplate:test-template");
    Urn moduleUrn = UrnUtils.getUrn("urn:li:orreryPageModule:test-module");

    // Create GMS template properties
    OrreryPageTemplateProperties gmsProperties = new OrreryPageTemplateProperties();

    // Create rows with modules
    OrreryPageTemplateRow row = new OrreryPageTemplateRow();
    row.setModules(new UrnArray(moduleUrn));
    gmsProperties.setRows(new OrreryPageTemplateRowArray(row));

    // Create surface
    OrreryPageTemplateSurface surface = new OrreryPageTemplateSurface();
    surface.setSurfaceType(com.linkedin.template.PageTemplateSurfaceType.HOME_PAGE);
    gmsProperties.setSurface(surface);

    // Create visibility
    OrreryPageTemplateVisibility visibility = new OrreryPageTemplateVisibility();
    visibility.setScope(com.linkedin.template.PageTemplateScope.GLOBAL);
    gmsProperties.setVisibility(visibility);

    // Create audit stamps
    AuditStamp created = new AuditStamp();
    created.setTime(System.currentTimeMillis());
    created.setActor(UrnUtils.getUrn("urn:li:corpuser:testuser"));
    gmsProperties.setCreated(created);

    AuditStamp lastModified = new AuditStamp();
    lastModified.setTime(System.currentTimeMillis());
    lastModified.setActor(UrnUtils.getUrn("urn:li:corpuser:testuser"));
    gmsProperties.setLastModified(lastModified);

    // Create EntityResponse
    EntityResponse entityResponse = new EntityResponse();
    entityResponse.setUrn(templateUrn);

    EnvelopedAspectMap aspectMap = new EnvelopedAspectMap();
    EnvelopedAspect aspect = new EnvelopedAspect();
    aspect.setValue(new com.linkedin.entity.Aspect(gmsProperties.data()));
    aspectMap.put("orreryPageTemplateProperties", aspect);
    entityResponse.setAspects(aspectMap);

    // Test mapping
    OrreryPageTemplate result = PageTemplateMapper.map(null, entityResponse);

    // Verify results
    assertNotNull(result);
    assertEquals(result.getUrn(), templateUrn.toString());
    assertEquals(result.getType(), EntityType.ORRERY_PAGE_TEMPLATE);

    assertNotNull(result.getProperties());
    assertNotNull(result.getProperties().getRows());
    assertEquals(result.getProperties().getRows().size(), 1);

    com.linkedin.orrery.graphql.generated.OrreryPageTemplateRow resultRow =
        result.getProperties().getRows().get(0);
    assertNotNull(resultRow.getModules());
    assertEquals(resultRow.getModules().size(), 1);

    OrreryPageModule resultModule = resultRow.getModules().get(0);
    assertEquals(resultModule.getUrn(), moduleUrn.toString());
    assertEquals(resultModule.getType(), EntityType.ORRERY_PAGE_MODULE);

    // Verify surface
    assertNotNull(result.getProperties().getSurface());
    assertEquals(
        result.getProperties().getSurface().getSurfaceType(),
        com.linkedin.orrery.graphql.generated.PageTemplateSurfaceType.HOME_PAGE);

    // Verify visibility
    assertNotNull(result.getProperties().getVisibility());
    assertEquals(
        result.getProperties().getVisibility().getScope(),
        com.linkedin.orrery.graphql.generated.PageTemplateScope.GLOBAL);

    // Verify audit stamps
    assertNotNull(result.getProperties().getCreated());
    assertEquals(result.getProperties().getCreated().getTime(), created.getTime());
    assertNotNull(result.getProperties().getCreated().getActor());
    assertEquals(
        result.getProperties().getCreated().getActor().getUrn(), created.getActor().toString());

    assertNotNull(result.getProperties().getLastModified());
    assertEquals(result.getProperties().getLastModified().getTime(), lastModified.getTime());
    assertNotNull(result.getProperties().getLastModified().getActor());
    assertEquals(
        result.getProperties().getLastModified().getActor().getUrn(),
        lastModified.getActor().toString());
  }

  @Test
  public void testPageTemplateMapperWithoutRequiredAspect() {
    // Create test entity response
    Urn templateUrn = UrnUtils.getUrn("urn:li:orreryPageTemplate:test-template");
    EntityResponse entityResponse = new EntityResponse();
    entityResponse.setUrn(templateUrn);
    EnvelopedAspectMap aspectMap = new EnvelopedAspectMap();
    entityResponse.setAspects(aspectMap);

    // Test mapping
    OrreryPageTemplate result = PageTemplateMapper.map(null, entityResponse);

    // Verify result
    assertNull(result);
  }

  @Test
  public void testPageTemplateMapperWithAssetSummary() {
    // Create test data
    Urn templateUrn = UrnUtils.getUrn("urn:li:orreryPageTemplate:test-template");
    Urn moduleUrn = UrnUtils.getUrn("urn:li:orreryPageModule:test-module");
    Urn structuredPropertyUrn = UrnUtils.getUrn("urn:li:structuredProperty:test-property");

    // Create GMS template properties
    OrreryPageTemplateProperties gmsProperties = new OrreryPageTemplateProperties();

    // Create rows with modules
    OrreryPageTemplateRow row = new OrreryPageTemplateRow();
    row.setModules(new UrnArray(moduleUrn));
    gmsProperties.setRows(new OrreryPageTemplateRowArray(row));

    // Create asset summary with various element types
    com.linkedin.template.OrreryPageTemplateAssetSummary gmsAssetSummary =
        new com.linkedin.template.OrreryPageTemplateAssetSummary();

    // Create summary elements
    SummaryElementArray summaryElements = new SummaryElementArray();

    // Add a CREATED element
    com.linkedin.template.SummaryElement createdElement =
        new com.linkedin.template.SummaryElement();
    createdElement.setElementType(com.linkedin.template.SummaryElementType.CREATED);
    summaryElements.add(createdElement);

    // Add a TAGS element
    com.linkedin.template.SummaryElement tagsElement = new com.linkedin.template.SummaryElement();
    tagsElement.setElementType(com.linkedin.template.SummaryElementType.TAGS);
    summaryElements.add(tagsElement);

    // Add a STRUCTURED_PROPERTY element
    com.linkedin.template.SummaryElement structuredPropertyElement =
        new com.linkedin.template.SummaryElement();
    structuredPropertyElement.setElementType(
        com.linkedin.template.SummaryElementType.STRUCTURED_PROPERTY);
    structuredPropertyElement.setStructuredPropertyUrn(structuredPropertyUrn);
    summaryElements.add(structuredPropertyElement);

    // An element type only a newer version knows (read after a rollback) is left out.
    com.linkedin.template.SummaryElement newerElement = new com.linkedin.template.SummaryElement();
    newerElement.data().put("elementType", "ELEMENT_FROM_NEWER_BUILD");
    summaryElements.add(newerElement);

    gmsAssetSummary.setSummaryElements(summaryElements);
    gmsProperties.setAssetSummary(gmsAssetSummary);

    // Create surface
    OrreryPageTemplateSurface surface = new OrreryPageTemplateSurface();
    surface.setSurfaceType(com.linkedin.template.PageTemplateSurfaceType.ASSET_SUMMARY);
    gmsProperties.setSurface(surface);

    // Create visibility
    OrreryPageTemplateVisibility visibility = new OrreryPageTemplateVisibility();
    visibility.setScope(com.linkedin.template.PageTemplateScope.GLOBAL);
    gmsProperties.setVisibility(visibility);

    // Create audit stamps
    AuditStamp created = new AuditStamp();
    created.setTime(System.currentTimeMillis());
    created.setActor(UrnUtils.getUrn("urn:li:corpuser:testuser"));
    gmsProperties.setCreated(created);

    AuditStamp lastModified = new AuditStamp();
    lastModified.setTime(System.currentTimeMillis());
    lastModified.setActor(UrnUtils.getUrn("urn:li:corpuser:testuser"));
    gmsProperties.setLastModified(lastModified);

    // Create EntityResponse
    EntityResponse entityResponse = new EntityResponse();
    entityResponse.setUrn(templateUrn);

    EnvelopedAspectMap aspectMap = new EnvelopedAspectMap();
    EnvelopedAspect aspect = new EnvelopedAspect();
    aspect.setValue(new com.linkedin.entity.Aspect(gmsProperties.data()));
    aspectMap.put("orreryPageTemplateProperties", aspect);
    entityResponse.setAspects(aspectMap);

    // Test mapping
    OrreryPageTemplate result = PageTemplateMapper.map(null, entityResponse);

    // Verify results
    assertNotNull(result);
    assertEquals(result.getUrn(), templateUrn.toString());
    assertEquals(result.getType(), EntityType.ORRERY_PAGE_TEMPLATE);

    assertNotNull(result.getProperties());
    assertNotNull(result.getProperties().getAssetSummary());

    OrreryPageTemplateAssetSummary assetSummary = result.getProperties().getAssetSummary();
    assertNotNull(assetSummary.getSummaryElements());
    assertEquals(assetSummary.getSummaryElements().size(), 3);

    // Verify CREATED element
    SummaryElement mappedCreatedElement = assetSummary.getSummaryElements().get(0);
    assertEquals(mappedCreatedElement.getElementType(), SummaryElementType.CREATED);
    assertNull(mappedCreatedElement.getStructuredProperty());

    // Verify TAGS element
    SummaryElement mappedTagsElement = assetSummary.getSummaryElements().get(1);
    assertEquals(mappedTagsElement.getElementType(), SummaryElementType.TAGS);
    assertNull(mappedTagsElement.getStructuredProperty());

    // Verify STRUCTURED_PROPERTY element
    SummaryElement mappedStructuredPropertyElement = assetSummary.getSummaryElements().get(2);
    assertEquals(
        mappedStructuredPropertyElement.getElementType(), SummaryElementType.STRUCTURED_PROPERTY);
    assertNotNull(mappedStructuredPropertyElement.getStructuredProperty());

    StructuredPropertyEntity mappedStructuredProperty =
        mappedStructuredPropertyElement.getStructuredProperty();
    assertEquals(mappedStructuredProperty.getUrn(), structuredPropertyUrn.toString());
    assertEquals(mappedStructuredProperty.getType(), EntityType.STRUCTURED_PROPERTY);

    // Verify surface type is ASSET_SUMMARY
    assertNotNull(result.getProperties().getSurface());
    assertEquals(
        result.getProperties().getSurface().getSurfaceType(),
        com.linkedin.orrery.graphql.generated.PageTemplateSurfaceType.ASSET_SUMMARY);
  }

  @Test
  public void testPageTemplateMapperWithNullAssetSummary() {
    // Create test data
    Urn templateUrn = UrnUtils.getUrn("urn:li:orreryPageTemplate:test-template");
    Urn moduleUrn = UrnUtils.getUrn("urn:li:orreryPageModule:test-module");

    // Create GMS template properties without asset summary
    OrreryPageTemplateProperties gmsProperties = new OrreryPageTemplateProperties();

    // Create rows with modules
    OrreryPageTemplateRow row = new OrreryPageTemplateRow();
    row.setModules(new UrnArray(moduleUrn));
    gmsProperties.setRows(new OrreryPageTemplateRowArray(row));

    // Create surface
    OrreryPageTemplateSurface surface = new OrreryPageTemplateSurface();
    surface.setSurfaceType(com.linkedin.template.PageTemplateSurfaceType.HOME_PAGE);
    gmsProperties.setSurface(surface);

    // Create visibility
    OrreryPageTemplateVisibility visibility = new OrreryPageTemplateVisibility();
    visibility.setScope(com.linkedin.template.PageTemplateScope.GLOBAL);
    gmsProperties.setVisibility(visibility);

    // Create audit stamps
    AuditStamp created = new AuditStamp();
    created.setTime(System.currentTimeMillis());
    created.setActor(UrnUtils.getUrn("urn:li:corpuser:testuser"));
    gmsProperties.setCreated(created);

    AuditStamp lastModified = new AuditStamp();
    lastModified.setTime(System.currentTimeMillis());
    lastModified.setActor(UrnUtils.getUrn("urn:li:corpuser:testuser"));
    gmsProperties.setLastModified(lastModified);

    // Create EntityResponse
    EntityResponse entityResponse = new EntityResponse();
    entityResponse.setUrn(templateUrn);

    EnvelopedAspectMap aspectMap = new EnvelopedAspectMap();
    EnvelopedAspect aspect = new EnvelopedAspect();
    aspect.setValue(new com.linkedin.entity.Aspect(gmsProperties.data()));
    aspectMap.put("orreryPageTemplateProperties", aspect);
    entityResponse.setAspects(aspectMap);

    // Test mapping
    OrreryPageTemplate result = PageTemplateMapper.map(null, entityResponse);

    // Verify results
    assertNotNull(result);
    assertEquals(result.getUrn(), templateUrn.toString());
    assertEquals(result.getType(), EntityType.ORRERY_PAGE_TEMPLATE);

    assertNotNull(result.getProperties());
    // Asset summary should be null when not provided
    assertNull(result.getProperties().getAssetSummary());
  }

  @Test
  public void testPageTemplateMapperWithEmptyAssetSummary() {
    // Create test data
    Urn templateUrn = UrnUtils.getUrn("urn:li:orreryPageTemplate:test-template");
    Urn moduleUrn = UrnUtils.getUrn("urn:li:orreryPageModule:test-module");

    // Create GMS template properties
    OrreryPageTemplateProperties gmsProperties = new OrreryPageTemplateProperties();

    // Create rows with modules
    OrreryPageTemplateRow row = new OrreryPageTemplateRow();
    row.setModules(new UrnArray(moduleUrn));
    gmsProperties.setRows(new OrreryPageTemplateRowArray(row));

    // Create empty asset summary
    com.linkedin.template.OrreryPageTemplateAssetSummary gmsAssetSummary =
        new com.linkedin.template.OrreryPageTemplateAssetSummary();
    // Don't set summary elements (null)
    gmsProperties.setAssetSummary(gmsAssetSummary);

    // Create surface
    OrreryPageTemplateSurface surface = new OrreryPageTemplateSurface();
    surface.setSurfaceType(com.linkedin.template.PageTemplateSurfaceType.ASSET_SUMMARY);
    gmsProperties.setSurface(surface);

    // Create visibility
    OrreryPageTemplateVisibility visibility = new OrreryPageTemplateVisibility();
    visibility.setScope(com.linkedin.template.PageTemplateScope.GLOBAL);
    gmsProperties.setVisibility(visibility);

    // Create audit stamps
    AuditStamp created = new AuditStamp();
    created.setTime(System.currentTimeMillis());
    created.setActor(UrnUtils.getUrn("urn:li:corpuser:testuser"));
    gmsProperties.setCreated(created);

    AuditStamp lastModified = new AuditStamp();
    lastModified.setTime(System.currentTimeMillis());
    lastModified.setActor(UrnUtils.getUrn("urn:li:corpuser:testuser"));
    gmsProperties.setLastModified(lastModified);

    // Create EntityResponse
    EntityResponse entityResponse = new EntityResponse();
    entityResponse.setUrn(templateUrn);

    EnvelopedAspectMap aspectMap = new EnvelopedAspectMap();
    EnvelopedAspect aspect = new EnvelopedAspect();
    aspect.setValue(new com.linkedin.entity.Aspect(gmsProperties.data()));
    aspectMap.put("orreryPageTemplateProperties", aspect);
    entityResponse.setAspects(aspectMap);

    // Test mapping
    OrreryPageTemplate result = PageTemplateMapper.map(null, entityResponse);

    // Verify results
    assertNotNull(result);
    assertEquals(result.getUrn(), templateUrn.toString());
    assertEquals(result.getType(), EntityType.ORRERY_PAGE_TEMPLATE);

    assertNotNull(result.getProperties());
    assertNotNull(result.getProperties().getAssetSummary());

    OrreryPageTemplateAssetSummary assetSummary = result.getProperties().getAssetSummary();
    assertNotNull(assetSummary.getSummaryElements());
    assertEquals(assetSummary.getSummaryElements().size(), 0);
  }

  @Test
  public void testPageTemplateMapperWithAllSummaryElementTypes() {
    // Create test data
    Urn templateUrn = UrnUtils.getUrn("urn:li:orreryPageTemplate:test-template");
    Urn moduleUrn = UrnUtils.getUrn("urn:li:orreryPageModule:test-module");
    Urn structuredPropertyUrn = UrnUtils.getUrn("urn:li:structuredProperty:test-property");

    // Create GMS template properties
    OrreryPageTemplateProperties gmsProperties = new OrreryPageTemplateProperties();

    // Create rows with modules
    OrreryPageTemplateRow row = new OrreryPageTemplateRow();
    row.setModules(new UrnArray(moduleUrn));
    gmsProperties.setRows(new OrreryPageTemplateRowArray(row));

    // Create asset summary with all element types
    com.linkedin.template.OrreryPageTemplateAssetSummary gmsAssetSummary =
        new com.linkedin.template.OrreryPageTemplateAssetSummary();

    // Create summary elements for all types
    SummaryElementArray summaryElements = new SummaryElementArray();

    // Add all element types
    com.linkedin.template.SummaryElementType[] allTypes = {
      com.linkedin.template.SummaryElementType.CREATED,
      com.linkedin.template.SummaryElementType.TAGS,
      com.linkedin.template.SummaryElementType.GLOSSARY_TERMS,
      com.linkedin.template.SummaryElementType.OWNERS,
      com.linkedin.template.SummaryElementType.DOMAIN,
      com.linkedin.template.SummaryElementType.STRUCTURED_PROPERTY
    };

    for (com.linkedin.template.SummaryElementType elementType : allTypes) {
      com.linkedin.template.SummaryElement element = new com.linkedin.template.SummaryElement();
      element.setElementType(elementType);

      // Only set structured property URN for STRUCTURED_PROPERTY type
      if (elementType == com.linkedin.template.SummaryElementType.STRUCTURED_PROPERTY) {
        element.setStructuredPropertyUrn(structuredPropertyUrn);
      }

      summaryElements.add(element);
    }

    gmsAssetSummary.setSummaryElements(summaryElements);
    gmsProperties.setAssetSummary(gmsAssetSummary);

    // Create surface
    OrreryPageTemplateSurface surface = new OrreryPageTemplateSurface();
    surface.setSurfaceType(com.linkedin.template.PageTemplateSurfaceType.ASSET_SUMMARY);
    gmsProperties.setSurface(surface);

    // Create visibility
    OrreryPageTemplateVisibility visibility = new OrreryPageTemplateVisibility();
    visibility.setScope(com.linkedin.template.PageTemplateScope.GLOBAL);
    gmsProperties.setVisibility(visibility);

    // Create audit stamps
    AuditStamp created = new AuditStamp();
    created.setTime(System.currentTimeMillis());
    created.setActor(UrnUtils.getUrn("urn:li:corpuser:testuser"));
    gmsProperties.setCreated(created);

    AuditStamp lastModified = new AuditStamp();
    lastModified.setTime(System.currentTimeMillis());
    lastModified.setActor(UrnUtils.getUrn("urn:li:corpuser:testuser"));
    gmsProperties.setLastModified(lastModified);

    // Create EntityResponse
    EntityResponse entityResponse = new EntityResponse();
    entityResponse.setUrn(templateUrn);

    EnvelopedAspectMap aspectMap = new EnvelopedAspectMap();
    EnvelopedAspect aspect = new EnvelopedAspect();
    aspect.setValue(new com.linkedin.entity.Aspect(gmsProperties.data()));
    aspectMap.put("orreryPageTemplateProperties", aspect);
    entityResponse.setAspects(aspectMap);

    // Test mapping
    OrreryPageTemplate result = PageTemplateMapper.map(null, entityResponse);

    // Verify results
    assertNotNull(result);
    assertEquals(result.getUrn(), templateUrn.toString());
    assertEquals(result.getType(), EntityType.ORRERY_PAGE_TEMPLATE);

    assertNotNull(result.getProperties());
    assertNotNull(result.getProperties().getAssetSummary());

    OrreryPageTemplateAssetSummary assetSummary = result.getProperties().getAssetSummary();
    assertNotNull(assetSummary.getSummaryElements());
    assertEquals(assetSummary.getSummaryElements().size(), 6);

    // Verify all element types are mapped correctly
    SummaryElementType[] expectedTypes = {
      SummaryElementType.CREATED,
      SummaryElementType.TAGS,
      SummaryElementType.GLOSSARY_TERMS,
      SummaryElementType.OWNERS,
      SummaryElementType.DOMAIN,
      SummaryElementType.STRUCTURED_PROPERTY
    };

    for (int i = 0; i < expectedTypes.length; i++) {
      SummaryElement mappedElement = assetSummary.getSummaryElements().get(i);
      assertEquals(mappedElement.getElementType(), expectedTypes[i]);

      // Only STRUCTURED_PROPERTY should have a structured property entity
      if (expectedTypes[i] == SummaryElementType.STRUCTURED_PROPERTY) {
        assertNotNull(mappedElement.getStructuredProperty());
        assertEquals(
            mappedElement.getStructuredProperty().getUrn(), structuredPropertyUrn.toString());
        assertEquals(
            mappedElement.getStructuredProperty().getType(), EntityType.STRUCTURED_PROPERTY);
      } else {
        assertNull(mappedElement.getStructuredProperty());
      }
    }
  }
}
