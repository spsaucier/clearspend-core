package com.clearspend.capital.controller.type.mcc;

import com.clearspend.capital.common.typedid.data.MccGroupId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.enums.I2CMccGroup;
import com.fasterxml.jackson.annotation.JsonProperty;
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

  public static MccGroup of(com.clearspend.capital.data.model.MccGroup mccGroup) {
    return new MccGroup(mccGroup.getId(), mccGroup.getI2cMccGroupRef(), mccGroup.getName());
  }
}
