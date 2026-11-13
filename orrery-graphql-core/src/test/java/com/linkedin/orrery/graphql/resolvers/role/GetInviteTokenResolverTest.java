package com.linkedin.orrery.graphql.resolvers.role;

import static com.linkedin.orrery.graphql.TestUtils.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import com.orrery.authentication.Authentication;
import com.orrery.authentication.invite.InviteTokenAuthorizationException;
import com.orrery.authentication.invite.InviteTokenService;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.exception.AuthorizationException;
import com.linkedin.orrery.graphql.generated.GetInviteTokenInput;
import graphql.schema.DataFetchingEnvironment;
import io.orreryproject.metadata.context.OperationContext;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class GetInviteTokenResolverTest {
  private static final String ROLE_URN_STRING = "urn:li:orreryRole:Admin";
  private static final String INVITE_TOKEN_STRING = "inviteToken";
  private InviteTokenService _inviteTokenService;
  private GetInviteTokenResolver _resolver;
  private DataFetchingEnvironment _dataFetchingEnvironment;
  private Authentication _authentication;
  private OperationContext opContext;

  @BeforeMethod
  public void setupTest() throws Exception {
    _inviteTokenService = mock(InviteTokenService.class);
    _dataFetchingEnvironment = mock(DataFetchingEnvironment.class);
    _authentication = mock(Authentication.class);
    opContext = mock(OperationContext.class);
    when(opContext.getAuthentication()).thenReturn(_authentication);
    _resolver = new GetInviteTokenResolver(_inviteTokenService);
  }

  @Test
  public void testNotAuthorizedFails() {
    QueryContext mockContext = getMockDenyContext();
    when(_dataFetchingEnvironment.getContext()).thenReturn(mockContext);

    assertThrows(() -> _resolver.get(_dataFetchingEnvironment).join());
  }

  @Test
  public void testPasses() throws Exception {
    QueryContext mockContext = getMockAllowContext();
    when(_dataFetchingEnvironment.getContext()).thenReturn(mockContext);
    when(mockContext.getAuthentication()).thenReturn(_authentication);
    when(_inviteTokenService.getInviteToken(any(OperationContext.class), any(), eq(false)))
        .thenReturn(INVITE_TOKEN_STRING);

    GetInviteTokenInput input = new GetInviteTokenInput();
    input.setRoleUrn(ROLE_URN_STRING);
    when(_dataFetchingEnvironment.getArgument(eq("input"))).thenReturn(input);

    assertEquals(
        _resolver.get(_dataFetchingEnvironment).join().getInviteToken(), INVITE_TOKEN_STRING);
  }

  @Test
  public void testRoleDeniedMapsToAuthorizationException() throws Exception {
    QueryContext mockContext = getMockAllowContext();
    when(_dataFetchingEnvironment.getContext()).thenReturn(mockContext);
    when(mockContext.getAuthentication()).thenReturn(_authentication);
    when(_inviteTokenService.getInviteToken(any(OperationContext.class), any(), eq(false)))
        .thenThrow(new InviteTokenAuthorizationException("denied"));

    GetInviteTokenInput input = new GetInviteTokenInput();
    input.setRoleUrn(ROLE_URN_STRING);
    when(_dataFetchingEnvironment.getArgument(eq("input"))).thenReturn(input);

    Exception exception =
        expectThrows(Exception.class, () -> _resolver.get(_dataFetchingEnvironment).join());
    assertTrue(exception.getCause() instanceof AuthorizationException);
  }
}
