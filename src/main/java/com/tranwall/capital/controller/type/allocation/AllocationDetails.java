package com.tranwall.capital.controller.type.allocation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.controller.type.account.Account;
import com.tranwall.capital.controller.type.user.UserData;
import com.tranwall.capital.service.AllocationService.AllocationDetailsRecord;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class AllocationDetails {

  @NonNull
  @JsonProperty("allocation")
  private Allocation allocation;

  @NonNull
  @JsonProperty("owner")
  private UserData owner;

  public AllocationDetails(AllocationDetailsRecord record) {
    allocation =
        new Allocation(
            record.allocation().getId(),
            record.allocation().getName(),
            record.allocation().getOwnerId(),
            Account.of(record.account()));
    allocation.setParentAllocationId(record.allocation().getParentAllocationId());

    owner = new UserData(record.owner());
  }
}
