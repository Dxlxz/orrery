package com.linkedin.orrery.graphql.resolvers.connection;

import static com.linkedin.orrery.graphql.TestUtils.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertThrows;

import com.google.common.collect.ImmutableMap;
import com.linkedin.common.DataPlatformInstance;
import com.linkedin.common.urn.Urn;
import com.linkedin.common.urn.UrnUtils;
import com.linkedin.connection.OrreryConnectionDetails;
import com.linkedin.connection.OrreryJsonConnection;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.OrreryConnection;
import com.linkedin.orrery.graphql.generated.OrreryConnectionDetailsType;
import com.linkedin.orrery.graphql.generated.OrreryJsonConnectionInput;
import com.linkedin.orrery.graphql.generated.EntityType;
import com.linkedin.orrery.graphql.generated.UpsertOrreryConnectionInput;
import com.linkedin.entity.Aspect;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.EnvelopedAspect;
import com.linkedin.entity.EnvelopedAspectMap;
import com.linkedin.metadata.Constants;
import com.linkedin.metadata.connection.ConnectionService;
import graphql.schema.DataFetchingEnvironment;
import io.orreryproject.metadata.context.OperationContext;
import io.orreryproject.metadata.services.SecretService;
import java.util.concurrent.CompletionException;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class UpsertConnectionResolverTest {

  private ConnectionService connectionService;
  private SecretService secretService;
  private UpsertConnectionResolver resolver;

  @BeforeMethod
  public void setUp() {
    connectionService = Mockito.mock(ConnectionService.class);
    secretService = Mockito.mock(SecretService.class);
    Mockito.when(secretService.encrypt(Mockito.any(), Mockito.eq("{}"))).thenReturn("encrypted");
    Mockito.when(secretService.decrypt(Mockito.any(), Mockito.eq("encrypted"))).thenReturn("{}");
    resolver = new UpsertConnectionResolver(connectionService, secretService);
  }

  @Test
  public void testGetAuthorized() throws Exception {
    // Mock inputs
    Urn connectionUrn = UrnUtils.getUrn("urn:li:orreryConnection:test-id");
    Urn platformUrn = UrnUtils.getUrn("urn:li:dataPlatform:slack");

    final UpsertOrreryConnectionInput input = new UpsertOrreryConnectionInput();
    input.setId(connectionUrn.getId());
    input.setPlatformUrn(platformUrn.toString());
    input.setType(OrreryConnectionDetailsType.JSON);
    input.setName("test-name");
    input.setJson(new OrreryJsonConnectionInput("{}"));

    QueryContext mockContext = getMockAllowContext();
    DataFetchingEnvironment mockEnv = Mockito.mock(DataFetchingEnvironment.class);
    Mockito.when(mockEnv.getArgument(Mockito.eq("input"))).thenReturn(input);
    Mockito.when(mockEnv.getContext()).thenReturn(mockContext);

    final OrreryConnectionDetails details =
        new OrreryConnectionDetails()
            .setType(com.linkedin.connection.OrreryConnectionDetailsType.JSON)
            .setJson(new OrreryJsonConnection().setEncryptedBlob("encrypted"));

    final DataPlatformInstance platformInstance =
        new DataPlatformInstance().setPlatform(platformUrn);

    when(connectionService.upsertConnection(
            any(OperationContext.class),
            Mockito.eq(input.getId()),
            Mockito.eq(platformUrn),
            Mockito.eq(details.getType()),
            Mockito.eq(details.getJson()),
            Mockito.any(String.class)))
        .thenReturn(connectionUrn);
    when(connectionService.getConnectionEntityResponse(
            any(OperationContext.class), Mockito.eq(connectionUrn)))
        .thenReturn(
            new EntityResponse()
                .setUrn(connectionUrn)
                .setEntityName(Constants.ORRERY_CONNECTION_ENTITY_NAME)
                .setAspects(
                    new EnvelopedAspectMap(
                        ImmutableMap.of(
                            Constants.ORRERY_CONNECTION_DETAILS_ASPECT_NAME,
                            new EnvelopedAspect()
                                .setName(Constants.ORRERY_CONNECTION_DETAILS_ASPECT_NAME)
                                .setValue(new Aspect(details.data())),
                            Constants.DATA_PLATFORM_INSTANCE_ASPECT_NAME,
                            new EnvelopedAspect()
                                .setName(Constants.DATA_PLATFORM_INSTANCE_ASPECT_NAME)
                                .setValue(new Aspect(platformInstance.data()))))));

    OrreryConnection actual = resolver.get(mockEnv).get();

    Assert.assertEquals(actual.getType(), EntityType.ORRERY_CONNECTION);
    Assert.assertEquals(actual.getUrn(), connectionUrn.toString());
    Assert.assertEquals(actual.getPlatform().getUrn(), platformUrn.toString());
    Assert.assertEquals(actual.getDetails().getType(), input.getType());
    Assert.assertEquals(actual.getDetails().getJson().getBlob(), input.getJson().getBlob());
  }

  @Test
  public void testGetUnAuthorized() {
    // Mock inputs
    Urn connectionUrn = UrnUtils.getUrn("urn:li:orreryConnection:test-id");

    final UpsertOrreryConnectionInput input = new UpsertOrreryConnectionInput();
    input.setId(connectionUrn.getId());
    input.setPlatformUrn(connectionUrn.toString());
    input.setType(OrreryConnectionDetailsType.JSON);

    QueryContext mockContext = getMockAllowContext();
    DataFetchingEnvironment mockEnv = Mockito.mock(DataFetchingEnvironment.class);
    Mockito.when(mockEnv.getArgument(Mockito.eq("input"))).thenReturn(input);
    Mockito.when(mockEnv.getContext()).thenReturn(mockContext);

    assertThrows(CompletionException.class, () -> resolver.get(mockEnv).join());
  }
}
