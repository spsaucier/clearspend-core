package com.clearspend.capital.controller.type.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Optional;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class UpdateAccountActivityRequest {

  @JsonProperty("notes")
  @NonNull
  @NotNull(message = "notes on the account activity")
  private String notes;

  /** Expense category code */
  @JsonProperty("iconRef")
  @Min(1)
  private Optional<Integer> iconRef;
}
