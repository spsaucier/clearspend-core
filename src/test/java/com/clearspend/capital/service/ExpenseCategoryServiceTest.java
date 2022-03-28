package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.data.model.ExpenseCategory;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.ExpenseCategoryStatus;
import com.clearspend.capital.data.repository.ExpenseCategoryRepository;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class ExpenseCategoryServiceTest extends BaseCapitalTest {

  @Autowired private ExpenseCategoryRepository expenseCategoryRepository;

  @Autowired private ExpenseCategoryService expenseCategoryService;

  @Autowired private TestHelper testHelper;

  @Test
  void retrieveExpenseCategories() {
    Business business = testHelper.createBusiness().business();

    log.info("AllCategories: {}", expenseCategoryRepository.findByBusinessId(business.getId()));
    List<ExpenseCategory> foundCategories =
        expenseCategoryService.retrieveExpenseCategoriesForBusiness(business.getId());
    assertThat(foundCategories).isNotNull();
    assertThat(foundCategories.size()).isEqualTo(23);
    assertThat(foundCategories.get(0).getIconRef()).isEqualTo(1);
  }

  @Test
  void canDisableAndEnableExpenseCategories() {
    Business business = testHelper.createBusiness().business();
    List<ExpenseCategory> foundCategories =
        expenseCategoryService.retrieveExpenseCategoriesForBusiness(business.getId());
    List<ExpenseCategory> disabledCategories =
        expenseCategoryService.disableExpenseCategories(
            List.of(foundCategories.get(0).getId(), foundCategories.get(1).getId()));

    assertThat(disabledCategories.size()).isEqualTo(2);
    assertThat(
            expenseCategoryRepository
                .findByBusinessIdAndStatus(business.getId(), ExpenseCategoryStatus.DISABLED)
                .size())
        .isEqualTo(2);

    expenseCategoryService.enableAllExpenseCategories(business.getId());

    assertThat(
            expenseCategoryRepository
                .findByBusinessIdAndStatus(business.getId(), ExpenseCategoryStatus.DISABLED)
                .size())
        .isEqualTo(0);
  }
}
