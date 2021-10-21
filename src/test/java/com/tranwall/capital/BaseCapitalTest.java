package com.tranwall.capital;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.mockserver.client.MockServerClient;
import org.mockserver.springtest.MockServerTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@CapitalTest
@Testcontainers
// It seems that mock server leads to situation when new context is spawned
// for every test class leading to a situation when db stops accepting any new
// connections. Marking the context as dirty is a workaround to avoid it
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@MockServerTest("server.url=http://localhost:${mockServerPort}")
public abstract class BaseCapitalTest {

  private MockServerClient mockServerClient;
  protected MockServerHelper mockServerHelper;

  @BeforeEach
  void mockServerHelper() {
    mockServerHelper = new MockServerHelper(mockServerClient);
  }

  public final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  @Container
  private static final PostgreSQLContainer<?> postgreSQLContainer =
      SharedPostgreSQLContainer.getInstance();
}
