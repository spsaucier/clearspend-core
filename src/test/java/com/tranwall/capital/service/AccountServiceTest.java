package com.tranwall.capital.service;

import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.repository.UserRepository;
import com.tranwall.capital.service.AccountService.AccountReallocateFundsRecord;
import com.tranwall.capital.service.AccountService.AdjustmentRecord;
import com.tranwall.capital.service.BusinessService.BusinessAndAllocationsRecord;
import java.math.BigDecimal;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class AccountServiceTest extends BaseCapitalTest {

  @Autowired private ServiceHelper serviceHelper;
  @Autowired private AccountService accountService;

  @Autowired private UserRepository userRepository;

  private BusinessAndAllocationsRecord businessAndAllocationsRecord;
  private Bin bin;
  private Program program;

  @BeforeEach
  public void setup() {
    if (bin == null) {
      bin = serviceHelper.createBin();
      program = serviceHelper.createProgram(bin);
      businessAndAllocationsRecord = serviceHelper.createBusiness(program);
    }
  }

  @Test
  void createAccount() {}

  @Test
  void depositFunds() {
    AdjustmentRecord adjustmentRecord =
        accountService.depositFunds(
            businessAndAllocationsRecord.business().getId(),
            Amount.of(Currency.USD, new BigDecimal("1000")));
  }

  @Test
  void withdrawFunds() {
    accountService.depositFunds(
        businessAndAllocationsRecord.business().getId(),
        Amount.of(Currency.USD, new BigDecimal("700.51")));
    AdjustmentRecord adjustmentRecord =
        accountService.withdrawFunds(
            businessAndAllocationsRecord.business().getId(),
            Amount.of(Currency.USD, new BigDecimal("241.85")));
  }

  @Test
  void reallocateFunds() {
    accountService.depositFunds(
        businessAndAllocationsRecord.business().getId(),
        Amount.of(Currency.USD, new BigDecimal("700.51")));
    AccountReallocateFundsRecord adjustmentRecord =
        accountService.reallocateFunds(
            businessAndAllocationsRecord.businessAccount().getId(),
            businessAndAllocationsRecord.allocationRecords().get(0).account().getId(),
            Amount.of(Currency.USD, new BigDecimal("241.85")));
  }
}
