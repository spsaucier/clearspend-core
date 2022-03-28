package com.clearspend.capital.controller.type.chartOfAccounts;

import com.clearspend.capital.common.typedid.data.ExpenseCategoryId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.fasterxml.jackson.annotation.JsonProperty;
import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;
import lombok.With;

@Data
@AllArgsConstructor
public class AddChartOfAccountsMappingRequest {
  @JsonProperty("accountRef")
  @NonNull
  private final String accountRef;

  @JsonProperty("expenseCategoryId")
  @Nullable
  private @With TypedId<ExpenseCategoryId> expenseCategoryId;

  @JsonProperty("expenseCategoryName")
  @Nullable
  private @With String expenseCategoryName;

  public AddChartOfAccountsMappingRequest(@NonNull String accountRef) {
    this.accountRef = accountRef;
  }
}
