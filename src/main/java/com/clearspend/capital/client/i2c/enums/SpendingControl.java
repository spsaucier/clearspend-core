package com.clearspend.capital.client.i2c.enums;

import com.clearspend.capital.client.i2c.util.I2CEnumSerializable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SpendingControl implements I2CEnumSerializable<SpendingControl> {
  BLOCK_ONLINE_TRANSACTIONS("CH_ECOM_CONFG"),
  BLOCK_POS_PURCHASES("CH_POS_CONFG"),
  BLOCK_ATM_WITHDRAWALS("CH_ATM_CONFG"),
  BLOCK_BY_PHONE_OR_MAIL_TRANSACTIONS("CH_MOto_CONFG"),
  BLOCK_INTERNATIONAL_TRANSACTIONS("CH_INTL_CONFG"),
  DAILY_PURCHASE_LIMIT("D_PUR_LMT"),
  DAILY_CASH_WITHDRAWAL_LIMIT("D_CA_PUR_LMT"),
  PURCHASE_TRANSACTION_AMOUNT_LIMIT("TX_PUR_AMT_LMT"),
  CASH_WITHDRAWAL_TRANSACTION_AMOUNT_LIMIT("TX_CA_AMT_LMT"),
  CARD_TO_BANK_MAX_AMOUNT_LIMIT("TX_C2B_AMT_LMT"),
  BANK_TO_CARD_MAX_AMOUNT_LIMIT("TX_B2C_AMT_LMT"),
  PURCHASE_LIMIT("PUR_LMT"),
  CASH_WITHDRAWAL_LIMIT("CA_PUR_LMT");

  private final String i2cSpendingControl;

  @Override
  public String serialize() {
    return i2cSpendingControl;
  }
}
