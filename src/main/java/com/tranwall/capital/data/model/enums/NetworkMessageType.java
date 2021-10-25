package com.tranwall.capital.data.model.enums;

public enum NetworkMessageType {
  PRE_AUTH_TRANSACTION(100),
  PRE_AUTH_TRANSACTION_ADVICE(120),
  FINANCIAL_TRANSACTION(200),
  FINANCIAL_TRANSACTION_ADVICE(220), // Forced Post
  REVERSAL_TRANSACTION(400),
  REVERSAL_TRANSACTION_ADVICE(420),
  SERVICE_FEE_TRANSACTION(755);

  private int mti;

  NetworkMessageType(int mti) {
    this.mti = mti;
  }
}
