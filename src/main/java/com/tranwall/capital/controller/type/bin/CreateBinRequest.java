package com.tranwall.capital.controller.type.bin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CreateBinRequest {

  @Sensitive
  @JsonProperty("bin")
  private String bin;

  @Sensitive
  @JsonProperty("name")
  private String name;
}
