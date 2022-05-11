package com.clearspend.capital.configuration;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ToString
@Configuration
@ConfigurationProperties(prefix = "async")
public class AsyncProperties {

  private int corePoolSize;
  private int maxPoolSize;
  private int queueCapacity;
  private String threadNamePrefix;
}
