package com.tranwall.capital.controller.type.mcc;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.MccGroupId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.enums.I2CMccGroup;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class MccGroup {

  @JsonProperty("mccGroupId")
  @NonNull
  private TypedId<MccGroupId> mccGroupId;

  @JsonProperty("i2cMccGroupRef")
  @NonNull
  private I2CMccGroup i2cMccGroupRef;

  @JsonProperty("name")
  @NonNull
  private String name;

  public static MccGroup of(com.tranwall.capital.data.model.MccGroup mccGroup) {
    return new MccGroup(mccGroup.getId(), mccGroup.getI2cMccGroupRef(), mccGroup.getName());
  }
}
