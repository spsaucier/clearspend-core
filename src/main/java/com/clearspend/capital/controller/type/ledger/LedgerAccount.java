package com.clearspend.capital.controller.type.ledger;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = As.EXISTING_PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = LedgerAllocationAccount.class, name = "ALLOCATION"),
  @JsonSubTypes.Type(value = LedgerBankAccount.class, name = "BANK"),
  @JsonSubTypes.Type(value = LedgerCardAccount.class, name = "CARD"),
  @JsonSubTypes.Type(value = LedgerMerchantAccount.class, name = "MERCHANT")
})
public interface LedgerAccount {

  @JsonProperty("type")
  LedgerAccountType getType();
}
