package com.clearspend.capital.data.model.enums;

import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountCreateParams.Company.Structure;
import java.util.List;

public enum BusinessType {
  INDIVIDUAL(null, AccountCreateParams.BusinessType.INDIVIDUAL),
  SOLE_PROPRIETORSHIP(Structure.SOLE_PROPRIETORSHIP, AccountCreateParams.BusinessType.COMPANY),
  SINGLE_MEMBER_LLC(Structure.SINGLE_MEMBER_LLC, AccountCreateParams.BusinessType.COMPANY),
  MULTI_MEMBER_LLC(Structure.MULTI_MEMBER_LLC, AccountCreateParams.BusinessType.COMPANY),
  PRIVATE_PARTNERSHIP(Structure.PRIVATE_PARTNERSHIP, AccountCreateParams.BusinessType.COMPANY),
  PUBLIC_PARTNERSHIP(Structure.PUBLIC_PARTNERSHIP, AccountCreateParams.BusinessType.COMPANY),
  PRIVATE_CORPORATION(Structure.PRIVATE_CORPORATION, AccountCreateParams.BusinessType.COMPANY),
  PUBLIC_CORPORATION(Structure.PUBLIC_CORPORATION, AccountCreateParams.BusinessType.COMPANY),
  INCORPORATED_NON_PROFIT(
      Structure.INCORPORATED_NON_PROFIT, AccountCreateParams.BusinessType.NON_PROFIT),
  ACCOUNTING_FIRM(null, null),
  BANK(null, null),
  CONSULTING_FIRM(null, null);

  private final Structure stripeValue;
  private final AccountCreateParams.BusinessType stripeBusinessType;

  BusinessType(Structure stripeValue, AccountCreateParams.BusinessType stripeBusinessType) {
    this.stripeValue = stripeValue;
    this.stripeBusinessType = stripeBusinessType;
  }

  public Structure getStripeValue() {
    return stripeValue;
  }

  public AccountCreateParams.BusinessType getStripeBusinessType() {
    return stripeBusinessType;
  }

  public boolean isPartnerType() {
    return List.of(ACCOUNTING_FIRM, BANK, CONSULTING_FIRM).contains(this);
  }

  public boolean isStripeType() {
    return !isPartnerType();
  }
}
