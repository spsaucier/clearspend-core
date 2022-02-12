package com.clearspend.capital.controller.type.expense;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ExpenseCategoryRequest {

  @JsonProperty("expenseCategoryName")
  private String expenseCategoryName;
}
