package orrery.server;

import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.*;

import org.mockserver.integration.ClientAndServer;
import org.mockserver.matchers.Times;

public class TestOrreryServer {

  private ClientAndServer mockServer;

  public Integer getPort() {
    return mockServer.getPort();
  }

  public ClientAndServer getMockServer() {
    return mockServer;
  }

  public TestOrreryServer() {
    mockServer = startClientAndServer();
    init();
  }

  public void init() {
    mockServer
        .when(
            request()
                .withMethod("GET")
                .withPath("/config")
                .withHeader("Content-type", "application/json"),
            Times.unlimited())
        .respond(org.mockserver.model.HttpResponse.response().withBody("{\"noCode\": true }"));
  }
}
