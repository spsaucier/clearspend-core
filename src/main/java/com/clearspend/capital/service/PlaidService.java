package com.clearspend.capital.service;

import com.clearspend.capital.common.advice.AssignApplicationSecurityContextAdvice.SecureWebhook;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.PlaidLogEntry;
import com.clearspend.capital.data.model.business.AccountLinkStatus;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.enums.PlaidResponseType;
import com.clearspend.capital.data.repository.PlaidLogEntryRepository;
import com.clearspend.capital.service.type.CurrentUser;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@SecureWebhook
public class PlaidService {

  private final BusinessBankAccountService businessBankAccountService;
  private final PlaidLogEntryRepository plaidLogEntryRepository;
  private final TwilioService twilioService;
  private final BusinessService businessService;
  private final ObjectMapper mapper;

  private final Map<String, BiConsumer<String, Map<String, Object>>> hooks =
      Map.of("AUTH", this::authWebhook);

  @PreAuthorize("hasGlobalPermission('APPLICATION')")
  public void webhook(Map<String, Object> request) {
    String webhookType =
        Optional.ofNullable((String) request.get("webhook_type"))
            .orElseThrow(() -> new InvalidRequestException("webhook_type required"));
    String itemId = (String) request.get("item_id");
    Optional.ofNullable(hooks.get(webhookType))
        .ifPresentOrElse(
            f -> f.accept(itemId, request),
            () -> {
              throw new UnsupportedOperationException(webhookType);
            });
  }

  private void validateWebhook() {
    // TODO CAP-1272 https://plaid.com/docs/api/webhooks/webhook-verification/
    Map<String, Object> jwtClaims = CurrentUser.getClaims().orElseThrow();
    if (!"ES256".equals(jwtClaims.get("alg"))) {
      throw new AccessDeniedException("");
    }
    if (Optional.ofNullable(jwtClaims.get("iat"))
            .map(o -> Integer.valueOf(String.valueOf(o)))
            .orElse(0)
        < System.currentTimeMillis() / 1000.0 - 300) {
      throw new AccessDeniedException("expired JWT");
    }
    String kid = (String) jwtClaims.get("kid");
    // ...
    // security here is not critical yet because we are just receiving notice that it's
    // time to check on an account again.  If/when we process more webhooks, we might find
    // it's important to check more thoroughly.
  }

  @SneakyThrows
  private void authWebhook(String itemId, Map<String, Object> request) {
    final String plaidAccountRef = (String) request.get("account_id");
    // String verificationStatus = request.get("webhook_code");
    // verification status gets checked in updateLinkedAccounts

    BusinessBankAccount account =
        businessBankAccountService
            .findAccountByPlaidAccountRef(plaidAccountRef)
            .orElseThrow(
                () -> new RecordNotFoundException(Table.BUSINESS_BANK_ACCOUNT, plaidAccountRef));
    final TypedId<BusinessId> businessId = account.getBusinessId();

    plaidLogEntryRepository.save(
        new PlaidLogEntry(businessId, mapper.writeValueAsString(request), PlaidResponseType.OTHER));
    account = businessBankAccountService.updateLinkedAccount(account).orElseThrow();
    if (account.getLinkStatus().equals(AccountLinkStatus.AUTOMATIC_MICROTRANSACTOIN_PENDING)) {
      // At this point, the updateLinkedAccount should work and update the status to linked.
      // Since it didn't, either this message was spurious (probably not), or it indicates
      // genuine failure.
      businessBankAccountService.failAccountLinking(account);
      businessBankAccountService.unregisterExternalBank(businessId, account.getId());
      return;
    }
    businessBankAccountService.registerExternalBankForService(businessId, account.getId());

    // Email the business to indicate the update is complete
    Business business = businessService.getBusiness(businessId).business();
    twilioService.sendFinancialAccountReadyEmail(
        business.getBusinessEmail().getEncrypted(), business.getBusinessName());
  }
}
