package com.clearspend.capital.controller.type.allocation;

import com.clearspend.capital.controller.type.Amount;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.hibernate.validator.constraints.Range;

@Data
@RequiredArgsConstructor
public class AllocationAutoTopUpConfigCreateRequest {

  @JsonProperty("monthlyDay")
  @NonNull
  @NotNull(message = "day of mount required")
  @Range(max = 28, min = 1, message = "Last day of the month to do auto top-up will be 28.")
  private Integer monthlyDay;

  @JsonProperty("amount")
  @NonNull
  @NotNull(message = "amount required")
  private Amount amount;
}
