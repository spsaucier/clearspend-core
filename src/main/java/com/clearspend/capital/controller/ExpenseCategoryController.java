package com.clearspend.capital.controller;

import com.clearspend.capital.common.typedid.data.ExpenseCategoryId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.expense.ExpenseCategory;
import com.clearspend.capital.service.ExpenseCategoryService;
import com.clearspend.capital.service.type.CurrentUser;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/expense-categories")
@RequiredArgsConstructor
public class ExpenseCategoryController {

  private final ExpenseCategoryService expenseCategoryService;

  @GetMapping("/list")
  List<ExpenseCategory> getCategories() {
    return expenseCategoryService
        .retrieveExpenseCategoriesForBusiness(CurrentUser.getBusinessId())
        .stream()
        .map(ExpenseCategory::of)
        .sorted(Comparator.comparing(ExpenseCategory::getCategoryName))
        .collect(Collectors.toList());
  }

  @PostMapping("/disable")
  List<ExpenseCategory> disableExpenseCategories(
      @Validated @RequestBody List<TypedId<ExpenseCategoryId>> expenseCategories) {
    return expenseCategoryService.disableExpenseCategories(expenseCategories).stream()
        .map(ExpenseCategory::of)
        .collect(Collectors.toList());
  }

  @PostMapping("/enable-all")
  List<ExpenseCategory> enableAllExpenseCategories() {
    return expenseCategoryService.enableAllExpenseCategories(CurrentUser.getBusinessId()).stream()
        .map(ExpenseCategory::of)
        .collect(Collectors.toList());
  }
}
