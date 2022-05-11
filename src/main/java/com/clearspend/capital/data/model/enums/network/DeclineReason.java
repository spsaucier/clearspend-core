package com.clearspend.capital.data.model.enums.network;

public enum DeclineReason {
  INSUFFICIENT_FUNDS,
  INVALID_CARD_STATUS,
  UNLINKED_CARD,
  CARD_NOT_FOUND,
  LIMIT_EXCEEDED,
  OPERATION_LIMIT_EXCEEDED,
  SPEND_CONTROL_VIOLATED,
  ADDRESS_POSTAL_CODE_MISMATCH,
  CVC_MISMATCH,
  EXPIRY_MISMATCH,
  BUSINESS_SUSPENSION,

  // Stripe failed inbound/outbound transfer reasons:
  ST_ACCOUNT_CLOSED,
  ST_ACCOUNT_FROZEN,
  ST_BANK_ACCOUNT_RESTRICTED,
  ST_BANK_OWNERSHIP_CHANGED,
  ST_COULD_NOT_PROCESS,
  ST_INVALID_ACCOUNT_NUMBER,
  ST_INCORRECT_ACCOUNT_HOLDER_NAME,
  ST_INVALID_CURRENCY,
  ST_NO_ACCOUNT,
  ST_DECLINED,
  ST_FAILED,
  ST_CANCELLED,
  ST_UNKNOWN;

  public static DeclineReason fromStripeTransferFailure(String code) {
    return switch (code) {
      case "account_closed" -> ST_ACCOUNT_CLOSED;
      case "account_frozen" -> ST_ACCOUNT_FROZEN;
      case "bank_account_restricted" -> ST_BANK_ACCOUNT_RESTRICTED;
      case "bank_ownership_changed" -> ST_BANK_OWNERSHIP_CHANGED;
      case "could_not_process" -> ST_COULD_NOT_PROCESS;
      case "invalid_account_number" -> ST_INVALID_ACCOUNT_NUMBER;
      case "incorrect_account_holder_name" -> ST_INCORRECT_ACCOUNT_HOLDER_NAME;
      case "invalid_currency" -> ST_INVALID_CURRENCY;
      case "no_account" -> ST_NO_ACCOUNT;
      case "declined" -> ST_DECLINED;
      default -> ST_UNKNOWN;
    };
  }
}
