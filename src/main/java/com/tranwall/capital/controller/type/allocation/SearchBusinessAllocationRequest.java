package com.tranwall.capital.controller.type.allocation;

import com.fasterxml.jackson.annotation.JsonProperty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class SearchBusinessAllocationRequest {

  @JsonProperty("name")
  @NonNull
  @NotNull(message = "name required")
  @Size(min = 3, max = 200, message = "minimum of 3 characters, maximum of 200")
  private String name;
}
