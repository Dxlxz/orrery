package com.linkedin.metadata.orreryusage;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;

import org.testng.annotations.Test;

public class OrreryUsageEventTypeTest {

  @Test
  public void testGetTypeMethod() {
    // Test that getType method works for various event types
    assertEquals(
        OrreryUsageEventType.getType("PageViewEvent"), OrreryUsageEventType.PAGE_VIEW_EVENT);
    assertEquals(OrreryUsageEventType.getType("LogInEvent"), OrreryUsageEventType.LOG_IN_EVENT);
    assertEquals(
        OrreryUsageEventType.getType("WelcomeToOrreryModalViewEvent"),
        OrreryUsageEventType.WELCOME_TO_ORRERY_MODAL_VIEW_EVENT);

    // Test the new enum value
    assertEquals(
        OrreryUsageEventType.getType("ProductTourButtonClickEvent"),
        OrreryUsageEventType.PRODUCT_TOUR_BUTTON_CLICK_EVENT);

    // Test case insensitive matching
    assertEquals(
        OrreryUsageEventType.getType("producttourButtonClickEvent"),
        OrreryUsageEventType.PRODUCT_TOUR_BUTTON_CLICK_EVENT);

    // Test non-existent event type
    assertNull(OrreryUsageEventType.getType("NonExistentEvent"));
  }

  @Test
  public void testEnumValues() {
    // Test that enum values have correct type strings
    assertEquals(
        OrreryUsageEventType.PRODUCT_TOUR_BUTTON_CLICK_EVENT.getType(),
        "ProductTourButtonClickEvent");
    assertEquals(
        OrreryUsageEventType.WELCOME_TO_ORRERY_MODAL_VIEW_EVENT.getType(),
        "WelcomeToOrreryModalViewEvent");
    assertEquals(
        OrreryUsageEventType.WELCOME_TO_ORRERY_MODAL_INTERACT_EVENT.getType(),
        "WelcomeToOrreryModalInteractEvent");
    assertEquals(
        OrreryUsageEventType.WELCOME_TO_ORRERY_MODAL_EXIT_EVENT.getType(),
        "WelcomeToOrreryModalExitEvent");
    assertEquals(
        OrreryUsageEventType.WELCOME_TO_ORRERY_MODAL_CLICK_VIEW_DOCUMENTATION_EVENT.getType(),
        "WelcomeToOrreryModalClickViewDocumentationEvent");
  }

  @Test
  public void testAllEnumValuesHaveTypes() {
    // Ensure all enum values have non-null type strings
    for (OrreryUsageEventType eventType : OrreryUsageEventType.values()) {
      assertNotNull(eventType.getType(), "Event type should not be null for " + eventType.name());
      // Verify that getType method can find each enum value
      assertEquals(OrreryUsageEventType.getType(eventType.getType()), eventType);
    }
  }
}
