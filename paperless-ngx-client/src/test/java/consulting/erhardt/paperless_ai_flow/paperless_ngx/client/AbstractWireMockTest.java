package consulting.erhardt.paperless_ai_flow.paperless_ngx.client;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 * Abstract base class for WireMock-based integration tests.
 * Manages the WireMock server lifecycle and provides helper methods for stubbing API responses.
 */
public abstract class AbstractWireMockTest {

  protected static WireMockServer wireMockServer;
  protected static String baseUrl;

  @BeforeAll
  static void startWireMock() {
    wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
    wireMockServer.start();
    baseUrl = "http://localhost:" + wireMockServer.port();
    WireMock.configureFor("localhost", wireMockServer.port());
  }

  @AfterAll
  static void stopWireMock() {
    if (wireMockServer != null && wireMockServer.isRunning()) {
      wireMockServer.stop();
    }
  }

  @BeforeEach
  void resetWireMock() {
    wireMockServer.resetAll();
  }

  /**
   * Get the base URL for the WireMock server.
   */
  protected String getBaseUrl() {
    return baseUrl;
  }

  /**
   * Get the port of the WireMock server.
   */
  protected int getPort() {
    return wireMockServer.port();
  }
}
