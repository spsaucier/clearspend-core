package com.tranwall.capital.client.plaid;

import com.plaid.client.ApiClient;
import com.plaid.client.model.CountryCode;
import com.plaid.client.model.LinkTokenCreateRequest;
import com.plaid.client.model.LinkTokenCreateRequestUser;
import com.plaid.client.model.LinkTokenCreateResponse;
import com.plaid.client.model.Products;
import com.plaid.client.request.PlaidApi;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.UUID;
import javax.annotation.PostConstruct;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import retrofit2.Response;

@Component
@Data
@Slf4j
public class PlaidClient {
  private PlaidApi plaidClient;
  @NonNull private PlaidProperties plaidProperties;

  @PostConstruct
  public void init() {
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
    plaidClient = apiClient.createService(PlaidApi.class);
  }

  public void createLinkToken() throws IOException {
    LinkTokenCreateRequest request =
        new LinkTokenCreateRequest()
            .clientName("Tranwall")
            .language("en")
            .countryCodes(Collections.singletonList(CountryCode.US))
            .products(Collections.singletonList(Products.TRANSACTIONS))
            .user(new LinkTokenCreateRequestUser().clientUserId(UUID.randomUUID().toString()));
    Response<LinkTokenCreateResponse> response = plaidClient.linkTokenCreate(request).execute();
    log.info("{}", response.code());
    log.info("{}", response.body());
    log.info("{}", response.errorBody() != null ? response.errorBody().string() : "");
  }
}
