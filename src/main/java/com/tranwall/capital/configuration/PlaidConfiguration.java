package com.tranwall.capital.configuration;

import com.plaid.client.ApiClient;
import com.plaid.client.request.PlaidApi;
import com.tranwall.capital.client.plaid.PlaidProperties;
import java.util.HashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class PlaidConfiguration {

  @Bean
  public PlaidApi plaidApi(PlaidProperties plaidProperties) {
    log.debug("{}", plaidProperties.getEnvironment());
    log.debug("{}", plaidProperties.getClientId());
    log.debug("Plaid secret is set: {}", plaidProperties.getSecret() != null);
    // Set up ApiClient
    HashMap<String, String> apiKeys = new HashMap<>();
    apiKeys.put("clientId", plaidProperties.getClientId());
    apiKeys.put("secret", plaidProperties.getSecret());
    ApiClient apiClient = new ApiClient(apiKeys);
    if (plaidProperties.getEnvironment().equalsIgnoreCase("SANDBOX")) {
      apiClient.setPlaidAdapter(ApiClient.Sandbox);
    } else if (plaidProperties.getEnvironment().equalsIgnoreCase("DEVELOPMENT")) {
      apiClient.setPlaidAdapter(ApiClient.Development);
    } else if (plaidProperties.getEnvironment().equalsIgnoreCase("PRODUCTION")) {
      apiClient.setPlaidAdapter(ApiClient.Production);
    } else {
      throw new UnsupportedOperationException(
          String.format(
              "Plaid environment %s not supported. Supported values are [SANDBOX, DEVELOPMENT, PRODUCTION]",
              plaidProperties.getEnvironment()));
    }

    // Set up PlaidClient
    return apiClient.createService(PlaidApi.class);
  }
}
