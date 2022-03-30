package com.clearspend.capital.controller.type.chartOfAccounts;

import com.clearspend.capital.common.typedid.data.ExpenseCategoryId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nullable;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class AddChartOfAccountsMappingRequest {
  @JsonProperty("accountRef")
  @NonNull
  private final String accountRef;

  @JsonProperty("expenseCategoryId")
  @Nullable
  private TypedId<ExpenseCategoryId> expenseCategoryId;

  @JsonProperty("expenseCategoryName")
  @Nullable
  private String expenseCategoryName;

  @JsonProperty("fullyQualifiedCategory")
  @Nullable
  private String fullyQualifiedCategory;
}
