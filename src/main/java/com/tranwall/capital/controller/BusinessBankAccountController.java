package com.tranwall.capital.controller;

import static com.tranwall.capital.controller.Common.BUSINESS_ID;

import com.tranwall.capital.common.typedid.data.BusinessBankAccountId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.adjustment.CreateAdjustmentResponse;
import com.tranwall.capital.controller.type.business.bankaccount.BankAccount;
import com.tranwall.capital.controller.type.business.bankaccount.TransactBankAccountRequest;
import com.tranwall.capital.data.model.BusinessBankAccount;
import com.tranwall.capital.service.AccountService.AdjustmentRecord;
import com.tranwall.capital.service.BusinessBankAccountService;
import io.swagger.v3.oas.annotations.Parameter;
import java.io.IOException;
import java.util.List;
import lombok.Data;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/business-bank-accounts")
@CrossOrigin
@Data
@Slf4j
public class BusinessBankAccountController {

  @NonNull private BusinessBankAccountService businessBankAccountService;

  public record LinkTokenResponse(String linkToken) {}

  @GetMapping("/link-token")
  private LinkTokenResponse linkToken(
      @RequestHeader(name = BUSINESS_ID) TypedId<BusinessId> businessId) throws IOException {
    // TODO: Get business UUID from JWT
    return new LinkTokenResponse(businessBankAccountService.getLinkToken(businessId));
  }

  @GetMapping(
      value = "/link-token/{linkToken}/accounts",
      produces = MediaType.APPLICATION_JSON_VALUE)
  private List<BankAccount> linkBusinessBankAccounts(
      @RequestHeader(name = BUSINESS_ID) TypedId<BusinessId> businessId,
      @PathVariable String linkToken)
      throws IOException {
    // TODO: Get business UUID from JWT
    return toListBankAccount(
        businessBankAccountService.linkBusinessBankAccounts(linkToken, businessId));
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
  private List<BankAccount> getBusinessBankAccounts(
      @RequestHeader(name = BUSINESS_ID) TypedId<BusinessId> businessId) {
    // TODO: Get business UUID from JWT
    return toListBankAccount(businessBankAccountService.getBusinessBankAccounts(businessId));
  }

  @PostMapping(
      value = "/{businessBankAccountId}/transactions",
      produces = MediaType.APPLICATION_JSON_VALUE)
  private CreateAdjustmentResponse transact(
      @RequestHeader(name = BUSINESS_ID) TypedId<BusinessId> businessId,
      @PathVariable(value = "businessBankAccountId")
          @Parameter(
              required = true,
              name = "businessBankAccountId",
              description = "ID of the businessBankAccount record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<BusinessBankAccountId> businessBankAccountId,
      @RequestBody @Validated TransactBankAccountRequest request) {
    // TODO: Get business UUID from JWT
    AdjustmentRecord adjustmentRecord =
        businessBankAccountService.transactBankAccount(
            businessId,
            businessBankAccountId,
            request.getBankAccountTransactType(),
            request.getAmount().toAmount(),
            true);
    return new CreateAdjustmentResponse(adjustmentRecord.adjustment().getId());
  }
}
