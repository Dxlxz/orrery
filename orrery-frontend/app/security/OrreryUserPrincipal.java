package security;

import java.security.Principal;

public class OrreryUserPrincipal implements Principal {
  private final String name;

  public OrreryUserPrincipal(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    OrreryUserPrincipal that = (OrreryUserPrincipal) o;
    return name.equals(that.name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public String toString() {
    return "OrreryUserPrincipal[" + name + "]";
  }
}
