package com.clearspend.capital.configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExecutorConfiguration {

  @Bean
  @Qualifier("MessageEnrichment")
  public ExecutorService createMessageEnrichmentExecutor() {
    return Executors.newSingleThreadExecutor();
  }
}
