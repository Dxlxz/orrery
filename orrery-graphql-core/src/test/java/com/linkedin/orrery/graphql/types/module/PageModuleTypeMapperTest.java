package com.linkedin.orrery.graphql.types.module;

import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import com.linkedin.module.OrreryPageModuleType;
import org.testng.annotations.Test;

public class PageModuleTypeMapperTest {

  @Test
  public void testMapValidModuleType() {
    // Test mapping a valid module type
    OrreryPageModuleType gmsType = OrreryPageModuleType.LINK;

    com.linkedin.orrery.graphql.generated.OrreryPageModuleType result =
        PageModuleTypeMapper.map(gmsType);

    assertNotNull(result);
    assertEquals(result, com.linkedin.orrery.graphql.generated.OrreryPageModuleType.LINK);
  }

  @Test
  public void testMapAllValidModuleTypes() {
    // Test all the valid module types that exist in both enums
    OrreryPageModuleType[] gmsTypes = {
      OrreryPageModuleType.LINK,
      OrreryPageModuleType.RICH_TEXT,
      OrreryPageModuleType.ASSET_COLLECTION,
      OrreryPageModuleType.HIERARCHY,
      OrreryPageModuleType.OWNED_ASSETS,
      OrreryPageModuleType.DOMAINS,
      OrreryPageModuleType.ASSETS,
      OrreryPageModuleType.CHILD_HIERARCHY,
      OrreryPageModuleType.DATA_PRODUCTS,
      OrreryPageModuleType.RELATED_TERMS,
      OrreryPageModuleType.PLATFORMS,
      OrreryPageModuleType.COLUMNS,
      OrreryPageModuleType.UNKNOWN
    };

    for (OrreryPageModuleType gmsType : gmsTypes) {
      com.linkedin.orrery.graphql.generated.OrreryPageModuleType result =
          PageModuleTypeMapper.map(gmsType);

      assertNotNull(result);
      assertEquals(result.name(), gmsType.name());
    }
  }

  @Test
  public void testMapInvalidModuleTypeReturnsUnknown() {
    // Create a mock OrreryPageModuleType that returns a value not in the GraphQL enum
    OrreryPageModuleType mockType = mock(OrreryPageModuleType.class);
    when(mockType.toString()).thenReturn("INVALID_MODULE_TYPE_12345");

    // This should catch the IllegalArgumentException and return UNKNOWN
    com.linkedin.orrery.graphql.generated.OrreryPageModuleType result =
        PageModuleTypeMapper.map(mockType);

    assertNotNull(result);
    assertEquals(result, com.linkedin.orrery.graphql.generated.OrreryPageModuleType.UNKNOWN);
  }

  @Test
  public void testMapWithNullContextValidType() {
    // Test that the mapper works with null context (first parameter)
    OrreryPageModuleType gmsType = OrreryPageModuleType.RICH_TEXT;

    com.linkedin.orrery.graphql.generated.OrreryPageModuleType result =
        PageModuleTypeMapper.INSTANCE.apply(null, gmsType);

    assertNotNull(result);
    assertEquals(result, com.linkedin.orrery.graphql.generated.OrreryPageModuleType.RICH_TEXT);
  }

  @Test
  public void testMapWithNullContextInvalidType() {
    // Test that the mapper handles invalid type with null context
    OrreryPageModuleType mockType = mock(OrreryPageModuleType.class);
    when(mockType.toString()).thenReturn("NONEXISTENT_TYPE");

    com.linkedin.orrery.graphql.generated.OrreryPageModuleType result =
        PageModuleTypeMapper.INSTANCE.apply(null, mockType);

    assertNotNull(result);
    assertEquals(result, com.linkedin.orrery.graphql.generated.OrreryPageModuleType.UNKNOWN);
  }

  @Test
  public void testMapUnknownTypeExplicitly() {
    // Explicitly test that UNKNOWN maps to UNKNOWN
    OrreryPageModuleType gmsType = OrreryPageModuleType.UNKNOWN;

    com.linkedin.orrery.graphql.generated.OrreryPageModuleType result =
        PageModuleTypeMapper.map(gmsType);

    assertNotNull(result);
    assertEquals(result, com.linkedin.orrery.graphql.generated.OrreryPageModuleType.UNKNOWN);
  }

  @Test
  public void testStaticMapMethodUsesInstanceApply() {
    // Verify that the static map method uses the instance's apply method
    OrreryPageModuleType gmsType = OrreryPageModuleType.DOMAINS;

    com.linkedin.orrery.graphql.generated.OrreryPageModuleType result1 =
        PageModuleTypeMapper.map(gmsType);
    com.linkedin.orrery.graphql.generated.OrreryPageModuleType result2 =
        PageModuleTypeMapper.INSTANCE.apply(null, gmsType);

    assertEquals(result1, result2);
  }
}
