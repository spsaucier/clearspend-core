package com.clearspend.capital.client.codat.types;

import com.clearspend.capital.common.typedid.data.CodatCategoryId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class SetCategoryNamesRequest {
  @JsonProperty("categoryId")
  @NonNull
  TypedId<CodatCategoryId> categoryId;

  @JsonProperty("name")
  @NonNull
  String name;
}
