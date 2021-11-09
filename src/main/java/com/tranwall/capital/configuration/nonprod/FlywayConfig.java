package com.tranwall.capital.configuration.nonprod;

import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

// from:
// https://stackoverflow.com/questions/37780487/run-flyway-java-based-callbacks-with-spring-boot
@Profile("!test")
@Configuration
public class FlywayConfig {

  @Bean
  public FlywayConfigurationCustomizer flywayConfigurationCustomizer(
      ClearFusionAuthCallback clearFusionAuthCallback) {
    return configuration -> configuration.callbacks(clearFusionAuthCallback);
  }
}
