package com.clearspend.capital.data.model.enums.network;

import com.clearspend.capital.data.model.enums.AccountActivityType;

public enum NetworkMessageType {
  AUTH_REQUEST(AccountActivityType.NETWORK_AUTHORIZATION),
  AUTH_CREATED(AccountActivityType.NETWORK_AUTHORIZATION),
  AUTH_UPDATED(AccountActivityType.NETWORK_AUTHORIZATION),
  PRE_AUTH_ADVICE(AccountActivityType.NETWORK_AUTHORIZATION),
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
