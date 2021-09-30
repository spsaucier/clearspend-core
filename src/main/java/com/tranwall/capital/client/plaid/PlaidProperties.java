package com.tranwall.capital.client.plaid;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "plaid")
@Getter
@Setter
public class PlaidProperties {
  private String clientId;
  private String secret;
  private String environment;
}
