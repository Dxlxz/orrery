package com.orrery.authorization.fieldresolverprovider;

import static org.mockito.Mockito.mock;
import static org.testng.Assert.assertEquals;

import com.orrery.authorization.EntitySpec;
import com.orrery.authorization.FieldResolver;
import io.orreryproject.metadata.context.OperationContext;
import java.util.Collections;
import java.util.concurrent.ExecutionException;
import org.testng.annotations.Test;

// Class-level @Test: Gradle's TestNG detection ignores method annotations on abstract classes, so
// without it a subclass that declares no @Test of its own is never run.
@Test
public abstract class EntityFieldResolverProviderBaseTest<T extends EntityFieldResolverProvider> {
  protected abstract T buildFieldResolverProvider();

  @Test
  public void testEmpty() throws ExecutionException, InterruptedException {
    assertEquals(
        buildFieldResolverProvider()
            .getFieldResolver(mock(OperationContext.class), new EntitySpec("dataset", ""))
            .getFieldValuesFuture()
            .get(),
        FieldResolver.getResolverFromValues(Collections.emptySet()).getFieldValuesFuture().get());
  }
}
