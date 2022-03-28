package com.clearspend.capital.controller.type.chartOfAccounts;

import com.clearspend.capital.common.typedid.data.ExpenseCategoryId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class ChartOfAccountsMappingResponse {
  @JsonProperty("accountRef")
  @NonNull
  private String accountRef;

  @JsonProperty("categoryIconRef")
  @NonNull
  private Integer categoryIconRef;

  @JsonProperty("expenseCategoryId")
  @NonNull
  private TypedId<ExpenseCategoryId> expenseCategoryId;
}
