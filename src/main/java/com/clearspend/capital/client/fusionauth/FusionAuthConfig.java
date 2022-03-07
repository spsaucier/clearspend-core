package com.clearspend.capital.client.fusionauth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class FusionAuthConfig {

  @Bean("fusionAuthClientLib")
  @Profile("!test")
  io.fusionauth.client.FusionAuthClient fusionAuthClient(FusionAuthProperties properties) {
    return new io.fusionauth.client.FusionAuthClient(
        properties.getApiKey(), properties.getBaseUrl());
  }
}
