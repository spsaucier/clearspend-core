package com.clearspend.capital.common.error;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessBankAccountId;

public class BalanceNotFoundException extends RuntimeException {
  public BalanceNotFoundException(final TypedId<BusinessBankAccountId> id) {
    super(String.format("No balance found for bank account with id: %s", id));
  }
}
