package com.linkedin.orrery.graphql.resolvers.policy;

import static com.linkedin.orrery.graphql.resolvers.ResolverUtils.bindArgument;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;

import com.orrery.authorization.AuthorizerChain;
import com.orrery.authorization.OrreryAuthorizer;
import com.orrery.authorization.PolicyEngine;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.concurrency.GraphQLConcurrencyUtils;
import com.linkedin.orrery.graphql.exception.AuthorizationException;
import com.linkedin.orrery.graphql.generated.EntityType;
import com.linkedin.orrery.graphql.generated.GetGrantedPrivilegesInput;
import com.linkedin.orrery.graphql.generated.PolicyEvaluationDetail;
import com.linkedin.orrery.graphql.generated.Privileges;
import com.linkedin.orrery.graphql.generated.ResourceSpec;
import com.linkedin.orrery.graphql.resolvers.ResolverUtils;
import graphql.schema.DataFetchingEnvironment;
import io.orreryproject.metadata.context.OperationContext;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.mockito.MockedStatic;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class GetGrantedPrivilegesResolverTest {

  private GetGrantedPrivilegesResolver resolver;
  private DataFetchingEnvironment environment;
  private QueryContext context;
  private AuthorizerChain authorizerChain;
  private OrreryAuthorizer orreryAuthorizer;
  private OperationContext operationContext;
  private MockedStatic<ResolverUtils> resolverUtils;
  private MockedStatic<PolicyAuthUtils> policyAuthUtils;
  private MockedStatic<GraphQLConcurrencyUtils> concurrencyUtils;
  private static final String ACTOR_URN = "urn:li:corpuser:test";
  private static final String RESOURCE_URN = "urn:li:dataset:test";

  @BeforeMethod
  public void setup() {
    resolver = new GetGrantedPrivilegesResolver();
    environment = mock(DataFetchingEnvironment.class);
    context = mock(QueryContext.class);
    authorizerChain = mock(AuthorizerChain.class);
    orreryAuthorizer = mock(OrreryAuthorizer.class);
    operationContext = mock(OperationContext.class);
    resolverUtils = mockStatic(ResolverUtils.class);
    policyAuthUtils = mockStatic(PolicyAuthUtils.class);
    concurrencyUtils = mockStatic(GraphQLConcurrencyUtils.class);

    when(environment.getContext()).thenReturn(context);
    when(context.getAuthorizer()).thenReturn(authorizerChain);
    when(context.getOperationContext()).thenReturn(operationContext);
    when(authorizerChain.getDefaultAuthorizer()).thenReturn(orreryAuthorizer);
  }

  @AfterMethod
  public void tearDown() {
    resolverUtils.close();
    policyAuthUtils.close();
    concurrencyUtils.close();
  }

  @Test
  public void testGetGrantedPrivilegesWithAuthorizedUser() throws Exception {
    // Setup
    GetGrantedPrivilegesInput input = new GetGrantedPrivilegesInput();
    input.setActorUrn(ACTOR_URN);
    input.setIncludeEvaluationDetails(false);

    when(environment.getArgument("input")).thenReturn(input);
    when(context.getActorUrn()).thenReturn(ACTOR_URN);
    policyAuthUtils.when(() -> PolicyAuthUtils.canManagePolicies(context)).thenReturn(false);
    resolverUtils
        .when(() -> bindArgument(eq(input), eq(GetGrantedPrivilegesInput.class)))
        .thenReturn(input);

    List<String> privileges = List.of("READ");
    Map<String, String> denyReasons = new HashMap<>();
    PolicyEngine.PolicyGrantedPrivileges mockResult =
        mock(PolicyEngine.PolicyGrantedPrivileges.class);
    when(mockResult.getPrivileges()).thenReturn(privileges);
    when(mockResult.getReasonOfDeny()).thenReturn(denyReasons);
    when(orreryAuthorizer.getGrantedPrivileges(any(), any(), eq(operationContext)))
        .thenReturn(mockResult);

    Privileges expectedPrivileges =
        Privileges.builder().setPrivileges(privileges).setEvaluationDetails(null).build();

    concurrencyUtils
        .when(() -> GraphQLConcurrencyUtils.supplyAsync(any(), any(), any()))
        .thenAnswer(invocation -> CompletableFuture.completedFuture(expectedPrivileges));

    // Execute
    CompletableFuture<Privileges> result = resolver.get(environment);

    // Verify
    Privileges returnedPrivileges = result.get();
    assertEquals(returnedPrivileges.getPrivileges(), List.of("READ"));
    assertEquals(returnedPrivileges.getEvaluationDetails(), null);
  }

  @Test
  public void testGetGrantedPrivilegesWithResourceSpec() throws Exception {
    // Setup
    GetGrantedPrivilegesInput input = new GetGrantedPrivilegesInput();
    input.setActorUrn(ACTOR_URN);
    input.setIncludeEvaluationDetails(false);

    ResourceSpec resourceSpec = new ResourceSpec();
    resourceSpec.setResourceType(EntityType.DATASET);
    resourceSpec.setResourceUrn(RESOURCE_URN);
    input.setResourceSpec(resourceSpec);

    when(environment.getArgument("input")).thenReturn(input);
    when(context.getActorUrn()).thenReturn(ACTOR_URN);
    policyAuthUtils.when(() -> PolicyAuthUtils.canManagePolicies(context)).thenReturn(false);
    resolverUtils
        .when(() -> bindArgument(eq(input), eq(GetGrantedPrivilegesInput.class)))
        .thenReturn(input);

    List<String> privileges = List.of("READ");
    Map<String, String> denyReasons = new HashMap<>();
    PolicyEngine.PolicyGrantedPrivileges mockResult =
        mock(PolicyEngine.PolicyGrantedPrivileges.class);
    when(mockResult.getPrivileges()).thenReturn(privileges);
    when(mockResult.getReasonOfDeny()).thenReturn(denyReasons);
    when(orreryAuthorizer.getGrantedPrivileges(any(), any(), eq(operationContext)))
        .thenReturn(mockResult);

    Privileges expectedPrivileges =
        Privileges.builder().setPrivileges(privileges).setEvaluationDetails(null).build();

    concurrencyUtils
        .when(() -> GraphQLConcurrencyUtils.supplyAsync(any(), any(), any()))
        .thenAnswer(invocation -> CompletableFuture.completedFuture(expectedPrivileges));

    // Execute
    CompletableFuture<Privileges> result = resolver.get(environment);

    // Verify
    Privileges returnedPrivileges = result.get();
    assertEquals(returnedPrivileges.getPrivileges(), List.of("READ"));
  }

  @Test
  public void testGetGrantedPrivilegesWithEvaluationDetails() throws Exception {
    // Setup
    GetGrantedPrivilegesInput input = new GetGrantedPrivilegesInput();
    input.setActorUrn(ACTOR_URN);
    input.setIncludeEvaluationDetails(true);

    when(environment.getArgument("input")).thenReturn(input);
    when(context.getActorUrn()).thenReturn(ACTOR_URN);
    policyAuthUtils.when(() -> PolicyAuthUtils.canManagePolicies(context)).thenReturn(true);
    resolverUtils
        .when(() -> bindArgument(eq(input), eq(GetGrantedPrivilegesInput.class)))
        .thenReturn(input);

    List<String> privileges = List.of("READ");
    Map<String, String> denyReasons = new HashMap<>();
    denyReasons.put("policy1", "Access denied by policy1");
    PolicyEngine.PolicyGrantedPrivileges mockResult =
        mock(PolicyEngine.PolicyGrantedPrivileges.class);
    when(mockResult.getPrivileges()).thenReturn(privileges);
    when(mockResult.getReasonOfDeny()).thenReturn(denyReasons);
    when(orreryAuthorizer.getGrantedPrivileges(any(), any(), eq(operationContext)))
        .thenReturn(mockResult);

    List<PolicyEvaluationDetail> evaluationDetails = new ArrayList<>();
    evaluationDetails.add(new PolicyEvaluationDetail("policy1", "Access denied by policy1"));

    Privileges expectedPrivileges =
        Privileges.builder()
            .setPrivileges(privileges)
            .setEvaluationDetails(evaluationDetails)
            .build();

    concurrencyUtils
        .when(() -> GraphQLConcurrencyUtils.supplyAsync(any(), any(), any()))
        .thenAnswer(invocation -> CompletableFuture.completedFuture(expectedPrivileges));

    // Execute
    CompletableFuture<Privileges> result = resolver.get(environment);

    // Verify
    Privileges returnedPrivileges = result.get();
    assertEquals(returnedPrivileges.getPrivileges(), List.of("READ"));
    assertEquals(returnedPrivileges.getEvaluationDetails().size(), 1);
    PolicyEvaluationDetail detail = returnedPrivileges.getEvaluationDetails().get(0);
    assertEquals(detail.getPolicyName(), "policy1");
    assertEquals(detail.getReason(), "Access denied by policy1");
  }

  @Test
  public void testGetGrantedPrivilegesUnauthorized() {
    // Setup
    GetGrantedPrivilegesInput input = new GetGrantedPrivilegesInput();
    input.setActorUrn("urn:li:corpuser:different");
    when(environment.getArgument("input")).thenReturn(input);
    when(context.getActorUrn()).thenReturn(ACTOR_URN);
    policyAuthUtils.when(() -> PolicyAuthUtils.canManagePolicies(context)).thenReturn(false);
    resolverUtils
        .when(() -> bindArgument(eq(input), eq(GetGrantedPrivilegesInput.class)))
        .thenReturn(input);

    // Execute and Verify
    assertThrows(AuthorizationException.class, () -> resolver.get(environment));
  }

  @Test
  public void testGetGrantedPrivilegesUnsupportedAuthorizer() {
    // Setup
    GetGrantedPrivilegesInput input = new GetGrantedPrivilegesInput();
    input.setActorUrn(ACTOR_URN);
    when(environment.getArgument("input")).thenReturn(input);
    when(context.getActorUrn()).thenReturn(ACTOR_URN);
    when(context.getAuthorizer()).thenReturn(mock(OrreryAuthorizer.class));
    policyAuthUtils.when(() -> PolicyAuthUtils.canManagePolicies(context)).thenReturn(false);
    resolverUtils
        .when(() -> bindArgument(eq(input), eq(GetGrantedPrivilegesInput.class)))
        .thenReturn(input);

    // Execute and Verify
    assertThrows(UnsupportedOperationException.class, () -> resolver.get(environment));
  }
}
