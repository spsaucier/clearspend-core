package com.clearspend.capital.controller.type.allocation;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.controller.type.account.Account;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.fasterxml.jackson.annotation.JsonProperty;
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

  @JsonProperty("ownerId")
  @NonNull
  @NotNull(message = "owner is required")
  private TypedId<UserId> ownerId;

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
            record.allocation().getOwnerId(),
            Account.of(record.account()));

    result.setParentAllocationId(record.allocation().getParentAllocationId());

    return result;
  }
}
