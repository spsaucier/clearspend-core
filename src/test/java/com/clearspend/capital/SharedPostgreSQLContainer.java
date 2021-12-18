package com.clearspend.capital;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/** Override stop method in order to make database durable between test classes */
public class SharedPostgreSQLContainer extends PostgreSQLContainer<SharedPostgreSQLContainer> {
  private static final SharedPostgreSQLContainer container = new SharedPostgreSQLContainer();

  private SharedPostgreSQLContainer() {
    super(DockerImageName.parse("postgres:13.4-alpine").asCompatibleSubstituteFor("postgres"));
    withDatabaseName("capital");
    withUsername("postgres");
    withPassword("docker");
  }

  public static SharedPostgreSQLContainer getInstance() {
    return container;
  }

  @Override
  public void start() {
    super.start();
    System.setProperty("TC_DB_URL", container.getJdbcUrl());
    System.setProperty("TC_DB_USERNAME", container.getUsername());
    System.setProperty("TC_DB_PASSWORD", container.getPassword());
  }

  @Override
  public void stop() {
    // do nothing, JVM handles shut down
  }
}
