package com.clearspend.capital;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

@Slf4j
public class SharedRedisContainer extends GenericContainer<SharedRedisContainer> {
  private static final SharedRedisContainer container = new SharedRedisContainer();
  private static final DoNothingContainer<SharedRedisContainer> doNothingContainer =
      new DoNothingContainer<>();

  public SharedRedisContainer() {
    super(DockerImageName.parse("redis:6.2.7"));
    addFixedExposedPort(16379, 6379);
  }

  public static GenericContainer<SharedRedisContainer> getInstance() {
    if (TestEnv.isFastTestExecution()) {
      return doNothingContainer;
    }
    return container;
  }

  @Override
  public void start() {
    super.start();
    System.setProperty("TC_REDIS_PORT", "16379");
    followOutput(new Slf4jLogConsumer(log));
  }
}
