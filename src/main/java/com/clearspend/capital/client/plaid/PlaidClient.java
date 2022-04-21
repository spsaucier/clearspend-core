package com.clearspend.capital.client.plaid;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.crypto.data.model.embedded.EncryptedString;
import com.clearspend.capital.data.model.PlaidLogEntry;
import com.clearspend.capital.data.model.enums.PlaidResponseType;
import com.clearspend.capital.data.repository.PlaidLogEntryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plaid.client.model.AccountBase;
import com.plaid.client.model.AccountIdentity;
import com.plaid.client.model.AccountsBalanceGetRequest;
import com.plaid.client.model.AccountsGetResponse;
import com.plaid.client.model.AuthGetRequest;
import com.plaid.client.model.AuthGetResponse;
import com.plaid.client.model.CountryCode;
import com.plaid.client.model.IdentityGetRequest;
import com.plaid.client.model.IdentityGetResponse;
import com.plaid.client.model.InstitutionsGetByIdRequest;
import com.plaid.client.model.InstitutionsGetByIdResponse;
import com.plaid.client.model.ItemPublicTokenExchangeRequest;
import com.plaid.client.model.ItemPublicTokenExchangeResponse;
import com.plaid.client.model.LinkTokenCreateRequest;
import com.plaid.client.model.LinkTokenCreateRequestUser;
import com.plaid.client.model.LinkTokenCreateResponse;
import com.plaid.client.model.NumbersACH;
import com.plaid.client.model.ProcessorStripeBankAccountTokenCreateRequest;
import com.plaid.client.model.ProcessorStripeBankAccountTokenCreateResponse;
import com.plaid.client.model.Products;
import com.plaid.client.request.PlaidApi;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import retrofit2.Response;

@Component
@Slf4j
@Profile("prod")
public class PlaidClient {

  public static final String PLAID_CLIENT_NAME = "ClearSpend";
  public static final String LANGUAGE = "en";

  @NonNull private final PlaidProperties plaidProperties;
  @NonNull protected final PlaidApi plaidApi;
  @NonNull private final ObjectMapper mapper;
  @NonNull private final PlaidLogEntryRepository plaidLogEntryRepository;

  public PlaidClient(
      @NonNull PlaidProperties plaidProperties,
      @NonNull PlaidApi plaidApi,
      @NonNull ObjectMapper mapper,
      @NonNull PlaidLogEntryRepository plaidLogEntryRepository) {
    this.plaidProperties = plaidProperties;
    this.plaidApi = plaidApi;
    this.mapper = mapper;
    this.plaidLogEntryRepository = plaidLogEntryRepository;
  }

  protected PlaidApi client() {
    return plaidApi;
  }

  public boolean isConfigured() {
    return plaidProperties.isConfigured();
  }

  public PlaidClient.OwnersResponse getOwners(
      String accessToken, @NonNull TypedId<BusinessId> businessId) throws IOException {
    IdentityGetRequest authGetRequest = new IdentityGetRequest().accessToken(accessToken);
    Response<IdentityGetResponse> authGetResponse = plaidApi.identityGet(authGetRequest).execute();

    return new OwnersResponse(accessToken, validBody(businessId, authGetResponse).getAccounts());
  }

  public record OwnersResponse(String accessToken, List<AccountIdentity> accounts) {}

  public record AccountsResponse(
      String accessToken,
      List<AccountBase> accounts,
      List<NumbersACH> achList,
      String institutionName) {}

  @SuppressWarnings("MissingCasesInEnumSwitch")
  public String createLinkToken(TypedId<BusinessId> businessId) throws IOException {
    /* The Plaid documentation suggests that we can use
     * <a href="https://plaid.com/docs/api/items/#itemget">/item/get</a> to find out
     * what products an institution supports, but we don't have the institution yet.
     *
     * I'm assuming the majority of institutions will allow retrieving IDENTITY,
     * so adding an extra call to avoid the exception seems like it would slow
     * the process needlessly.
     */
    try {
      return createLinkToken(businessId, Arrays.asList(Products.AUTH, Products.IDENTITY));
    } catch (PlaidClientException e) {
      switch (e.getErrorCode()) {
          // These are the only cases that warrant special handling
        case PRODUCTS_NOT_SUPPORTED,
            PRODUCT_NOT_ENABLED,
            PRODUCT_NOT_READY,
            PRODUCT_UNAVAILABLE,
            INVALID_PRODUCT,
            INVALID_PRODUCTS -> {
          return createLinkToken(businessId, Collections.singletonList(Products.AUTH));
        }
      }
      throw e;
    }
  }

  public String createLinkToken(TypedId<BusinessId> businessId, String accessToken)
      throws IOException {
    // TODO CAP-218 Use RUX https://plaid.com/docs/link/returning-user/
    LinkTokenCreateRequest request =
        new LinkTokenCreateRequest()
            .clientName(PLAID_CLIENT_NAME)
            .user(new LinkTokenCreateRequestUser().clientUserId(businessId.toString()))
            .language(LANGUAGE)
            .countryCodes(Collections.singletonList(CountryCode.US))
            .accessToken(accessToken);
    Response<LinkTokenCreateResponse> response = plaidApi.linkTokenCreate(request).execute();
    log.debug("{}", response.code());

    return validBody(businessId, response).getLinkToken();
  }

  public String createLinkToken(TypedId<BusinessId> businessId, List<Products> products)
      throws IOException {
    // TODO CAP-218 Use RUX https://plaid.com/docs/link/returning-user/
    LinkTokenCreateRequest request =
        new LinkTokenCreateRequest()
            .clientName(PLAID_CLIENT_NAME)
            .language(LANGUAGE)
            .countryCodes(Collections.singletonList(CountryCode.US))
            .products(products)
            .user(new LinkTokenCreateRequestUser().clientUserId(businessId.toString()));
    Response<LinkTokenCreateResponse> response = plaidApi.linkTokenCreate(request).execute();
    log.debug("{}", response.code());

    return validBody(businessId, response).getLinkToken();
  }

  public AccountsResponse getAccounts(String accessToken, @NonNull TypedId<BusinessId> businessId)
      throws IOException {
    AuthGetRequest authGetRequest = new AuthGetRequest().accessToken(accessToken);
    Response<AuthGetResponse> authGetResponse = plaidApi.authGet(authGetRequest).execute();

    AuthGetResponse body = validBody(businessId, authGetResponse);
    String institutionName = getInstitutionName(body.getItem().getInstitutionId());

    return new AccountsResponse(
        accessToken, body.getAccounts(), body.getNumbers().getAch(), institutionName);
  }

  private String getInstitutionName(String institutionId) throws IOException {

    Response<InstitutionsGetByIdResponse> institution =
        plaidApi
            .institutionsGetById(
                new InstitutionsGetByIdRequest()
                    .institutionId(institutionId)
                    .countryCodes(List.of(CountryCode.US)))
            .execute();

    return institution.body() != null ? institution.body().getInstitution().getName() : "";
  }

  public List<AccountBase> getBalances(@NonNull TypedId<BusinessId> businessId, String accessToken)
      throws IOException {
    AccountsBalanceGetRequest request = new AccountsBalanceGetRequest().accessToken(accessToken);
    Response<AccountsGetResponse> response = client().accountsBalanceGet(request).execute();
    final AccountsGetResponse accountsGetResponse = validBody(businessId, response);
    return accountsGetResponse.getAccounts();
  }

  /**
   * Validate that the given response was successful and return its decoded body.
   *
   * @param <T> The type of response expected
   * @param businessId The business for which validation will be performed, for recording the
   *     results
   * @param response A response to analyze for success
   * @return the body of the response, decoded
   * @throws IOException if there is a problem making a string out of the errorBody
   * @throws PlaidClientException if Plaid returned an error
   */
  protected <T> @NonNull T validBody(@NonNull TypedId<BusinessId> businessId, Response<T> response)
      throws IOException {
    if (response == null || !response.isSuccessful()) {
      String errorMessage = "Error in response";
      String errorBody = "[empty]";
      if (response != null && response.errorBody() != null) {
        errorBody = response.errorBody().string();
      }
      log.debug("{}: {}", errorMessage, errorBody);
      log.debug(String.valueOf(response));

      PlaidError error = mapper.readValue(errorBody, PlaidError.class);
      plaidLogEntryRepository.save(
          new PlaidLogEntry(businessId, new EncryptedString(errorBody), PlaidResponseType.ERROR));
      throw new PlaidClientException(error, errorBody, String.valueOf(response));
    }
    final T responseObj = (T) Objects.requireNonNull(response.body());
    PlaidResponseType responseType =
        Arrays.stream(PlaidResponseType.values())
            .filter(t -> t.getResponseClass().isAssignableFrom(responseObj.getClass()))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException(responseObj.getClass().getName()));
    StringWriter writer = new StringWriter();
    mapper.writeValue(writer, responseObj);
    plaidLogEntryRepository.save(
        new PlaidLogEntry(businessId, new EncryptedString(writer.toString()), responseType));
    return responseObj;
  }

  public String exchangePublicTokenForAccessToken(
      @NonNull String linkToken, @NonNull TypedId<BusinessId> businessId) throws IOException {
    ItemPublicTokenExchangeRequest itemPublicTokenCreateRequest =
        new ItemPublicTokenExchangeRequest().publicToken(linkToken);
    Response<ItemPublicTokenExchangeResponse> response =
        plaidApi.itemPublicTokenExchange(itemPublicTokenCreateRequest).execute();

    return validBody(businessId, response).getAccessToken();
  }

  public String getStripeBankAccountToken(
      String plaidAccessToken, String plaidAccountId, TypedId<BusinessId> businessId) {
    ProcessorStripeBankAccountTokenCreateRequest request =
        new ProcessorStripeBankAccountTokenCreateRequest()
            .accessToken(plaidAccessToken)
            .accountId(plaidAccountId);
    try {
      Response<ProcessorStripeBankAccountTokenCreateResponse> response =
          plaidApi.processorStripeBankAccountTokenCreate(request).execute();

      return validBody(businessId, response).getStripeBankAccountToken();
    } catch (IOException e) {
      throw new RuntimeException("Failed to get stripe account token from plaid", e);
    }
  }

  /** For testing only */
  public Boolean sandboxItemResetLogin(TypedId<BusinessId> businessId, String plaidAccessToken) {
    throw new UnsupportedOperationException();
  }
}
