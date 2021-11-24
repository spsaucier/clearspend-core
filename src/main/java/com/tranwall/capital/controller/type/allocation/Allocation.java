package com.tranwall.capital.controller.type.allocation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.account.Account;
import com.tranwall.capital.service.AllocationService.AllocationRecord;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class Allocation {

  @JsonProperty("allocationId")
  @NonNull
  @NotNull(message = "allocationId required")
  private TypedId<AllocationId> allocationId;

  @JsonProperty("name")
  @NonNull
  @NotNull(message = "name required")
  private String name;

  @JsonProperty("parentAllocationId")
  private TypedId<AllocationId> parentAllocationId;

  @JsonProperty("childrenAllocationIds")
  private List<TypedId<AllocationId>> childrenAllocationIds = new ArrayList<>();

  @JsonProperty("account")
  @NonNull
  @NotNull(message = "account required")
  private Account account;

  public static Allocation of(AllocationRecord record) {
    Allocation result =
        new Allocation(
            record.allocation().getId(),
            record.allocation().getName(),
            Account.of(record.account()));

    result.setParentAllocationId(record.allocation().getParentAllocationId());

    return result;
  }
}
