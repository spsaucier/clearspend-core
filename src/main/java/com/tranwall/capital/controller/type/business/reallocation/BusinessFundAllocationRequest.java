package com.tranwall.capital.controller.type.business.reallocation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.AccountId;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.Amount;
import com.tranwall.capital.data.model.enums.BusinessReallocationType;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class BusinessFundAllocationRequest {

  @JsonProperty("allocationId")
  @NonNull
  private TypedId<AllocationId> allocationId;

  @JsonProperty("accountId")
  @NonNull
  private TypedId<AccountId> accountId;

  @JsonProperty("fundsTransactType")
  @NonNull
  private BusinessReallocationType businessReallocationType;

  @JsonProperty("amount")
  @NonNull
  private Amount amount;
}
