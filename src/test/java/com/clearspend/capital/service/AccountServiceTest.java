package com.clearspend.capital.service;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.enums.BankAccountTransactType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.service.AccountService.AccountReallocateFundsRecord;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import java.math.BigDecimal;
import javax.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Transactional
class AccountServiceTest extends BaseCapitalTest {

  @Autowired private TestHelper testHelper;
  @Autowired private AccountService accountService;
  @Autowired private BusinessBankAccountService businessBankAccountService;

  @Test
  void createAccount() {}

  @Test
  void reallocateFunds() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    BusinessBankAccount businessBankAccount =
        testHelper.createBusinessBankAccount(createBusinessRecord.business().getId());

    businessBankAccountService.transactBankAccount(
        createBusinessRecord.business().getId(),
        businessBankAccount.getId(),
        BankAccountTransactType.DEPOSIT,
        Amount.of(Currency.USD, new BigDecimal("720.51")),
        false);

    AllocationRecord allocation =
        testHelper.createAllocation(
            createBusinessRecord.business().getId(),
            "name",
            createBusinessRecord.allocationRecord().allocation().getId(),
            createBusinessRecord.user());

    AccountReallocateFundsRecord adjustmentRecord =
        accountService.reallocateFunds(
            createBusinessRecord.allocationRecord().account().getId(),
            allocation.allocation().getAccountId(),
            Amount.of(Currency.USD, new BigDecimal("241.85")));
  }
}
