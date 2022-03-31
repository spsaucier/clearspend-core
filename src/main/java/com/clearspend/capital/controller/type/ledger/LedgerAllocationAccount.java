package com.clearspend.capital.controller.type.ledger;

import com.clearspend.capital.controller.type.activity.AllocationInfo;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.embedded.AllocationDetails;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class LedgerAllocationAccount implements LedgerAccount {

  @JsonProperty("allocationInfo")
  @NonNull
  private AllocationInfo allocationInfo;

  @Override
  public LedgerAccountType getType() {
    return LedgerAccountType.ALLOCATION;
  }

  public static LedgerAllocationAccount of(AllocationDetails allocationDetails) {
    if (allocationDetails == null) {
      return null;
    }

    return new LedgerAllocationAccount(
        new AllocationInfo(allocationDetails.getId(), allocationDetails.getName()));
  }

  public static LedgerAllocationAccount of(Allocation allocation) {
    return new LedgerAllocationAccount(
        new AllocationInfo(allocation.getId(), allocation.getName()));
  }
}
