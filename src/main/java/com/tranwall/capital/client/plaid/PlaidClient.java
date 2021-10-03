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
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import retrofit2.Response;

@Component
@Slf4j
public class PlaidClient {
  public static final String PLAID_CLIENT_NAME = "Tranwall";
  public static final String LANGUAGE = "en";

  private final PlaidApi plaidClient;

  @Autowired
  public PlaidClient(PlaidProperties plaidProperties) {
    ApiClient apiClient =
        new ApiClient(
            Map.of(
                "clientId", plaidProperties.getClientId(),
                "secret", plaidProperties.getSecret()));

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

  public String createLinkToken() throws IOException {
    // TODO: User ID instead of random UUID
    LinkTokenCreateRequest request =
        new LinkTokenCreateRequest()
            .clientName(PLAID_CLIENT_NAME)
            .language(LANGUAGE)
            .countryCodes(Collections.singletonList(CountryCode.US))
            .products(Collections.singletonList(Products.TRANSACTIONS))
            .user(new LinkTokenCreateRequestUser().clientUserId(UUID.randomUUID().toString()));
    Response<LinkTokenCreateResponse> response = plaidClient.linkTokenCreate(request).execute();
    log.debug("{}", response.code());
    log.debug("{}", response.body());
    log.debug("{}", response.errorBody() != null ? response.errorBody().string() : "");

    if (response.body() == null) {
      throw new RuntimeException("No response body from Plaid");
    }

    return response.body().getLinkToken();
  }
}
