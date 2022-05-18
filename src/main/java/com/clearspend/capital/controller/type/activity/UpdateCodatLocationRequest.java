package com.clearspend.capital.controller.type.activity;

import com.clearspend.capital.common.typedid.data.CodatCategoryId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UpdateCodatLocationRequest {
  @JsonProperty("locationId")
  @Nullable
  private TypedId<CodatCategoryId> locationId;
}
