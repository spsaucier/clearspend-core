package com.clearspend.capital.controller.type.card;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.card.limits.CurrencyLimit;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.PaymentType;
import com.clearspend.capital.data.repository.CardRepositoryCustom.CardAllocationDetailsRecord;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CardAllocationSpendControls {
  @NonNull
  @NotNull
  @JsonProperty("allocationId")
  @Schema(description = "The ID of the allocation connected with this card.")
  private TypedId<AllocationId> allocationId;

  @JsonProperty("allocationName")
  @Schema(description = "The name of the allocation connected with this card.")
  private String allocationName;

  @JsonProperty("limits")
  @Schema(
      description =
          "Currency limits for this card/allocation pair. If a create/update operation and this is null, the existing value for the allocation/card will be used.")
  private List<CurrencyLimit> limits;

  @JsonProperty("disabledMccGroups")
  @Schema(
      description =
          "Disabled MCC Groups for this card/allocation pair. If a create/update operation and this is null, the existing value for the allocation/card will be used.")
  private Set<MccGroup> disabledMccGroups;

  @JsonProperty("disabledPaymentTypes")
  @Schema(
      description =
          "Disabled Payment Types for this card/allocation pair. If a create/update operation and this is null, the existing value for the allocation/card will be used.")
  private Set<PaymentType> disabledPaymentTypes;

  @JsonProperty("disableForeign")
  @Schema(
      description =
          "Disable foreign transactions for this card/allocation pair. If a create/update operation and this is null, the existing value for the allocation/card will be used.")
  private Boolean disableForeign;

  public static CardAllocationSpendControls of(final Allocation allocation) {
    final CardAllocationSpendControls controls =
        new CardAllocationSpendControls(allocation.getId());
    controls.setAllocationName(allocation.getName());
    return controls;
  }

  public static CardAllocationSpendControls of(final CardAllocationDetailsRecord record) {
    final CardAllocationSpendControls controls =
        new CardAllocationSpendControls(record.allocationId());
    controls.setAllocationName(record.allocationName());
    controls.setLimits(CurrencyLimit.ofMap(record.transactionLimit().getLimits()));
    controls.setDisabledMccGroups(record.transactionLimit().getDisabledMccGroups());
    controls.setDisabledPaymentTypes(record.transactionLimit().getDisabledPaymentTypes());
    controls.setDisableForeign(record.transactionLimit().getDisableForeign());
    return controls;
  }
}
