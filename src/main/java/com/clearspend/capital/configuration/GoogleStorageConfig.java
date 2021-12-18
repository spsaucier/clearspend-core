package com.clearspend.capital.configuration;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.contrib.nio.testing.LocalStorageHelper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class GoogleStorageConfig {

  // In order to make local/test instances work without real GCP connection
  @Bean
  @Profile({"default", "test"})
  Storage storage() {
    return LocalStorageHelper.getOptions().getService();
  }
}
