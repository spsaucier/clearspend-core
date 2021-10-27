package com.tranwall.capital;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.mockserver.integration.ClientAndServer;
import org.springframework.util.SocketUtils;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@CapitalTest
@Testcontainers
public abstract class BaseCapitalTest {

  private static ClientAndServer mockServer;
  private static int mockServerPort;

  static {
    mockServerPort = SocketUtils.findAvailableTcpPort(40000, 40100);
    mockServer = ClientAndServer.startClientAndServer(mockServerPort);
    System.setProperty("mockServerPort", String.valueOf(mockServerPort));
  }

  public final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .registerModule(new Jdk8Module())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  protected MockServerHelper mockServerHelper;

  @BeforeEach
  void mockServerHelper() {
    mockServer.reset();
    mockServerHelper = new MockServerHelper(mockServer);
  }

  @Container
  private static final PostgreSQLContainer<?> postgreSQLContainer =
      SharedPostgreSQLContainer.getInstance();

  static Network fusionAuthNetwork = Network.newNetwork();

  @Container
  private static final FusionAuthPostgreSQLContainer fusionAuthPostgreSQLContainer =
      FusionAuthPostgreSQLContainer.getInstance();

  @Container
  private static final SharedFusionAuthContainer fusionauthContainer =
      SharedFusionAuthContainer.getInstance();
}
