package com.tranwall.capital.controller.type.allocation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.data.model.enums.Currency;
import java.util.UUID;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CreateAllocationRequest {

  @JsonProperty("programId")
  @NonNull
  @NotNull(message = "programId required")
  private UUID programId;

  @JsonProperty("name")
  @NonNull
  @NotNull(message = "name required")
  private String name;

  @JsonProperty("parentAllocationId")
  private UUID parentAllocationId;

  @JsonProperty("currency")
  @NonNull
  @NotNull(message = "currency required")
  private Currency currency;
}
