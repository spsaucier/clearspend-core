package com.clearspend.capital.controller.type.allocation;

import com.clearspend.capital.controller.type.account.Account;
import com.clearspend.capital.controller.type.card.limits.CurrencyLimit;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.PaymentType;
import com.clearspend.capital.service.AllocationService.AllocationDetailsRecord;
import com.fasterxml.jackson.annotation.JsonProperty;
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
  @JsonProperty("limits")
  private List<CurrencyLimit> limits;

  @NonNull
  @JsonProperty("disabledMccGroups")
  private Set<MccGroup> disabledMccGroups;

  @NonNull
  @JsonProperty("disabledPaymentTypes")
  Set<PaymentType> disabledPaymentTypes;

  @NonNull
  @JsonProperty("disableForeign")
  private Boolean disableForeign;

  public static AllocationDetailsResponse of(AllocationDetailsRecord record) {
    Allocation allocation =
        new Allocation(
            record.allocation().getId(),
            record.allocation().getName(),
            Account.of(record.account()));
    allocation.setParentAllocationId(record.allocation().getParentAllocationId());
    allocation.setArchived(record.allocation().isArchived());

    return new AllocationDetailsResponse(
        allocation,
        CurrencyLimit.ofMap(record.transactionLimit().getLimits()),
        record.transactionLimit().getDisabledMccGroups(),
        record.transactionLimit().getDisabledPaymentTypes(),
        record.transactionLimit().getDisableForeign());
  }
}
