package com.clearspend.capital.client.plaid;

import static com.stripe.Stripe.clientId;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.repository.PlaidLogEntryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.plaid.client.model.SandboxItemResetLoginRequest;
import com.plaid.client.model.SandboxItemResetLoginResponse;
import com.plaid.client.model.SandboxItemSetVerificationStatusRequest;
import com.plaid.client.model.SandboxItemSetVerificationStatusRequest.VerificationStatusEnum;
import com.plaid.client.model.SandboxItemSetVerificationStatusResponse;
import com.plaid.client.request.PlaidApi;
import java.io.IOException;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import retrofit2.Response;

@Profile("!test & !prod")
@Component
@Slf4j
public class SandboxPlaidClient extends PlaidClient {

  public SandboxPlaidClient(
      @NonNull PlaidProperties plaidProperties,
      @NonNull PlaidApi plaidApi,
      @NonNull ObjectMapper mapper,
      @NonNull PlaidLogEntryRepository plaidLogEntryRepository) {
    super(plaidProperties, plaidApi, mapper, plaidLogEntryRepository);
  }

  /**
   * For testing only
   *
   * @param businessId the business being reset (for logging)
   * @param plaidAccessToken the accessToken to reset
   * @return true upon success
   */
  @Override
  public Boolean sandboxItemResetLogin(TypedId<BusinessId> businessId, String plaidAccessToken) {
    SandboxItemResetLoginRequest request =
        new SandboxItemResetLoginRequest().accessToken(plaidAccessToken);

    try {
      Response<SandboxItemResetLoginResponse> response =
          plaidApi.sandboxItemResetLogin(request).execute();

      return validBody(businessId, response).getResetLogin();
    } catch (IOException e) {
      throw new RuntimeException("Failed to reset login", e);
    }
  }

  @Override
  @SneakyThrows
  public @NonNull SandboxItemSetVerificationStatusResponse setVerificationStatus(
      @NonNull final TypedId<BusinessId> businessId,
      String accessToken,
      String accountId,
      VerificationStatusEnum newStatus) {
    SandboxItemSetVerificationStatusRequest request =
        new SandboxItemSetVerificationStatusRequest()
            .accessToken(accessToken)
            .secret(plaidProperties.getSecret())
            .clientId(clientId)
            .accountId(accountId);
    request.setAccessToken(accessToken);
    request.setVerificationStatus(newStatus);

    return validBody(businessId, plaidApi.sandboxItemSetVerificationStatus(request).execute());
  }
}
