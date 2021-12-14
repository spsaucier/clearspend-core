package com.tranwall.capital.controller.type.allocation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.MccGroupId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.account.Account;
import com.tranwall.capital.controller.type.card.limits.CurrencyLimit;
import com.tranwall.capital.controller.type.user.UserData;
import com.tranwall.capital.data.model.enums.TransactionChannel;
import com.tranwall.capital.service.AllocationService.AllocationDetailsRecord;
import java.util.List;
import java.util.Set;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class AllocationDetailsResponse {

  @NonNull
  @JsonProperty("allocation")
  private Allocation allocation;

  @NonNull
  @JsonProperty("owner")
  private UserData owner;

  @NonNull
  @JsonProperty("limits")
  private List<CurrencyLimit> limits;

  @NonNull
  @JsonProperty("disabledMccGroups")
  private List<TypedId<MccGroupId>> disabledMccGroups;

  @NonNull
  @JsonProperty("disabledTransactionChannels")
  Set<TransactionChannel> disabledTransactionChannels;

  public static AllocationDetailsResponse of(AllocationDetailsRecord record) {
    Allocation allocation =
        new Allocation(
            record.allocation().getId(),
            record.allocation().getName(),
            record.allocation().getOwnerId(),
            Account.of(record.account()));
    allocation.setParentAllocationId(record.allocation().getParentAllocationId());

    return new AllocationDetailsResponse(
        allocation,
        new UserData(record.owner()),
        CurrencyLimit.ofMap(record.transactionLimit().getLimits()),
        record.transactionLimit().getDisabledMccGroups(),
        record.transactionLimit().getDisabledTransactionChannels());
  }
}
