package com.clearspend.capital;

import com.clearspend.capital.util.SimpleYaml;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.util.Map;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.SocketUtils;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/** Override stop method in order to make database durable between test classes */
@Slf4j
public class SharedFusionAuthContainer extends GenericContainer<SharedFusionAuthContainer> {

  private static final SharedFusionAuthContainer container = new SharedFusionAuthContainer();

  @SneakyThrows
  private static String getImageName() {
    final SimpleYaml config =
        new SimpleYaml(new BufferedInputStream(new FileInputStream("docker-compose.yml")));
    return (String) config.get("services.fusionauth.image");
  }

  @SneakyThrows
  private SharedFusionAuthContainer() {
    super(DockerImageName.parse(getImageName()));
    withNetwork(BaseCapitalTest.fusionAuthNetwork);
    withFileSystemBind(
        "./local/fusionauth/kickstart", "/usr/local/fusionauth/kickstart", BindMode.READ_ONLY);
    waitingFor(Wait.forLogMessage(".*Server startup in.*\\n", 1));
    dependsOn(FusionAuthPostgreSQLContainer.getInstance());
    int fusionAuthPort = SocketUtils.findAvailableTcpPort(40000);
    addFixedExposedPort(fusionAuthPort, 9011);

    withEnv(
            Map.of(
                "DATABASE_URL", "jdbc:postgresql://db:5432/fusionauth",
                "DATABASE_ROOT_USERNAME", "fusionauth",
                "DATABASE_ROOT_PASSWORD", "docker",
                "DATABASE_USERNAME", "fusionauth",
                "DATABASE_PASSWORD", "docker",
                "FUSIONAUTH_APP_MEMORY", "192m",
                "FUSIONAUTH_APP_RUNTIME_MODE", "development",
                "FUSIONAUTH_APP_URL", "http://localhost:9011",
                "SEARCH_TYPE", "database",
                "FUSIONAUTH_APP_KICKSTART_FILE", "/usr/local/fusionauth/kickstart/kickstart.json"))
        .withEnv("FUSIONAUTH_PORT", String.valueOf(fusionAuthPort));
  }

  public static SharedFusionAuthContainer getInstance() {
    return container;
  }

  @Override
  public void start() {
    super.start();
    log.info("fusionauth port: {}", container.getMappedPort(9011));
    System.setProperty(
        "FUSIONAUTH_BASE_URL", String.format("http://localhost:%d", container.getMappedPort(9011)));
    Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log);
    container.followOutput(logConsumer);
  }

  @Override
  public void stop() {
    // do nothing, JVM handles shut down
  }
}
