package security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

public class OrreryUserPrincipalTest {

  @Test
  public void testGetName() {
    OrreryUserPrincipal principal = new OrreryUserPrincipal("testuser");
    assertEquals("testuser", principal.getName(), "Principal name should match constructor value");
  }

  @Test
  public void testEquals() {
    OrreryUserPrincipal principal1 = new OrreryUserPrincipal("testuser");
    OrreryUserPrincipal principal2 = new OrreryUserPrincipal("testuser");
    OrreryUserPrincipal principal3 = new OrreryUserPrincipal("otheruser");

    // Test equality with same name
    assertTrue(principal1.equals(principal2), "Principals with same name should be equal");
    assertTrue(principal2.equals(principal1), "Equals should be symmetric");

    // Test inequality with different name
    assertFalse(
        principal1.equals(principal3), "Principals with different names should not be equal");

    // Test with null and different object type
    assertFalse(principal1.equals(null), "Principal should not equal null");
    assertFalse(principal1.equals("testuser"), "Principal should not equal string with same name");

    // Test reflexivity
    assertTrue(principal1.equals(principal1), "Principal should equal itself");
  }

  @Test
  public void testHashCode() {
    OrreryUserPrincipal principal1 = new OrreryUserPrincipal("testuser");
    OrreryUserPrincipal principal2 = new OrreryUserPrincipal("testuser");

    // Test hash code consistency
    assertEquals(
        principal1.hashCode(),
        principal2.hashCode(),
        "Equal principals should have same hash code");

    // Test hash code is based on name
    assertEquals(
        "testuser".hashCode(),
        principal1.hashCode(),
        "Principal hash code should be based on name");
  }

  @Test
  public void testToString() {
    OrreryUserPrincipal principal = new OrreryUserPrincipal("testuser");
    String expectedString = "OrreryUserPrincipal[testuser]";

    assertEquals(
        expectedString, principal.toString(), "toString should return formatted principal name");
  }
}
