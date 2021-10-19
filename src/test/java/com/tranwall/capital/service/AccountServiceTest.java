package com.tranwall.capital.service;

import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.typedid.data.BusinessBankAccountId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.model.enums.FundsTransactType;
import com.tranwall.capital.service.AccountService.AccountReallocateFundsRecord;
import com.tranwall.capital.service.BusinessService.BusinessAndAllocationsRecord;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class AccountServiceTest extends BaseCapitalTest {

  @Autowired private TestHelper testHelper;
  @Autowired private AccountService accountService;
  @Autowired private BusinessBankAccountService businessBankAccountService;

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
  void createAccount() {}

  @Test
  void reallocateFunds() {
    BusinessAndAllocationsRecord businessAndAllocationsRecord = testHelper.createBusiness(program);
    TypedId<BusinessBankAccountId> businessBankAccountId =
        testHelper.createBusinessBankAccount(
            businessAndAllocationsRecord.businessAccount().getBusinessId());
    businessBankAccountService.transactBankAccount(
        businessAndAllocationsRecord.business().getId(),
        businessBankAccountId,
        FundsTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("720.51")),
        false);
    AccountReallocateFundsRecord adjustmentRecord =
        accountService.reallocateFunds(
            businessAndAllocationsRecord.businessAccount().getId(),
            businessAndAllocationsRecord.allocationRecords().get(0).account().getId(),
            Amount.of(Currency.USD, new BigDecimal("241.85")));
  }
}
