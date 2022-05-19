package com.clearspend.capital;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import javax.transaction.Transactional;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@CapitalTest
@Testcontainers
@Transactional
public abstract class BaseCapitalTest {

  public final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .registerModule(new Jdk8Module())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  @Container
  private static final GenericContainer<SharedPostgreSQLContainer> postgreSQLContainer =
      SharedPostgreSQLContainer.getInstance();

  static Network fusionAuthNetwork = Network.newNetwork();

  @Container
  private static final GenericContainer<FusionAuthPostgreSQLContainer>
      fusionAuthPostgreSQLContainer = FusionAuthPostgreSQLContainer.getInstance();

  @Container
  private static final GenericContainer<SharedFusionAuthContainer> fusionauthContainer =
      SharedFusionAuthContainer.getInstance();

  @Container
  private static final GenericContainer<SharedRedisContainer> redisContainer =
      SharedRedisContainer.getInstance();
}
