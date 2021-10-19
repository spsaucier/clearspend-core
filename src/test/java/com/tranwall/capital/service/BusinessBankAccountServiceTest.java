package com.tranwall.capital.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.error.InsufficientFundsException;
import com.tranwall.capital.common.typedid.data.BusinessBankAccountId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.FundsTransactType;
import com.tranwall.capital.service.AccountService.AdjustmentRecord;
import com.tranwall.capital.service.BusinessService.BusinessAndAllocationsRecord;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class BusinessBankAccountServiceTest extends BaseCapitalTest {

  @Autowired private TestHelper testHelper;
  @Autowired private BusinessBankAccountService bankAccountService;

  private Bin bin;
  private Program program;

  @BeforeEach
  public void setup() {
    if (bin == null) {
      bin = testHelper.createBin();
      program = testHelper.createProgram(bin);
    }
  }

  @Test
  void depositFunds() {
    BusinessAndAllocationsRecord businessAndAllocationsRecord = testHelper.createBusiness(program);
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(
            businessAndAllocationsRecord.businessAccount().getBusinessId());
    AdjustmentRecord adjustmentRecord =
        bankAccountService.transactBankAccount(
            businessAndAllocationsRecord.business().getId(),
            businessBankAccountId,
            FundsTransactType.DEPOSIT,
            Amount.of(Currency.USD, new BigDecimal("1000")),
            true);
  }

  @Test
  void withdrawFunds_success() {
    BusinessAndAllocationsRecord businessAndAllocationsRecord = testHelper.createBusiness(program);
    TypedId<BusinessId> businessId = businessAndAllocationsRecord.businessAccount().getBusinessId();
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(businessId);
    bankAccountService.transactBankAccount(
        businessAndAllocationsRecord.business().getId(),
        businessBankAccountId,
        FundsTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("700.51")),
        false);
    AdjustmentRecord adjustmentRecord =
        bankAccountService.transactBankAccount(
            businessAndAllocationsRecord.business().getId(),
            businessBankAccountId,
            FundsTransactType.WITHDRAW,
            Amount.of(Currency.USD, new BigDecimal("241.85")),
            true);
  }

  @Test
  void withdrawFunds_insufficientBalance() {
    BusinessAndAllocationsRecord businessAndAllocationsRecord = testHelper.createBusiness(program);
    TypedId<BusinessId> businessId = businessAndAllocationsRecord.businessAccount().getBusinessId();
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(businessId);
    bankAccountService.transactBankAccount(
        businessAndAllocationsRecord.business().getId(),
        businessBankAccountId,
        FundsTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("710.51")),
        true);

    InsufficientFundsException insufficientFundsException =
        assertThrows(
            InsufficientFundsException.class,
            () ->
                bankAccountService.transactBankAccount(
                    businessAndAllocationsRecord.business().getId(),
                    businessBankAccountId,
                    FundsTransactType.WITHDRAW,
                    Amount.of(Currency.USD, new BigDecimal("1.85")),
                    true));
  }
}
