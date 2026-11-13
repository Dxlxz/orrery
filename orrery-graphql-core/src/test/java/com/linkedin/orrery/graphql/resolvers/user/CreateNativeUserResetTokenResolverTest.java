package com.linkedin.orrery.graphql.resolvers.user;

import static com.linkedin.orrery.graphql.TestUtils.*;
import static org.mockito.Mockito.*;
import static org.testng.Assert.*;

import com.orrery.authentication.Authentication;
import com.orrery.authentication.user.NativeUserService;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.CreateNativeUserResetTokenInput;
import graphql.schema.DataFetchingEnvironment;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class CreateNativeUserResetTokenResolverTest {

  private static final String RESET_TOKEN = "resetToken";
  private static final String USER_URN_STRING = "urn:li:corpuser:test";

  private NativeUserService _nativeUserService;
  private CreateNativeUserResetTokenResolver _resolver;
  private DataFetchingEnvironment _dataFetchingEnvironment;
  private Authentication _authentication;

  @BeforeMethod
  public void setupTest() {
    _nativeUserService = mock(NativeUserService.class);
    _dataFetchingEnvironment = mock(DataFetchingEnvironment.class);
    _authentication = mock(Authentication.class);

    _resolver = new CreateNativeUserResetTokenResolver(_nativeUserService);
  }

  @Test
  public void testFailsCannotManageUserCredentials() {
    QueryContext mockContext = getMockDenyContext();
    when(_dataFetchingEnvironment.getContext()).thenReturn(mockContext);

    assertThrows(() -> _resolver.get(_dataFetchingEnvironment).join());
  }

  @Test
  public void testFailsNullUserUrn() throws Exception {
    QueryContext mockContext = getMockAllowContext();
    when(_dataFetchingEnvironment.getContext()).thenReturn(mockContext);
    CreateNativeUserResetTokenInput input = new CreateNativeUserResetTokenInput(null);
    when(_dataFetchingEnvironment.getArgument(eq("input"))).thenReturn(input);
    when(mockContext.getAuthentication()).thenReturn(_authentication);
    when(_nativeUserService.generateNativeUserPasswordResetToken(any(), any()))
        .thenReturn(RESET_TOKEN);

    assertThrows(() -> _resolver.get(_dataFetchingEnvironment).join());
  }

  @Test
  public void testPasses() throws Exception {
    QueryContext mockContext = getMockAllowContext();
    when(_dataFetchingEnvironment.getContext()).thenReturn(mockContext);
    CreateNativeUserResetTokenInput input = new CreateNativeUserResetTokenInput(USER_URN_STRING);
    when(_dataFetchingEnvironment.getArgument(eq("input"))).thenReturn(input);
    when(mockContext.getAuthentication()).thenReturn(_authentication);
    when(_nativeUserService.generateNativeUserPasswordResetToken(any(), any()))
        .thenReturn(RESET_TOKEN);

    assertEquals(RESET_TOKEN, _resolver.get(_dataFetchingEnvironment).join().getResetToken());
  }
}
