package com.tranwall.capital.client.plaid;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.plaid.client.model.AccountsBalanceGetRequest;
import com.plaid.client.model.AccountsGetResponse;
import com.plaid.client.model.CountryCode;
import com.plaid.client.model.ItemPublicTokenExchangeRequest;
import com.plaid.client.model.ItemPublicTokenExchangeResponse;
import com.plaid.client.model.LinkTokenCreateRequest;
import com.plaid.client.model.LinkTokenCreateRequestUser;
import com.plaid.client.model.LinkTokenCreateResponse;
import com.plaid.client.model.Products;
import com.plaid.client.request.PlaidApi;
import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import retrofit2.Response;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlaidClient {
  public static final String PLAID_CLIENT_NAME = "Tranwall";
  public static final String LANGUAGE = "en";
  @NonNull private PlaidApi plaidApi;
  @NonNull private ObjectMapper objectMapper;

  public String createLinkToken() throws IOException {
    // TODO: User ID instead of random UUID
    LinkTokenCreateRequest request =
        new LinkTokenCreateRequest()
            .clientName(PLAID_CLIENT_NAME)
            .language(LANGUAGE)
            .countryCodes(Collections.singletonList(CountryCode.US))
            .products(Collections.singletonList(Products.TRANSACTIONS))
            .user(new LinkTokenCreateRequestUser().clientUserId(UUID.randomUUID().toString()));
    Response<LinkTokenCreateResponse> response = plaidApi.linkTokenCreate(request).execute();
    log.debug("{}", response.code());
    log.debug("{}", response.body());
    log.debug("{}", response.errorBody() != null ? response.errorBody().string() : "");

    if (response.body() == null) {
      throw new RuntimeException("No response body from Plaid");
    }

    return response.body().getLinkToken();
  }

  public String getAccounts(String linkToken) throws IOException {
    // TODO: Check for already existing access token

    Response<ItemPublicTokenExchangeResponse> response = exchangePublicToken(linkToken);

    if (!response.isSuccessful() || response.body() == null) {
      String errorMessage = "Error while exchanging public token";
      log.error(errorMessage);
      throw new RuntimeException(errorMessage);
    }

    AccountsBalanceGetRequest accountsBalanceGetRequest =
        new AccountsBalanceGetRequest().accessToken(response.body().getAccessToken());
    Response<AccountsGetResponse> accountsGetResponse =
        plaidApi.accountsBalanceGet(accountsBalanceGetRequest).execute();
    log.debug("{}", accountsGetResponse.code());
    log.debug("{}", accountsGetResponse.body());
    log.debug(
        "{}",
        accountsGetResponse.errorBody() != null ? accountsGetResponse.errorBody().string() : "");

    // TODO: Insert into bank account table

    return objectMapper.writeValueAsString(accountsGetResponse.body());
  }

  private Response<ItemPublicTokenExchangeResponse> exchangePublicToken(String linkToken)
      throws IOException {
    ItemPublicTokenExchangeRequest itemPublicTokenCreateRequest =
        new ItemPublicTokenExchangeRequest().publicToken(linkToken);
    Response<ItemPublicTokenExchangeResponse> response =
        plaidApi.itemPublicTokenExchange(itemPublicTokenCreateRequest).execute();
    log.debug("{}", response.code());
    log.debug("{}", response.body());
    log.debug("{}", response.errorBody() != null ? response.errorBody().string() : "");

    return response;
  }
}
