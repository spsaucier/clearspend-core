package com.clearspend.capital.client.stripe;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ToString
@Configuration
@ConfigurationProperties(prefix = "client.stripe")
public class StripeProperties {

  private String apiKey;
  private String secret;

  private boolean enableTelemetry;
  private Integer connectTimeout;
  private Integer readTimeout;
  private Integer maxNetworkRetries;

  private String tosAcceptanceIp;
}
