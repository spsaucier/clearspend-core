package com.clearspend.capital.controller.type.card;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.controller.type.activity.FilterAmount;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.annotations.VisibleForTesting;
import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class SearchCardRequest {

  @JsonProperty("users")
  private List<TypedId<UserId>> users;

  @JsonProperty("allocations")
  private List<TypedId<AllocationId>> allocations;

  @JsonProperty("searchText")
  private String searchText;

  @JsonProperty("balance")
  private FilterAmount balanceRange;

  @JsonProperty("activeCards")
  private Boolean includeActiveCards = true;

  @JsonProperty("frozenCards")
  private Boolean includeFrozenCards = true;

  @JsonProperty("cancelledCards")
  private Boolean includeCancelledCards = false;

  @JsonProperty("physical")
  private Boolean includePhysicalCards = true;

  @JsonProperty("virtual")
  private Boolean includeVirtualCards = true;

  @NonNull
  @NotNull(message = "Page request is mandatory")
  @JsonProperty("pageRequest")
  PageRequest pageRequest;

  @VisibleForTesting
  public void addUser(TypedId<UserId> userId) {
    if (users == null) {
      users = new ArrayList<>();
    }
    users.add(userId);
  }

  @VisibleForTesting
  public void addAllocation(TypedId<AllocationId> allocationId) {
    if (allocations == null) {
      allocations = new ArrayList<>();
    }
    allocations.add(allocationId);
  }
}
