package com.clearspend.capital.controller.type.expense;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ExpenseCategory {

  @JsonProperty("iconRef")
  @NonNull
  private Integer iconRef;

  @JsonProperty("categoryName")
  @NonNull
  private String categoryName;

  @JsonProperty("expenseCategoryId")
  @NonNull
  private String expenseCategoryId;

  public static ExpenseCategory of(
      com.clearspend.capital.data.model.ExpenseCategory expenseCategory) {
    return new ExpenseCategory(
        expenseCategory.getIconRef(),
        expenseCategory.getCategoryName(),
        expenseCategory.getId().toString());
  }
}
