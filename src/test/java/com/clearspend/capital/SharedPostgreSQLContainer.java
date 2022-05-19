package com.clearspend.capital;

import java.util.Map;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** Override stop method in order to make database durable between test classes */
public class SharedPostgreSQLContainer extends PostgreSQLContainer<SharedPostgreSQLContainer> {
  private static final SharedPostgreSQLContainer container = new SharedPostgreSQLContainer();
  private static final DoNothingContainer<SharedPostgreSQLContainer> doNothingContainer =
      new DoNothingContainer<>();

  private SharedPostgreSQLContainer() {
    super(DockerImageName.parse("postgres:13.4-alpine").asCompatibleSubstituteFor("postgres"));
    withDatabaseName("capital");
    withUsername("postgres");
    withPassword("docker").withEnv(Map.of("TZ", "UTC", "PGTZ", "UTC"));
  }

  public static GenericContainer<SharedPostgreSQLContainer> getInstance() {
    if (TestEnv.isFastTestExecution()) {
      return doNothingContainer;
    }
    return container;
  }

  @Override
  public void start() {
    super.start();
    System.setProperty("TC_DB_URL", getJdbcUrl());
    System.setProperty("TC_DB_USERNAME", getUsername());
    System.setProperty("TC_DB_PASSWORD", getPassword());
  }

  @Override
  public void stop() {
    // do nothing, JVM handles shut down
  }
}
