package com.tranwall.capital.client.plaid;

import com.plaid.client.model.*;
import com.plaid.client.request.PlaidApi;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
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
  public record AccountsResponse(String accessToken, List<NumbersACH> achList) {};

  public String createLinkToken(UUID businessId) throws IOException {
    LinkTokenCreateRequest request =
        new LinkTokenCreateRequest()
            .clientName(PLAID_CLIENT_NAME)
            .language(LANGUAGE)
            .countryCodes(Collections.singletonList(CountryCode.US))
            .products(Collections.singletonList(Products.AUTH))
            .user(new LinkTokenCreateRequestUser().clientUserId(businessId.toString()));
    Response<LinkTokenCreateResponse> response = plaidApi.linkTokenCreate(request).execute();
    log.debug("{}", response.code());
    log.debug("{}", response.body());
    log.debug("{}", response.errorBody() != null ? response.errorBody().string() : "");

    if (response.body() == null) {
      throw new RuntimeException("No response body from Plaid");
    }

    return response.body().getLinkToken();
  }

  public AccountsResponse getAccounts(String linkToken) throws IOException {
    Response<ItemPublicTokenExchangeResponse> response = exchangePublicToken(linkToken);

    if (!response.isSuccessful() || response.body() == null) {
      String errorMessage = "Error while exchanging public token";
      log.error(errorMessage);
      throw new RuntimeException(errorMessage);
    }

    AuthGetRequest authGetRequest =
        new AuthGetRequest().accessToken(response.body().getAccessToken());
    Response<AuthGetResponse> authGetResponse = plaidApi.authGet(authGetRequest).execute();
    log.debug("{}", authGetResponse.code());
    log.debug("{}", authGetResponse.body());
    log.debug(
        "{}", authGetResponse.errorBody() != null ? authGetResponse.errorBody().string() : "");

    if (authGetResponse.isSuccessful() && authGetResponse.body() != null)  {
      return new AccountsResponse(response.body().getAccessToken(), authGetResponse.body().getNumbers().getAch());
    }

    return null;
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
