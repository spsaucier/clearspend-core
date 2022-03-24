package com.clearspend.capital.controller.type.ledger;

import com.clearspend.capital.controller.type.activity.Merchant;
import com.clearspend.capital.data.model.embedded.MerchantDetails;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class LedgerMerchantAccount implements LedgerAccount {

  @JsonProperty("merchantInfo")
  @NonNull
  private Merchant merchantInfo;

  @Override
  public LedgerAccountType getType() {
    return LedgerAccountType.MERCHANT;
  }

  public LedgerMerchantAccount(MerchantDetails merchantDetails) {
    merchantInfo = Merchant.toMerchant(merchantDetails);
  }
}
