package com.clearspend.capital.controller.type.chartOfAccounts;

import com.clearspend.capital.common.typedid.data.ExpenseCategoryId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class AddChartOfAccountsMappingRequest {
  @JsonProperty("accountRef")
  @NonNull
  private String accountRef;

  @JsonProperty("expenseCategoryId")
  @NonNull
  private TypedId<ExpenseCategoryId> expenseCategoryId;
}
