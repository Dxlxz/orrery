package com.linkedin.orrery.graphql.resolvers.ingest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.testng.Assert.*;

import com.orrery.authorization.AuthorizationResult;
import com.orrery.authorization.EntitySpec;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.linkedin.common.urn.Urn;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.Secret;
import com.linkedin.ingestion.OrreryIngestionSourceConfig;
import com.linkedin.ingestion.OrreryIngestionSourceInfo;
import com.linkedin.ingestion.OrreryIngestionSourceSchedule;
import com.linkedin.metadata.Constants;
import com.linkedin.secret.OrrerySecretValue;
import io.orreryproject.metadata.context.OperationContext;
import org.mockito.Mockito;

public class IngestTestUtils {

  public static final Urn TEST_INGESTION_SOURCE_URN =
      Urn.createFromTuple(Constants.INGESTION_SOURCE_ENTITY_NAME, "test");
  public static final Urn TEST_SECRET_URN =
      Urn.createFromTuple(Constants.SECRETS_ENTITY_NAME, "TEST_SECRET");
  public static final Urn TEST_EXECUTION_REQUEST_URN =
      Urn.createFromTuple(Constants.EXECUTION_REQUEST_ENTITY_NAME, "1234");

  public static QueryContext getMockAllowContext() {
    QueryContext mockContext = Mockito.mock(QueryContext.class);
    Mockito.when(mockContext.getActorUrn()).thenReturn("urn:li:corpuser:test");

    when(mockContext.getOperationContext()).thenReturn(mock(OperationContext.class));
    when(mockContext.getOperationContext().authorize(any(), nullable(EntitySpec.class), any()))
        .thenReturn(new AuthorizationResult(null, AuthorizationResult.Type.ALLOW, ""));
    when(mockContext.getOperationContext().getObjectMapper()).thenReturn(new ObjectMapper());
    return mockContext;
  }

  public static QueryContext getMockDenyContext() {
    QueryContext mockContext = Mockito.mock(QueryContext.class);
    Mockito.when(mockContext.getActorUrn()).thenReturn("urn:li:corpuser:test");

    when(mockContext.getOperationContext()).thenReturn(mock(OperationContext.class));
    when(mockContext.getOperationContext().authorize(any(), nullable(EntitySpec.class), any()))
        .thenReturn(new AuthorizationResult(null, AuthorizationResult.Type.DENY, ""));
    return mockContext;
  }

  public static OrreryIngestionSourceInfo getTestIngestionSourceInfo() {
    OrreryIngestionSourceInfo info = new OrreryIngestionSourceInfo();
    info.setName("My Test Source");
    info.setType("mysql");
    info.setSchedule(
        new OrreryIngestionSourceSchedule().setTimezone("UTC").setInterval("* * * * *"));
    info.setConfig(
        new OrreryIngestionSourceConfig()
            .setVersion("0.8.18")
            .setRecipe("{}")
            .setExecutorId("executor id"));
    return info;
  }

  public static OrrerySecretValue getTestSecretValue() {
    OrrerySecretValue value = new OrrerySecretValue();
    value.setValue("encryptedvalue");
    value.setName(TEST_SECRET_URN.getId());
    value.setDescription("none");
    return value;
  }

  public static void verifyTestSecretGraphQL(Secret secret, OrrerySecretValue value) {
    assertEquals(secret.getUrn(), TEST_SECRET_URN.toString());
    assertEquals(secret.getName(), value.getName());
    assertEquals(secret.getDescription(), value.getDescription());
  }

  private IngestTestUtils() {}
}
