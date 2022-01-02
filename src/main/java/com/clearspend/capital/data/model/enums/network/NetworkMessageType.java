package com.clearspend.capital.data.model.enums.network;

import com.clearspend.capital.data.model.enums.AccountActivityType;

public enum NetworkMessageType {
  AUTH_REQUEST(AccountActivityType.NETWORK_AUTHORIZATION),
  AUTH_CREATED(AccountActivityType.NETWORK_AUTHORIZATION),
  AUTH_UPDATED(AccountActivityType.NETWORK_AUTHORIZATION),
  TRANSACTION_CREATED(AccountActivityType.NETWORK_CAPTURE),
  ;

  private final AccountActivityType accountActivityType;

  NetworkMessageType(AccountActivityType accountActivityType) {
    this.accountActivityType = accountActivityType;
  }

  public AccountActivityType getAccountActivityType() {
    return accountActivityType;
  }
}
