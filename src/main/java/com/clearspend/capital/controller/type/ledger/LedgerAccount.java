package com.clearspend.capital.controller.type.ledger;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @Type(value = LedgerAllocationAccount.class, name = "ALLOCATION"),
  @Type(value = LedgerBankAccount.class, name = "BANK"),
  @Type(value = LedgerCardAccount.class, name = "CARD"),
  @Type(value = LedgerMerchantAccount.class, name = "MERCHANT")
})
public interface LedgerAccount {

  @JsonProperty("type")
  LedgerAccountType getType();
}
