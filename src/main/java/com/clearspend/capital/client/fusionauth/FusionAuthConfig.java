package com.clearspend.capital.client.fusionauth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FusionAuthConfig {

  @Bean("fusionAuthClientLib")
  io.fusionauth.client.FusionAuthClient fusionAuthClient(FusionAuthProperties properties) {
    return new io.fusionauth.client.FusionAuthClient(
        properties.getApiKey(), properties.getBaseUrl());
  }
}
