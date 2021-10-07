package com.tranwall.capital.service;

import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.data.repository.UserRepository;
import com.tranwall.capital.service.AccountService.AccountReallocateFundsRecord;
import com.tranwall.capital.service.AccountService.AdjustmentRecord;
import com.tranwall.capital.service.BusinessService.BusinessRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

class AccountServiceTest extends BaseCapitalTest {

  @Autowired private ServiceHelper serviceHelper;
  @Autowired private AccountService accountService;

  @Autowired private UserRepository userRepository;

  private BusinessRecord businessRecord;
  private Bin bin;
  private Program program;

  @BeforeEach
  public void setup() {
    if (bin == null) {
      bin = serviceHelper.createBin();
      program = serviceHelper.createProgram(bin);
      businessRecord = serviceHelper.createBusiness(program);
    }
  }

  @Test
  void createAccount() {}

  @Test
  void depositFunds() {
    AdjustmentRecord adjustmentRecord =
        accountService.depositFunds(
            businessRecord.businessAccount().getId(),
            Amount.of(Currency.USD, new BigDecimal("1000")));
  }

  @Test
  void withdrawFunds() {
    accountService.depositFunds(
        businessRecord.businessAccount().getId(),
        Amount.of(Currency.USD, new BigDecimal("700.51")));
    AdjustmentRecord adjustmentRecord =
        accountService.withdrawFunds(
            businessRecord.businessAccount().getId(),
            Amount.of(Currency.USD, new BigDecimal("241.85")));
  }

  @Test
  void reallocateFunds() {
    accountService.depositFunds(
        businessRecord.businessAccount().getId(),
        Amount.of(Currency.USD, new BigDecimal("700.51")));
    AccountReallocateFundsRecord adjustmentRecord =
        accountService.reallocateFunds(
            businessRecord.businessAccount().getId(),
            businessRecord.allocationRecords().get(0).account().getId(),
            Amount.of(Currency.USD, new BigDecimal("241.85")));
  }
}
