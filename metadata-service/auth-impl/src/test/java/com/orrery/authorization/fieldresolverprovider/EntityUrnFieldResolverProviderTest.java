package com.orrery.authorization.fieldresolverprovider;

public class EntityUrnFieldResolverProviderTest
    extends EntityFieldResolverProviderBaseTest<EntityUrnFieldResolverProvider> {
  @Override
  protected EntityUrnFieldResolverProvider buildFieldResolverProvider() {
    return new EntityUrnFieldResolverProvider();
  }
}
