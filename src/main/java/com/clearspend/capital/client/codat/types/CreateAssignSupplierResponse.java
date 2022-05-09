package com.clearspend.capital.client.codat.types;

import com.clearspend.capital.common.typedid.data.AccountActivityId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CreateAssignSupplierResponse {
  @JsonProperty("accountActivityId")
  @NonNull
  private TypedId<AccountActivityId> accountActivityId;
}
