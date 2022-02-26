package com.clearspend.capital.controller;

import com.clearspend.capital.controller.type.expense.ExpenseCategory;
import com.clearspend.capital.service.ExpenseCategoryService;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/expense-categories")
@RequiredArgsConstructor
public class ExpenseCategoryController {

  private final ExpenseCategoryService expenseCategoryService;

  @GetMapping("/list")
  List<ExpenseCategory> getCategories() {
    return expenseCategoryService.retrieveExpenseCategories().stream()
        .map(ExpenseCategory::of)
        .sorted(Comparator.comparing(ExpenseCategory::getCategoryName))
        .collect(Collectors.toList());
  }
}
