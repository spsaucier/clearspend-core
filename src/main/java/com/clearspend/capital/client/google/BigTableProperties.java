package com.clearspend.capital.client.google;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "client.bigtable")
public class BigTableProperties {
  private String projectId;
  private String instanceId;
  private String credentials;
}
