package com.tranwall.capital.controller.nonprod;

import com.tranwall.capital.common.typedid.data.BusinessBankAccountId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.adjustment.CreateAdjustmentResponse;
import com.tranwall.capital.controller.type.business.bankaccount.TransactBankAccountRequest;
import com.tranwall.capital.service.AccountService.AdjustmentRecord;
import com.tranwall.capital.service.BusinessBankAccountService;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This controller contains end points that are not deployed top production but allow the caller to
 * do things that they normally wouldn't be a able to do.
 */
@Profile("!prod")
@RestController
@RequestMapping("/non-production")
@RequiredArgsConstructor
public class NonProductionController {

  private final BusinessBankAccountService businessBankAccountService;

  @PostMapping(
      value = "/business-bank-accounts/{businessBankAccountId}/transactions",
      produces = MediaType.APPLICATION_JSON_VALUE)
  private CreateAdjustmentResponse transact(
      @RequestHeader(name = "businessId") TypedId<BusinessId> businessId,
      @PathVariable(value = "businessBankAccountId")
          @ApiParam(
              required = true,
              name = "businessBankAccountId",
              value = "ID of the businessBankAccount record.",
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
            false);
    return new CreateAdjustmentResponse(adjustmentRecord.adjustment().getId());
  }
}