package com.clearspend.capital.service;

import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.data.model.ExpenseCategory;
import com.clearspend.capital.data.repository.ExpenseCategoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseCategoryService {
  private final ExpenseCategoryRepository categoryRepository;

  public List<ExpenseCategory> retrieveExpenseCategories() {
    return categoryRepository.findAll();
  }

  public ExpenseCategory retrieveExpenseCategory(Integer iconRef) {
    return categoryRepository
        .findByIconRef(iconRef)
        .orElseThrow(() -> new RecordNotFoundException(Table.EXPENSE_CATEGORY, iconRef));
  }
}
