package com.clearspend.capital.controller.type.expense;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
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

  @JsonProperty("status")
  @NonNull
  private String status;

  @JsonProperty("pathSegments")
  private String[] pathSegments;

  public static ExpenseCategory of(
      com.clearspend.capital.data.model.ExpenseCategory expenseCategory) {
    return new ExpenseCategory(
        expenseCategory.getIconRef(),
        expenseCategory.getCategoryName(),
        expenseCategory.getId().toString(),
        expenseCategory.getStatus().toString(),
        expenseCategory.getPathSegments());
  }
}
