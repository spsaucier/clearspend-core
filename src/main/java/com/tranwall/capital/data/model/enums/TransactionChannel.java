package com.tranwall.capital.data.model.enums;

/** Defines transaction origin */
public enum TransactionChannel {

  /** ATM based transaction */
  ATM,

  /** Transaction originated from pos terminals */
  POS,

  /** Mobile order/telephone order transactions */
  MOTO,

  /** Online (ecomm) transactions */
  ONLINE
}
