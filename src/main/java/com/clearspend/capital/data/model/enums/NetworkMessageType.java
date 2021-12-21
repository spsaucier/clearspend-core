package com.clearspend.capital.data.model.enums;

public enum NetworkMessageType {
  PRE_AUTH(AccountActivityType.NETWORK_PRE_AUTH),
  PRE_AUTH_ADVICE(AccountActivityType.NETWORK_PRE_AUTH),
  FINANCIAL_AUTH(AccountActivityType.NETWORK_FINANCIAL_AUTH),
  FINANCIAL_AUTH_ADVICE(AccountActivityType.NETWORK_FINANCIAL_AUTH), // Forced Post
  REVERSAL(AccountActivityType.NETWORK_REVERSAL),
  REVERSAL_ADVICE(AccountActivityType.NETWORK_REVERSAL),
  ;

  private final AccountActivityType accountActivityType;

  NetworkMessageType(AccountActivityType accountActivityType) {
    this.accountActivityType = accountActivityType;
  }

  public AccountActivityType getAccountActivityType() {
    return accountActivityType;
  }
}
