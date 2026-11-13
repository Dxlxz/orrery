package com.linkedin.orrery.graphql.resolvers.role;

import static com.linkedin.orrery.graphql.TestUtils.*;
import static com.linkedin.metadata.Constants.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.anyInt;
import static org.testng.Assert.*;

import com.orrery.authentication.Authentication;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.linkedin.common.urn.Urn;
import com.linkedin.orrery.graphql.QueryContext;
import com.linkedin.orrery.graphql.generated.ListRolesInput;
import com.linkedin.orrery.graphql.generated.ListRolesResult;
import com.linkedin.entity.Aspect;
import com.linkedin.entity.EntityResponse;
import com.linkedin.entity.EnvelopedAspect;
import com.linkedin.entity.EnvelopedAspectMap;
import com.linkedin.entity.client.EntityClient;
import com.linkedin.metadata.search.SearchEntity;
import com.linkedin.metadata.search.SearchEntityArray;
import com.linkedin.metadata.search.SearchResult;
import com.linkedin.metadata.search.SearchResultMetadata;
import com.linkedin.policy.OrreryRoleInfo;
import graphql.schema.DataFetchingEnvironment;
import java.util.Map;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ListRolesResolverTest {
  private static final String ADMIN_ROLE_URN_STRING = "urn:li:orreryRole:Admin";
  private static final String EDITOR_ROLE_URN_STRING = "urn:li:orreryRole:Editor";
  private static Map<Urn, EntityResponse> _entityResponseMap;

  private EntityClient _entityClient;
  private ListRolesResolver _resolver;
  private DataFetchingEnvironment _dataFetchingEnvironment;
  private Authentication _authentication;

  private EntityResponse getMockRoleEntityResponse(Urn roleUrn) {
    EntityResponse entityResponse = new EntityResponse().setUrn(roleUrn);
    OrreryRoleInfo orreryRoleInfo = new OrreryRoleInfo();
    orreryRoleInfo.setDescription(roleUrn.toString());
    orreryRoleInfo.setName(roleUrn.toString());
    entityResponse.setAspects(
        new EnvelopedAspectMap(
            ImmutableMap.of(
                ORRERY_ROLE_INFO_ASPECT_NAME,
                new EnvelopedAspect().setValue(new Aspect(orreryRoleInfo.data())))));

    return entityResponse;
  }

  @BeforeMethod
  public void setupTest() throws Exception {
    Urn adminRoleUrn = Urn.createFromString(ADMIN_ROLE_URN_STRING);
    Urn editorRoleUrn = Urn.createFromString(EDITOR_ROLE_URN_STRING);
    _entityResponseMap =
        ImmutableMap.of(
            adminRoleUrn,
            getMockRoleEntityResponse(adminRoleUrn),
            editorRoleUrn,
            getMockRoleEntityResponse(editorRoleUrn));

    _entityClient = mock(EntityClient.class);
    _dataFetchingEnvironment = mock(DataFetchingEnvironment.class);
    _authentication = mock(Authentication.class);

    _resolver = new ListRolesResolver(_entityClient);
  }

  @Test
  public void testNotAuthorizedFails() {
    QueryContext mockContext = getMockDenyContext();
    when(_dataFetchingEnvironment.getContext()).thenReturn(mockContext);

    assertThrows(() -> _resolver.get(_dataFetchingEnvironment).join());
  }

  @Test
  public void testListRoles() throws Exception {
    QueryContext mockContext = getMockAllowContext();
    when(_dataFetchingEnvironment.getContext()).thenReturn(mockContext);
    when(mockContext.getAuthentication()).thenReturn(_authentication);

    ListRolesInput input = new ListRolesInput();
    when(_dataFetchingEnvironment.getArgument("input")).thenReturn(input);
    final SearchResult roleSearchResult =
        new SearchResult()
            .setMetadata(new SearchResultMetadata())
            .setFrom(0)
            .setPageSize(10)
            .setNumEntities(2);
    roleSearchResult.setEntities(
        new SearchEntityArray(
            ImmutableList.of(
                new SearchEntity().setEntity(Urn.createFromString(ADMIN_ROLE_URN_STRING)),
                new SearchEntity().setEntity(Urn.createFromString(EDITOR_ROLE_URN_STRING)))));

    when(_entityClient.search(
            any(), eq(ORRERY_ROLE_ENTITY_NAME), any(), any(), anyInt(), anyInt()))
        .thenReturn(roleSearchResult);
    when(_entityClient.batchGetV2(any(), eq(ORRERY_ROLE_ENTITY_NAME), any(), any()))
        .thenReturn(_entityResponseMap);

    ListRolesResult result = _resolver.get(_dataFetchingEnvironment).join();
    assertEquals(result.getStart(), 0);
    assertEquals(result.getCount(), 10);
    assertEquals(result.getTotal(), 2);
    assertEquals(result.getRoles().size(), 2);
  }
}
