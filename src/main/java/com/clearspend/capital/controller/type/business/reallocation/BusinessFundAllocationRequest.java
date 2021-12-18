package com.clearspend.capital.controller.type.business.reallocation;

import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.data.model.enums.BusinessReallocationType;
import com.fasterxml.jackson.annotation.JsonProperty;
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
