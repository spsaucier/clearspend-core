package com.clearspend.capital.data.model.enums.network;

public enum DeclineReason {
  INSUFFICIENT_FUNDS,
  INVALID_CARD_STATUS,
  CARD_NOT_FOUND,
  LIMIT_EXCEEDED,
  ADDRESS_POSTAL_CODE_MISMATCH,
  CVC_MISMATCH,
  EXPIRY_MISMATCH,

  // Stripe inbound transfer reasons:
  SIT_ACCOUNT_CLOSED,
  SIT_ACCOUNT_FROZEN,
  SIT_BANK_ACCOUNT_RESTRICTED,
  SIT_BANK_OWNERSHIP_CHANGED,
  SIT_COULD_NOT_PROCESS,
  SIT_INVALID_ACCOUNT_NUMBER,
  SIT_INCORRECT_ACCOUNT_HOLDER_NAME,
  SIT_INVALID_CURRENCY,
  SIT_NO_ACCOUNT,
  SIT_UNKNOWN;

  public static DeclineReason fromStripeInboundTransferFailure(String code) {
    return switch (code) {
      case "account_closed" -> SIT_ACCOUNT_CLOSED;
      case "account_frozen" -> SIT_ACCOUNT_FROZEN;
      case "bank_account_restricted" -> SIT_BANK_ACCOUNT_RESTRICTED;
      case "bank_ownership_changed" -> SIT_BANK_OWNERSHIP_CHANGED;
      case "could_not_process" -> SIT_COULD_NOT_PROCESS;
      case "invalid_account_number" -> SIT_INVALID_ACCOUNT_NUMBER;
      case "incorrect_account_holder_name" -> SIT_INCORRECT_ACCOUNT_HOLDER_NAME;
      case "invalid_currency" -> SIT_INVALID_CURRENCY;
      case "no_account" -> SIT_NO_ACCOUNT;
      default -> SIT_UNKNOWN;
    };
  }
}
