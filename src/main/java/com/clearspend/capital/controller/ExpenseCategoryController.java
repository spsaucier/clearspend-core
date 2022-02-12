package com.clearspend.capital.controller;

import com.clearspend.capital.controller.type.expense.ExpenseCategory;
import com.clearspend.capital.controller.type.expense.ExpenseCategoryRequest;
import com.clearspend.capital.service.ExpenseCategoryService;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/expense-categories")
@RequiredArgsConstructor
public class ExpenseCategoryController {

  private final ExpenseCategoryService expenseCategoryService;

  @GetMapping("/list")
  public List<ExpenseCategory> getCategories() {
    return expenseCategoryService.retrieveExpenseCategories().stream()
        .map(ExpenseCategory::of)
        .sorted(Comparator.comparing(ExpenseCategory::getCategoryName))
        .collect(Collectors.toList());
  }

  @PatchMapping("/{expenseCategoryId}")
  private void updateExpenseCategory(
      @PathVariable(value = "expenseCategoryId")
          @Parameter(
              required = true,
              name = "expenseCategoryId",
              description = "ID of the expense category.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          Integer expenseCategoryId,
      @Validated @RequestBody ExpenseCategoryRequest request) {
    expenseCategoryService.updateExpenseCategory(
        expenseCategoryId, request.getExpenseCategoryName());
  }
}
