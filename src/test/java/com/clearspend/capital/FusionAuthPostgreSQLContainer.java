package com.clearspend.capital;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/** Override stop method in order to make database durable between test classes */
public class FusionAuthPostgreSQLContainer
    extends PostgreSQLContainer<FusionAuthPostgreSQLContainer> {
  private static final FusionAuthPostgreSQLContainer container =
      new FusionAuthPostgreSQLContainer();
  private static final DoNothingContainer<FusionAuthPostgreSQLContainer> doNothingContainer =
      new DoNothingContainer<>();

  private FusionAuthPostgreSQLContainer() {
    super(DockerImageName.parse("postgres:13.4-alpine").asCompatibleSubstituteFor("postgres"));
    withNetwork(BaseCapitalTest.fusionAuthNetwork);
    withNetworkAliases("db");
    withDatabaseName("fusionauth");
    withUsername("fusionauth");
    withPassword("docker");
    waitingFor(Wait.forLogMessage(".*database system is ready to accept connections.*\\n", 1));
  }

  public static GenericContainer<FusionAuthPostgreSQLContainer> getInstance() {
    if (TestEnv.isFastTestExecution()) {
      return doNothingContainer;
    }
    return container;
  }

  @Override
  public void start() {
    super.start();
  }

  @Override
  public void stop() {
    // do nothing, JVM handles shut down
  }
}
