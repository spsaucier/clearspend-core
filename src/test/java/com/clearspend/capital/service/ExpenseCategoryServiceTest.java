package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.data.model.ExpenseCategory;
import com.clearspend.capital.data.repository.ExpenseCategoryRepository;
import java.util.List;
import java.util.Optional;
import javax.transaction.Transactional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
@Transactional
class ExpenseCategoryServiceTest extends BaseCapitalTest {

  @Autowired private ExpenseCategoryRepository expenseCategoryRepository;

  @Autowired private ExpenseCategoryService expenseCategoryService;

  @Test
  void retrieveExpenseCategories() {
    log.info("AllCategories: {}", expenseCategoryRepository.findAll());
    List<ExpenseCategory> foundCategories = expenseCategoryService.retrieveExpenseCategories();
    assertThat(foundCategories).isNotNull();
    assertThat(foundCategories.size()).isEqualTo(23);
  }

  @SneakyThrows
  @Test
  void updateExpenseCategory_success() {
    Optional<ExpenseCategory> expenseCategory = expenseCategoryRepository.findByIconRef(5);
    log.info(String.valueOf(expenseCategory));
    if (!expenseCategory.get().getIconRef().toString().isBlank()) {
      expenseCategory.get().setCategoryName("Testing");
    }
    expenseCategoryService.updateExpenseCategory(
        expenseCategory.get().getIconRef(), expenseCategory.get().getCategoryName());
    Optional<ExpenseCategory> expenseCategory1 = expenseCategoryRepository.findByIconRef(5);
    log.info(String.valueOf(expenseCategory1));
  }
}
