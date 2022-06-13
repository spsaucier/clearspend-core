package com.clearspend.capital.data.model.business;

import com.plaid.client.model.AccountBase.VerificationStatusEnum;
import java.util.EnumSet;
import lombok.NonNull;

public enum AccountLinkStatus {
  LINKED,
  MANUAL_MICROTRANSACTION_PENDING,
  RE_LINK_REQUIRED,
  AUTOMATIC_MICROTRANSACTOIN_PENDING,
  FAILED;

  public static AccountLinkStatus of(@NonNull VerificationStatusEnum status) {
    return switch (status) {
      case PENDING_MANUAL_VERIFICATION -> MANUAL_MICROTRANSACTION_PENDING;
      case PENDING_AUTOMATIC_VERIFICATION -> AUTOMATIC_MICROTRANSACTOIN_PENDING;
      case MANUALLY_VERIFIED, AUTOMATICALLY_VERIFIED -> LINKED;
      case VERIFICATION_EXPIRED -> RE_LINK_REQUIRED;
      case VERIFICATION_FAILED -> FAILED;
    };
  }

  public static final EnumSet<AccountLinkStatus> MICROTRANSACTION_PENDING =
      EnumSet.of(MANUAL_MICROTRANSACTION_PENDING, AUTOMATIC_MICROTRANSACTOIN_PENDING);
}
