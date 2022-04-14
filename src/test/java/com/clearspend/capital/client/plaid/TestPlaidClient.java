package com.clearspend.capital.client.plaid;

import com.clearspend.capital.TestHelper;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.repository.PlaidLogEntryRepository;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plaid.client.model.AccountBase;
import com.plaid.client.model.AccountsGetResponse;
import com.plaid.client.model.InstitutionsGetByIdResponse;
import com.plaid.client.model.NumbersACH;
import com.plaid.client.model.Products;
import com.plaid.client.model.SandboxPublicTokenCreateRequest;
import com.plaid.client.model.SandboxPublicTokenCreateResponse;
import com.plaid.client.request.PlaidApi;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import retrofit2.Response;

@Profile("test")
@Component
@Slf4j
public class TestPlaidClient extends SandboxPlaidClient {

  private final ObjectMapper objectMapper;
  private final Resource mockAccountsResponse;
  private final Resource mockInstitutionResponse;
  private final Resource mockBalancesResponse;
  private final Resource mockOwnersResponse;

  public TestPlaidClient(
      @NonNull PlaidProperties plaidProperties,
      @NonNull PlaidApi plaidApi,
      @NonNull ObjectMapper mapper,
      @NonNull PlaidLogEntryRepository plaidLogEntryRepository,
      @Value("classpath:plaidResponses/accounts.json") @NonNull Resource mockAccountsResponse,
      @Value("classpath:plaidResponses/institution.json") @NonNull Resource mockInstitutionResponse,
      @Value("classpath:plaidResponses/balances.json") @NonNull Resource mockBalancesResponse,
      @Value("classpath:plaidResponses/owners.json") @NonNull Resource mockOwnersResponse) {
    super(plaidProperties, plaidApi, mapper, plaidLogEntryRepository);
    this.objectMapper = mapper;
    this.mockOwnersResponse = mockOwnersResponse;
    this.mockAccountsResponse = mockAccountsResponse;
    this.mockInstitutionResponse = mockInstitutionResponse;
    this.mockBalancesResponse = mockBalancesResponse;
  }

  /**
   * Sandbox-specific institution IDs used for testing. See sandbox documentation for more
   * information. https://plaid.com/docs/sandbox/institutions/
   */
  public record SandboxInstitution(String name, String sandbox_id, TypedId<BusinessId> businessId) {

    public SandboxInstitution(String name, String sandbox_id) {
      this(name, sandbox_id, new TypedId<>());
    }
  }

  public static final Set<SandboxInstitution> SANDBOX_INSTITUTIONS =
      Set.of(
          new SandboxInstitution("First Platypus Bank", "ins_109508"),
          new SandboxInstitution("First Gingham Credit Union", "ins_109509"),
          new SandboxInstitution("Tattersall Federal Credit Union", "ins_109510"),
          new SandboxInstitution("Tartan Bank", "ins_109511"),
          new SandboxInstitution("Houndstooth Bank", "ins_109512"),
          new SandboxInstitution("Tartan-Dominion Bank of Canada", "ins_43"),
          new SandboxInstitution("Flexible Platypus Open Banking UK", "ins_116834"),
          new SandboxInstitution("Royal Bank of Plaid", "ins_117650"), // (UK Bank)
          new SandboxInstitution(
              "Platypus OAuth Bank",
              "ins_127287"), // (for OAuth testing only, cannot be used with custom Sandbox data
          // or /sandbox/public_token/create)
          new SandboxInstitution(
              "Flexible Platypus Open Banking",
              "ins_117181") //  (for OAuth QR code authentication testing)
          );

  public static final Map<String, TypedId<BusinessId>> SANDBOX_INSTITUTIONS_BY_NAME;
  public static final Map<TypedId<BusinessId>, String> INSTITUTION_SANDBOX_ID_BY_BUSINESS_ID;

  static {
    SANDBOX_INSTITUTIONS_BY_NAME =
        SANDBOX_INSTITUTIONS.stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    SandboxInstitution::name, SandboxInstitution::businessId));
    INSTITUTION_SANDBOX_ID_BY_BUSINESS_ID =
        SANDBOX_INSTITUTIONS.stream()
            .collect(
                Collectors.toUnmodifiableMap(
                    SandboxInstitution::businessId, SandboxInstitution::sandbox_id));
  }

  /**
   * Create a Plaid sandbox link token. The strategy for creating the sandbox tokens is quite
   * different from the regular strategy, hence the different implementation for integration
   * testing.
   *
   * @param businessId identifying the business to link. One is assigned to each bank for testing
   *     purposes. The number is unique to each test run, and can be found in the constants {@link
   *     #SANDBOX_INSTITUTIONS_BY_NAME} and {@link #SANDBOX_INSTITUTIONS}. If the value provided is
   *     not a valid institution business ID, First Gingham Credit Union will be used by default.
   * @param products a list of products to expect from the institution
   * @return A link token
   * @throws IOException for connection failures and the like
   * @throws PlaidClientException if Plaid gives an error
   */
  @Override
  public String createLinkToken(TypedId<BusinessId> businessId, List<Products> products)
      throws IOException {
    if (TestHelper.businessIds.contains(businessId)) {
      return "link-token-mock-" + businessId;
    }

    SandboxPublicTokenCreateRequest request =
        new SandboxPublicTokenCreateRequest()
            .institutionId(INSTITUTION_SANDBOX_ID_BY_BUSINESS_ID.get(businessId));
    request.setInitialProducts(products);

    Response<SandboxPublicTokenCreateResponse> createResponse =
        client().sandboxPublicTokenCreate(request).execute();

    try {
      return Objects.requireNonNull(validBody(businessId, createResponse)).getPublicToken();
    } catch (PlaidClientException e) {
      TypedId<BusinessId> finalBusinessId = businessId;
      String institution =
          SANDBOX_INSTITUTIONS.stream()
              .filter(i -> i.businessId.equals(finalBusinessId))
              .findFirst()
              .toString();
      log.debug(institution);
      if (PlaidErrorCode.INVALID_INSTITUTION.equals(e.getErrorCode())) {
        log.error("Invalid institution " + institution);
      }
      throw e;
    }
  }

  @Override
  public String exchangePublicTokenForAccessToken(
      @NonNull String linkToken, @NonNull TypedId<BusinessId> businessId) throws IOException {
    if (isMockLinkToken(linkToken)) {
      return linkToken.replaceFirst("^link-token-mock-", "access-mock-");
    }
    return super.exchangePublicTokenForAccessToken(linkToken, businessId);
  }

  private boolean isMockLinkToken(@NonNull String linkToken) {
    return linkToken.startsWith("link-token-mock-");
  }

  private boolean isMockAccessToken(String accessToken) {
    return accessToken.startsWith("access-mock-");
  }

  record PlaidNumbers(List<NumbersACH> ach) {}

  record PlaidAccountResponse(List<AccountBase> accounts, PlaidNumbers numbers) {}

  @Override
  public AccountsResponse getAccounts(String accessToken, @NonNull TypedId<BusinessId> businessId)
      throws IOException {
    if (isMockAccessToken(accessToken)) {
      PlaidAccountResponse response =
          objectMapper.readValue(mockAccountsResponse.getFile(), PlaidAccountResponse.class);
      String institutionName =
          objectMapper
              .configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, true)
              .readValue(mockInstitutionResponse.getFile(), InstitutionsGetByIdResponse.class)
              .getInstitution()
              .getName();
      objectMapper.configure(DeserializationFeature.READ_ENUMS_USING_TO_STRING, false);
      return new AccountsResponse(
          accessToken, response.accounts(), response.numbers().ach(), institutionName);
    }

    return super.getAccounts(accessToken, businessId);
  }

  @Override
  public List<AccountBase> getBalances(@NonNull TypedId<BusinessId> businessId, String accessToken)
      throws IOException {
    if (isMockAccessToken(accessToken)) {
      return objectMapper
          .readValue(mockBalancesResponse.getFile(), AccountsGetResponse.class)
          .getAccounts();
    }

    return super.getBalances(businessId, accessToken);
  }

  @Override
  public OwnersResponse getOwners(String accessToken, @NonNull TypedId<BusinessId> businessId)
      throws IOException {
    if (isMockAccessToken(accessToken)) {
      return objectMapper.readValue(mockOwnersResponse.getFile(), OwnersResponse.class);
    }

    return super.getOwners(accessToken, businessId);
  }

  /*
   * Useful for writing out full responses for mocking
   */
  @Override
  protected <T> @NonNull T validBody(@NonNull TypedId<BusinessId> businessId, Response<T> response)
      throws IOException {
    T t = super.validBody(businessId, response);
    System.out.println(objectMapper.writeValueAsString(t));

    return t;
  }

  @Override
  public String getStripeBankAccountToken(
      String plaidAccessToken, String plaidAccountId, TypedId<BusinessId> businessId) {

    return "dummy_btok";
  }
}
