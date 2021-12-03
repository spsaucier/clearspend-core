package com.tranwall.capital.client.plaid;

import com.plaid.client.model.AccountBase;
import com.plaid.client.model.AccountIdentity;
import com.plaid.client.model.AuthGetRequest;
import com.plaid.client.model.AuthGetResponse;
import com.plaid.client.model.CountryCode;
import com.plaid.client.model.IdentityGetRequest;
import com.plaid.client.model.IdentityGetResponse;
import com.plaid.client.model.ItemPublicTokenExchangeRequest;
import com.plaid.client.model.ItemPublicTokenExchangeResponse;
import com.plaid.client.model.LinkTokenCreateRequest;
import com.plaid.client.model.LinkTokenCreateRequestUser;
import com.plaid.client.model.LinkTokenCreateResponse;
import com.plaid.client.model.NumbersACH;
import com.plaid.client.model.Products;
import com.plaid.client.request.PlaidApi;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import retrofit2.Response;

@Component
@RequiredArgsConstructor
@Slf4j
@Profile("!test")
public class PlaidClient {

  public static final String PLAID_CLIENT_NAME = "ClearSpend";
  public static final String LANGUAGE = "en";

  @NonNull private PlaidProperties plaidProperties;
  @NonNull private PlaidApi plaidApi;

  protected PlaidApi client() {
    return plaidApi;
  }

  public boolean isConfigured() {
    return plaidProperties.isConfigured();
  }

  public PlaidClient.OwnersResponse getOwners(String accessToken) throws IOException {
    IdentityGetRequest authGetRequest = new IdentityGetRequest().accessToken(accessToken);
    Response<IdentityGetResponse> authGetResponse = plaidApi.identityGet(authGetRequest).execute();

    return new OwnersResponse(accessToken, validBody(authGetResponse).getAccounts());
  }

  public record OwnersResponse(String accessToken, List<AccountIdentity> accounts) {}

  public record AccountsResponse(
      String accessToken, List<AccountBase> accounts, List<NumbersACH> achList) {}

  public String createLinkToken(TypedId<BusinessId> businessId) throws IOException {
    // TODO CAP-218 Use RUX https://plaid.com/docs/link/returning-user/
    LinkTokenCreateRequest request =
        new LinkTokenCreateRequest()
            .clientName(PLAID_CLIENT_NAME)
            .language(LANGUAGE)
            .countryCodes(Collections.singletonList(CountryCode.US))
            .products(Arrays.asList(Products.AUTH, Products.IDENTITY))
            .user(new LinkTokenCreateRequestUser().clientUserId(businessId.toString()));
    Response<LinkTokenCreateResponse> response = plaidApi.linkTokenCreate(request).execute();
    log.debug("{}", response.code());

    return validBody(response).getLinkToken();
  }

  public AccountsResponse getAccounts(String accessToken) throws IOException {
    AuthGetRequest authGetRequest = new AuthGetRequest().accessToken(accessToken);
    Response<AuthGetResponse> authGetResponse = plaidApi.authGet(authGetRequest).execute();

    AuthGetResponse body = validBody(authGetResponse);

    return new AccountsResponse(accessToken, body.getAccounts(), body.getNumbers().getAch());
  }

  /**
   * Validate that the given response was successful and return its decoded body.
   *
   * @param response A response to analyze for success
   * @param <T> The type of response expected
   * @return the body of the response, decoded
   * @throws IOException if there is a problem making a string out of the errorBody
   * @throws PlaidClientException if Plaid returned an error
   */
  protected <T> @NonNull T validBody(Response<T> response) throws IOException {
    if (response == null || !response.isSuccessful()) {
      String errorMessage = "Error in response";
      String errorBody = "[empty]";
      if (response != null && response.errorBody() != null) {
        errorBody = response.errorBody().string();
      }
      log.error("{}: {}", errorMessage, errorBody);
      log.debug(String.valueOf(response));
      throw new PlaidClientException(errorBody, String.valueOf(response));
    }
    return Objects.requireNonNull(response.body());
  }

  public String exchangePublicTokenForAccessToken(@NonNull String linkToken) throws IOException {
    ItemPublicTokenExchangeRequest itemPublicTokenCreateRequest =
        new ItemPublicTokenExchangeRequest().publicToken(linkToken);
    Response<ItemPublicTokenExchangeResponse> response =
        plaidApi.itemPublicTokenExchange(itemPublicTokenCreateRequest).execute();

    return validBody(response).getAccessToken();
  }
}
