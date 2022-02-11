package com.clearspend.capital.controller.type.allocation;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.controller.type.card.limits.CurrencyLimit;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.PaymentType;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.Set;
import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CreateAllocationRequest {

  @JsonProperty("name")
  @NonNull
  @NotNull(message = "name required")
  @Schema(example = "advertisement", description = "name of the department/ allocation")
  private String name;

  @NonNull
  @NotNull(message = "parent allocation id required")
  @JsonProperty("parentAllocationId")
  @Schema(example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
  private TypedId<AllocationId> parentAllocationId;

  @JsonProperty("ownerId")
  @NonNull
  @NotNull(message = "owner is required")
  private TypedId<UserId> ownerId;

  @JsonProperty("amount")
  @NonNull
  @NotNull(message = "amount required")
  @Schema(example = "{currency=USD,amount=100}")
  private Amount amount;

  @NonNull
  @NotEmpty(message = "limits must be provided")
  @JsonProperty("limits")
  @Valid
  private List<CurrencyLimit> limits;

  @NonNull
  @NotNull(message = "disabled msc groups collection is required")
  @JsonProperty("disabledMccGroups")
  private Set<MccGroup> disabledMccGroups;

  @NonNull
  @NotNull(message = "disabled payment types collection is required")
  @JsonProperty("disabledPaymentTypes")
  Set<PaymentType> disabledPaymentTypes;
}
