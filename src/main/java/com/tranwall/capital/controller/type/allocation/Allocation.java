package com.tranwall.capital.controller.type.allocation;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.controller.type.account.Account;
import java.util.UUID;
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
  private UUID allocationId;

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

  @JsonProperty("account")
  @NonNull
  @NotNull(message = "account required")
  private Account account;
}
