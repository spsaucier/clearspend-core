package com.clearspend.capital.controller.type.bin;

import com.clearspend.capital.common.typedid.data.BinId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.fasterxml.jackson.annotation.JsonProperty;
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
