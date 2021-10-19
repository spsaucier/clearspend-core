package com.tranwall.capital.controller.type.bin;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.BinId;
import com.tranwall.capital.common.typedid.data.TypedId;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CreateBinResponse {

  @JsonProperty("binId")
  @NonNull
  @NotNull(message = "binId required")
  private TypedId<BinId> binId;
}
