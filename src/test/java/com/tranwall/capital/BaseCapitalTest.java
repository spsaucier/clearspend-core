package com.tranwall.capital;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@CapitalTest
@Testcontainers
public abstract class BaseCapitalTest {

  @Container
  private static final PostgreSQLContainer<?> postgreSQLContainer =
      SharedPostgreSQLContainer.getInstance();
}
