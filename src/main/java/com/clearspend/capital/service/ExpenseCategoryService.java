package com.clearspend.capital.service;

import com.clearspend.capital.common.typedid.data.ExpenseCategoryId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.ExpenseCategory;
import com.clearspend.capital.data.model.enums.ExpenseCategoryStatus;
import com.clearspend.capital.data.repository.ExpenseCategoryRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseCategoryService {
  private final ExpenseCategoryRepository expenseCategoryRepository;

  public List<ExpenseCategory> retrieveExpenseCategoriesForBusiness(
      TypedId<BusinessId> businessId) {
    return expenseCategoryRepository.findByBusinessId(businessId);
  }

  public Optional<ExpenseCategory> getExpenseCategoryById(TypedId<ExpenseCategoryId> id) {
    return expenseCategoryRepository.findById(id);
  }

  public Optional<ExpenseCategory> getExpenseCategoryByName(String name) {
    return expenseCategoryRepository.findFirstCategoryByName(name);
  }

  public void createDefaultCategoriesForBusiness(TypedId<BusinessId> businessId) {
    List<String> defaultExpenseCategoryNames =
        List.of(
            "Assets",
            "Car Rental",
            "Entertainment",
            "Flights",
            "Meals",
            "Fuel",
            "Insurance",
            "Interest",
            "Lodging",
            "Maintenance",
            "Marketing",
            "Meetings",
            "Rent",
            "Shipping",
            "Services",
            "Software",
            "Subscriptions",
            "Supplies",
            "Utilities",
            "Taxes",
            "Training",
            "Transportation",
            "Other / Misc.");

    IntStream.range(0, defaultExpenseCategoryNames.size())
        .forEach(
            index -> {
              expenseCategoryRepository.save(
                  new ExpenseCategory(
                      businessId,
                      index + 1,
                      defaultExpenseCategoryNames.get(index),
                      ExpenseCategoryStatus.ACTIVE,
                      true));
            });
  }

  @Transactional
  public ExpenseCategory addExpenseCategory(
      TypedId<BusinessId> businessId, String categoryName, List<String> parentPath) {
    // Check that we don't already have an Expense Category with the same name
    return expenseCategoryRepository
        .findFirstCategoryByName(categoryName)
        .orElseGet(
            () -> {
              ExpenseCategory newCategory =
                  new ExpenseCategory(
                      businessId,
                      0, // IconRef should be zero?
                      categoryName,
                      ExpenseCategoryStatus.ACTIVE,
                      false);
              newCategory.setPathSegments(parentPath.toArray(new String[parentPath.size()]));
              return expenseCategoryRepository.save(newCategory);
            });
  }

  @Transactional
  public List<ExpenseCategory> disableExpenseCategories(
      List<TypedId<ExpenseCategoryId>> expenseCategories) {
    return expenseCategories.stream()
        .map(
            categoryId -> {
              ExpenseCategory currentCategory = expenseCategoryRepository.getById(categoryId);
              currentCategory.setStatus(ExpenseCategoryStatus.DISABLED);
              return expenseCategoryRepository.save(currentCategory);
            })
        .collect(Collectors.toList());
  }

  public List<ExpenseCategory> enableAllExpenseCategories(TypedId<BusinessId> businessId) {
    List<ExpenseCategory> disabledCategories =
        expenseCategoryRepository.findByBusinessIdAndStatus(
            businessId, ExpenseCategoryStatus.DISABLED);
    return disabledCategories.stream()
        .map(
            category -> {
              ExpenseCategory currentCategory = expenseCategoryRepository.getById(category.getId());
              currentCategory.setStatus(ExpenseCategoryStatus.ACTIVE);
              return expenseCategoryRepository.save(currentCategory);
            })
        .collect(Collectors.toList());
  }
}
