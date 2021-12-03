package com.tranwall.capital.controller;

import com.tranwall.capital.common.typedid.data.BusinessBankAccountId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.CurrentUser;
import com.tranwall.capital.controller.type.adjustment.CreateAdjustmentResponse;
import com.tranwall.capital.controller.type.business.bankaccount.BankAccount;
import com.tranwall.capital.controller.type.business.bankaccount.TransactBankAccountRequest;
import com.tranwall.capital.data.model.BusinessBankAccount;
import com.tranwall.capital.data.model.enums.BusinessOnboardingStep;
import com.tranwall.capital.data.model.enums.BusinessStatus;
import com.tranwall.capital.service.AccountService.AdjustmentRecord;
import com.tranwall.capital.service.BusinessBankAccountService;
import com.tranwall.capital.service.BusinessService;
import io.swagger.v3.oas.annotations.Parameter;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/business-bank-accounts")
@RequiredArgsConstructor
@Slf4j
public class BusinessBankAccountController {

  private final BusinessService businessService;
  private final BusinessBankAccountService businessBankAccountService;

  public record LinkTokenResponse(String linkToken) {}

  @GetMapping("/link-token")
  private LinkTokenResponse linkToken() throws IOException {
    return new LinkTokenResponse(
        businessBankAccountService.getLinkToken(CurrentUser.get().businessId()));
  }

  @GetMapping(
      value = "/link-token/{linkToken}/accounts",
      produces = MediaType.APPLICATION_JSON_VALUE)
  private List<BankAccount> linkBusinessBankAccounts(@PathVariable String linkToken)
      throws IOException {
    TypedId<BusinessId> businessId = CurrentUser.get().businessId();

    List<BankAccount> bankAccounts =
        toListBankAccount(
            businessBankAccountService.linkBusinessBankAccounts(linkToken, businessId));

    // TODO: might need to be changed if it is possible to link a bank account outside of the
    // onboarding process
    businessService.updateBusiness(
        businessId, BusinessStatus.ONBOARDING, BusinessOnboardingStep.TRANSFER_MONEY, null);

    return bankAccounts;
  }

  private List<BankAccount> toListBankAccount(List<BusinessBankAccount> businessBankAccounts) {
    return businessBankAccounts.stream()
        .map(
            e ->
                new BankAccount(
                    e.getId(),
                    e.getName(),
                    e.getRoutingNumber().getEncrypted(),
                    e.getAccountNumber().getEncrypted()))
        .toList();
  }

  @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
  private List<BankAccount> getBusinessBankAccounts() {
    return toListBankAccount(
        businessBankAccountService.getBusinessBankAccounts(CurrentUser.get().businessId()));
  }

  @PostMapping(
      value = "/{businessBankAccountId}/transactions",
      produces = MediaType.APPLICATION_JSON_VALUE)
  private CreateAdjustmentResponse transact(
      @PathVariable(value = "businessBankAccountId")
          @Parameter(
              required = true,
              name = "businessBankAccountId",
              description = "ID of the businessBankAccount record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<BusinessBankAccountId> businessBankAccountId,
      @RequestBody @Validated TransactBankAccountRequest request) {
    TypedId<BusinessId> businessId = CurrentUser.get().businessId();
    AdjustmentRecord adjustmentRecord =
        businessBankAccountService.transactBankAccount(
            businessId,
            businessBankAccountId,
            request.getBankAccountTransactType(),
            request.getAmount().toAmount(),
            true);

    if (request.isOnboarding()) {
      businessService.updateBusiness(
          businessId, BusinessStatus.ACTIVE, BusinessOnboardingStep.COMPLETE, null);
    }

    return new CreateAdjustmentResponse(adjustmentRecord.adjustment().getId());
  }
}
