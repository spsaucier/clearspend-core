package com.clearspend.capital.controller.business;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessBankAccountId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.adjustment.CreateAdjustmentResponse;
import com.clearspend.capital.controller.type.business.bankaccount.BankAccount;
import com.clearspend.capital.controller.type.business.bankaccount.TransactBankAccountRequest;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.enums.BusinessOnboardingStep;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.clearspend.capital.service.AccountService.AdjustmentAndHoldRecord;
import com.clearspend.capital.service.BusinessBankAccountService;
import com.clearspend.capital.service.BusinessService;
import com.clearspend.capital.service.type.CurrentUser;
import io.swagger.v3.oas.annotations.Parameter;
import java.io.IOException;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

  @Value("${clearspend.ach.hold.place:true}")
  private boolean placeHold;

  private final BusinessService businessService;
  private final BusinessBankAccountService businessBankAccountService;

  public record LinkTokenResponse(String linkToken) {}

  @PostConstruct
  private void init() {
    log.info("configValue: clearspend.ach.hold.place: {}", placeHold);
  }

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
      @RequestBody @Validated TransactBankAccountRequest request,
      HttpServletRequest httpServletRequest)
      throws IOException {
    TypedId<BusinessId> businessId = CurrentUser.get().businessId();

    // TODO: we are working with frontend team to split this endpoint into 2 separate endpoints
    // Once new endpoint will be used, some of the code below which is related to onboarding will be
    // removed
    if (request.isOnboarding()) {
      businessBankAccountService.registerExternalBank(
          businessId,
          businessBankAccountId,
          httpServletRequest.getRemoteAddr(),
          httpServletRequest.getHeader("User-Agent"));
    }

    AdjustmentAndHoldRecord adjustmentAndHoldRecord =
        businessBankAccountService.transactBankAccount(
            businessId,
            businessBankAccountId,
            request.getBankAccountTransactType(),
            request.getAmount().toAmount(),
            placeHold);

    if (request.isOnboarding()) {
      businessService.updateBusiness(
          businessId, BusinessStatus.ACTIVE, BusinessOnboardingStep.COMPLETE, null);
    }

    return new CreateAdjustmentResponse(adjustmentAndHoldRecord.adjustment().getId());
  }

  /**
   * New endpoint for frontend to be used for initial onboarding transaction. It will create and
   * link bank account, execute transaction itself, and will mark the business as fully active Once
   * this will be used, old endpoint will be cleaned-up and onboarding related code will be removed
   */
  @PostMapping(
      value = "/{businessBankAccountId}/onboard",
      produces = MediaType.APPLICATION_JSON_VALUE)
  private CreateAdjustmentResponse onboardingTransaction(
      @PathVariable(value = "businessBankAccountId")
          @Parameter(
              required = true,
              name = "businessBankAccountId",
              description = "ID of the businessBankAccount record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<BusinessBankAccountId> businessBankAccountId,
      @RequestBody @Validated TransactBankAccountRequest request,
      HttpServletRequest httpServletRequest)
      throws IOException {
    TypedId<BusinessId> businessId = CurrentUser.get().businessId();

    // TODO: 3 calls below will have to be moved to either a dedicated "onboarding service"
    // or to one of existing services, care should be taken then to not introduce circular
    // dependencies
    businessBankAccountService.registerExternalBank(
        businessId,
        businessBankAccountId,
        httpServletRequest.getRemoteAddr(),
        httpServletRequest.getHeader("User-Agent"));

    AdjustmentAndHoldRecord adjustmentAndHoldRecord =
        businessBankAccountService.transactBankAccount(
            businessId,
            businessBankAccountId,
            request.getBankAccountTransactType(),
            request.getAmount().toAmount(),
            true);

    businessService.updateBusiness(
        businessId, BusinessStatus.ACTIVE, BusinessOnboardingStep.COMPLETE, null);

    return new CreateAdjustmentResponse(adjustmentAndHoldRecord.adjustment().getId());
  }
}
