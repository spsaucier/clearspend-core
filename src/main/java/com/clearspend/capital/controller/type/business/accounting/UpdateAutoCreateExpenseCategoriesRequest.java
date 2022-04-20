package com.clearspend.capital.controller.type.business.accounting;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class UpdateAutoCreateExpenseCategoriesRequest {
  @JsonProperty("autoCreateExpenseCategories")
  @NonNull
  private Boolean autoCreateExpenseCategories;
}
