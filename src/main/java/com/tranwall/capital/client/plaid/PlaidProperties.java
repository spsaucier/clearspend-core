package com.tranwall.capital.client.plaid;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "client.plaid")
@Getter
@Setter
public class PlaidProperties {

  private String clientId;
  private String secret;
  private String environment;

  public boolean isConfigured() {
    return StringUtils.isNoneBlank(clientId, secret, environment);
  }
}
