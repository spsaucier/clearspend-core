package com.clearspend.capital.service;

import com.clearspend.capital.common.typedid.data.ExpenseCategoryId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.ExpenseCategory;
import com.clearspend.capital.data.model.enums.ExpenseCategoryStatus;
import com.clearspend.capital.data.repository.ExpenseCategoryRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.IntStream;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseCategoryService {
  private final ExpenseCategoryRepository categoryRepository;

  public List<ExpenseCategory> retrieveExpenseCategoriesForBusiness(
      TypedId<BusinessId> businessId) {
    return categoryRepository.findByBusinessId(businessId);
  }

  public Optional<ExpenseCategory> getExpenseCategoryById(TypedId<ExpenseCategoryId> id) {
    return categoryRepository.findById(id);
  }

  public Optional<ExpenseCategory> getExpenseCategoryByName(String name) {
    return categoryRepository.findFirstCategoryByName(name);
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
              categoryRepository.save(
                  new ExpenseCategory(
                      businessId,
                      index + 1,
                      defaultExpenseCategoryNames.get(index),
                      ExpenseCategoryStatus.ACTIVE));
            });
  }

  @Transactional
  public ExpenseCategory addExpenseCategory(TypedId<BusinessId> businessId, String categoryName) {
    // Check that we don't already have an Expense Category with the same name
    return categoryRepository
        .findFirstCategoryByName(categoryName)
        .orElseGet(
            () ->
                categoryRepository.save(
                    new ExpenseCategory(
                        businessId,
                        0, // IconRef should be zero?
                        categoryName,
                        ExpenseCategoryStatus.ACTIVE)));
  }
}
