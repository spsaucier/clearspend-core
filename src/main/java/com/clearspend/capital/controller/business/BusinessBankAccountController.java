package com.clearspend.capital.controller.business;

import com.clearspend.capital.common.error.InvalidKycStepException;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessBankAccountId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.adjustment.CreateAdjustmentResponse;
import com.clearspend.capital.controller.type.business.bankaccount.BankAccount;
import com.clearspend.capital.controller.type.business.bankaccount.TransactBankAccountRequest;
import com.clearspend.capital.data.model.business.AccountLinkStatus;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.enums.BusinessOnboardingStep;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.clearspend.capital.service.AccountService.AdjustmentAndHoldRecord;
import com.clearspend.capital.service.BusinessBankAccountService;
import com.clearspend.capital.service.BusinessService;
import com.clearspend.capital.service.BusinessService.OnboardingBusinessOp;
import com.clearspend.capital.service.type.CurrentUser;
import io.swagger.v3.oas.annotations.Parameter;
import java.io.IOException;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/business-bank-accounts")
@RequiredArgsConstructor
@Slf4j
public class BusinessBankAccountController {

  @Value("${clearspend.ach.hold.standard:true}")
  private boolean standardHold;

  private final BusinessService businessService;
  private final BusinessBankAccountService businessBankAccountService;

  public record LinkTokenResponse(String linkToken) {}

  @PostConstruct
  private void init() {
    log.info("configValue: clearspend.ach.hold.standard: {}", standardHold);
  }

  @GetMapping("/link-token")
  LinkTokenResponse linkToken() throws IOException {
    Business business = businessService.getBusiness(CurrentUser.getActiveBusinessId(), true);
    // in case we are in steps previous to LINK_ACCOUNT then is not allowed to continue
    if (business.getOnboardingStep().canTransferTo(BusinessOnboardingStep.LINK_ACCOUNT)) {
      throw new InvalidKycStepException();
    }
    return new LinkTokenResponse(
        businessBankAccountService.getLinkToken(CurrentUser.getActiveBusinessId()));
  }

  @GetMapping("/re-link/{businessBankAccountId}")
  LinkTokenResponse reLink(
      @PathVariable(value = "businessBankAccountId")
          @Parameter(
              required = true,
              name = "businessBankAccountId",
              description = "ID of the businessBankAccount record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<BusinessBankAccountId> businessBankAccountId)
      throws IOException {
    return new LinkTokenResponse(
        businessBankAccountService.reLink(
            CurrentUser.getActiveBusinessId(), businessBankAccountId));
  }

  @PutMapping(
      value = "/link-token/{publicToken}/accounts",
      produces = MediaType.APPLICATION_JSON_VALUE)
  @OnboardingBusinessOp(
      reviewer = "Craig Miller",
      explanation = "This method uses the Business for onboarding tasks")
  ResponseEntity<List<BankAccount>> linkBusinessBankAccounts(@PathVariable String publicToken)
      throws IOException {
    TypedId<BusinessId> businessId = CurrentUser.getActiveBusinessId();

    List<BankAccount> bankAccounts =
        toListBankAccount(
            businessBankAccountService.linkBusinessBankAccounts(publicToken, businessId));

    Business business = businessService.getBusiness(businessId, true);
    if (business.getStatus() == BusinessStatus.ONBOARDING) {
      businessService.updateBusinessForOnboarding(
          businessId, BusinessStatus.ONBOARDING, BusinessOnboardingStep.TRANSFER_MONEY, null);
    }

    int status =
        bankAccounts.stream()
            .filter(a -> a.getAccountLinkStatus() != AccountLinkStatus.LINKED)
            .findFirst()
            .map(a -> 202)
            .orElse(200);

    return ResponseEntity.status(status).body(bankAccounts);
  }

  private List<BankAccount> toListBankAccount(List<BusinessBankAccount> businessBankAccounts) {
    return businessBankAccounts.stream()
        .map(
            e ->
                new BankAccount(
                    e.getId(),
                    e.getName(),
                    e.getRoutingNumber().getEncrypted(),
                    e.getAccountNumber().getEncrypted(),
                    e.getLinkStatus()))
        .toList();
  }

  @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
  List<BankAccount> getBusinessBankAccounts() {
    return toListBankAccount(
        businessBankAccountService.getBusinessBankAccounts(
            CurrentUser.getActiveBusinessId(), true));
  }

  @PostMapping(
      value = "/{businessBankAccountId}/transactions",
      produces = MediaType.APPLICATION_JSON_VALUE)
  CreateAdjustmentResponse transact(
      @PathVariable(value = "businessBankAccountId")
          @Parameter(
              required = true,
              name = "businessBankAccountId",
              description = "ID of the businessBankAccount record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<BusinessBankAccountId> businessBankAccountId,
      @RequestBody @Validated TransactBankAccountRequest request) {
    AdjustmentAndHoldRecord adjustmentAndHoldRecord =
        businessBankAccountService.transactBankAccount(
            CurrentUser.getActiveBusinessId(),
            businessBankAccountId,
            CurrentUser.getUserId(),
            request.getBankAccountTransactType(),
            request.getAmount().toAmount(),
            standardHold);

    return new CreateAdjustmentResponse(adjustmentAndHoldRecord.adjustment().getId());
  }

  @PostMapping(
      value = "/{businessBankAccountId}/register",
      produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<?> register(
      @PathVariable(value = "businessBankAccountId")
          @Parameter(
              required = true,
              name = "businessBankAccountId",
              description = "ID of the businessBankAccount record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<BusinessBankAccountId> businessBankAccountId) {
    try {
      businessBankAccountService.registerExternalBank(
          CurrentUser.getActiveBusinessId(), businessBankAccountId);
    } catch (IllegalStateException e) {
      // The account is not correctly linked
      return ResponseEntity.status(424).build();
    }

    return ResponseEntity.ok().build();
  }

  @PostMapping(
      value = "/{businessBankAccountId}/fail_link",
      produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<?> failAccountLinking(
      @PathVariable(value = "businessBankAccountId")
          @Parameter(
              required = true,
              name = "businessBankAccountId",
              description = "ID of the businessBankAccount record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<BusinessBankAccountId> businessBankAccountId) {
    businessBankAccountService.failAccountLinking(
        CurrentUser.getActiveBusinessId(), businessBankAccountId);

    return ResponseEntity.ok().build();
  }

  @PostMapping(
      value = "/{businessBankAccountId}/unregister",
      produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<?> unregister(
      @PathVariable(value = "businessBankAccountId")
          @Parameter(
              required = true,
              name = "businessBankAccountId",
              description = "ID of the businessBankAccount record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<BusinessBankAccountId> businessBankAccountId) {
    businessBankAccountService.unregisterExternalBank(
        CurrentUser.getActiveBusinessId(), businessBankAccountId);

    return ResponseEntity.ok().build();
  }
}
