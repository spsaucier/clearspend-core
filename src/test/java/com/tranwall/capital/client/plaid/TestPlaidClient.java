package com.tranwall.capital.client.plaid;

import com.plaid.client.model.Products;
import com.plaid.client.model.SandboxPublicTokenCreateRequest;
import com.plaid.client.model.SandboxPublicTokenCreateResponse;
import com.plaid.client.request.PlaidApi;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import retrofit2.Response;

@Profile("test")
@Component
@Slf4j
public class TestPlaidClient extends PlaidClient {

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

  public TestPlaidClient(@NonNull PlaidProperties plaidProperties, @NonNull PlaidApi plaidApi) {
    super(plaidProperties, plaidApi);
  }

  /**
   * Create a Plaid sandbox link token. The strategy for creating the sandbox tokens is quite
   * different from the regular strategy, hence the different implementation for integration
   * testing.
   *
   * @param bankId identifying the institution (bank) to link. The number is unique to each test
   *     run, and can be found in the constants {@link #SANDBOX_INSTITUTIONS_BY_NAME} and {@link
   *     #SANDBOX_INSTITUTIONS}. If the value provided is not a valid institution business ID, First
   *     Gingham Credit Union will be used by default.
   * @return A link token
   * @throws IOException for connection failures and the like
   * @throws PlaidClientException if Plaid gives an error
   */
  @Override
  public String createLinkToken(TypedId<BusinessId> bankId) throws IOException {
    if (!INSTITUTION_SANDBOX_ID_BY_BUSINESS_ID.containsKey(bankId)) {
      log.info("Using default institution: First Gingham Credit Union");
      bankId = SANDBOX_INSTITUTIONS_BY_NAME.get("First Gingham Credit Union");
    }
    final TypedId<BusinessId> businessId = bankId;
    SandboxPublicTokenCreateRequest request =
        new SandboxPublicTokenCreateRequest()
            .institutionId(INSTITUTION_SANDBOX_ID_BY_BUSINESS_ID.get(businessId));
    request.setInitialProducts(Arrays.asList(Products.AUTH, Products.IDENTITY));

    Response<SandboxPublicTokenCreateResponse> createResponse =
        client().sandboxPublicTokenCreate(request).execute();

    try {
      return Objects.requireNonNull(validBody(createResponse)).getPublicToken();
    } catch (PlaidClientException e) {
      String institution =
          SANDBOX_INSTITUTIONS.stream()
              .filter(i -> i.businessId.equals(businessId))
              .findFirst()
              .toString();
      log.debug(institution);
      if (e.getMessage().contains("INVALID_INSTITUTION")) {
        throw new PlaidClientException(e.getMessage(), institution);
      } else {
        throw e;
      }
    }
  }
}
