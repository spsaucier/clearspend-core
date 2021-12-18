package com.clearspend.capital.controller.type.bin;

import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.common.typedid.data.BinId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class Bin {

  @JsonProperty("binId")
  @NonNull
  private TypedId<BinId> binId;

  @Sensitive
  @JsonProperty("bin")
  private String bin;

  @Sensitive
  @JsonProperty("name")
  private String name;

  public Bin(com.clearspend.capital.data.model.Bin bin) {
    this.binId = bin.getId();
    this.bin = bin.getBin();
    this.name = bin.getName();
  }
}
