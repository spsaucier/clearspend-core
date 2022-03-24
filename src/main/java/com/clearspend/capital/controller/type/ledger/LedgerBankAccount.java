package com.clearspend.capital.controller.type.ledger;

import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.embedded.BankAccountDetails;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class LedgerBankAccount implements LedgerAccount {

  @JsonProperty("bankInfo")
  @NonNull
  private BankInfo bankInfo;

  @Override
  public LedgerAccountType getType() {
    return LedgerAccountType.BANK;
  }

  public static LedgerBankAccount of(BankAccountDetails bankAccount) {
    if (bankAccount == null) {
      return null;
    }

    return new LedgerBankAccount(new BankInfo(bankAccount.getName(), bankAccount.getLastFour()));
  }

  public static LedgerBankAccount of(BusinessBankAccount bankAccount) {
    if (bankAccount == null) {
      return null;
    }

    String accountNumber = bankAccount.getAccountNumber().getEncrypted();

    return new LedgerBankAccount(
        new BankInfo(bankAccount.getName(), accountNumber.substring(accountNumber.length() - 4)));
  }
}
