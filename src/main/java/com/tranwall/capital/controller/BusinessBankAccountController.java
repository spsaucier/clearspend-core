package com.tranwall.capital.controller;

import com.tranwall.capital.controller.type.adjustment.CreateAdjustmentResponse;
import com.tranwall.capital.controller.type.business.bankaccount.BankAccount;
import com.tranwall.capital.controller.type.business.bankaccount.TransactBankAccountRequest;
import com.tranwall.capital.service.AccountService.AdjustmentRecord;
import com.tranwall.capital.service.BusinessBankAccountService;
import io.swagger.annotations.ApiParam;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
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

  @NonNull
  private BusinessBankAccountService businessBankAccountService;

  @GetMapping("/link-token")
  private String linkToken(@RequestHeader(name = "businessId") UUID businessId) throws IOException {
    // TODO: Get business UUID from JWT
    return businessBankAccountService.getLinkToken(businessId);
  }

  @GetMapping(value = "/accounts/{linkToken}", produces = MediaType.APPLICATION_JSON_VALUE)
  private List<BusinessBankAccountService.BusinessBankAccountRecord> accounts(
      @RequestHeader(name = "businessId") UUID businessId, @PathVariable String linkToken)
      throws IOException {
    // TODO: Get business UUID from JWT
    return businessBankAccountService.getAccounts(linkToken, businessId);
  }

  @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
  private List<BankAccount> accounts(@RequestHeader(name = "businessId") UUID businessId) {
    // TODO: Get business UUID from JWT
    return businessBankAccountService.getAccounts(businessId).stream()
        .map(
            e ->
                new BankAccount(
                    e.getId(),
                    e.getName(),
                    e.getAccountNumber()
                        .getEncrypted()
                        .substring(e.getAccountNumber().getEncrypted().length() - 4)))
        .collect(Collectors.toList());
  }

  @PostMapping(
      value = "/{businessBankAccountId}/transactions",
      produces = MediaType.APPLICATION_JSON_VALUE)
  private CreateAdjustmentResponse transact(
      @RequestHeader(name = "businessId") UUID businessId,
      @PathVariable(value = "businessBankAccountId")
      @ApiParam(
          required = true,
          name = "businessBankAccountId",
          value = "ID of the businessBankAccount record.",
          example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          UUID businessBankAccountId,
      @RequestBody @Validated TransactBankAccountRequest request) {
    // TODO: Get business UUID from JWT
    AdjustmentRecord adjustmentRecord =
        businessBankAccountService.transactBankAccount(
            businessId,
            businessBankAccountId,
            request.getBankAccountTransactType(),
            request.getAmount().toAmount());
    return new CreateAdjustmentResponse(adjustmentRecord.adjustment().getId());
  }
}
