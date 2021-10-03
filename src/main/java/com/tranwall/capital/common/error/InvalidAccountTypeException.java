package com.tranwall.capital.common.error;

import com.tranwall.capital.data.model.enums.AccountType;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class InvalidAccountTypeException extends RuntimeException {
  public InvalidAccountTypeException(AccountType expected, AccountType have) {
    super(String.format("Invalid accountType, expected %s but have %s", expected, have));
  }
}
