package com.clearspend.capital.client.stripe;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@ToString
@Configuration
@ConfigurationProperties(prefix = "client.stripe")
public class StripeProperties {

  private String apiKey;
  private boolean allowSkipStripeHeaderValidation;
  private String issuingSecret;
  private String connectSecret;

  private boolean enableTelemetry;
  private Integer connectTimeout;
  private Integer readTimeout;
  private Integer maxNetworkRetries;

  private String tosAcceptanceIp;
  private String clearspendConnectedAccountId;
  private String clearspendFinancialAccountId;
  private boolean enableTransferFailures;
  private String authFallbackUrl;

  public boolean isTestMode() {
    return apiKey == null || StringUtils.startsWith(apiKey, "sk_test");
  }
}
