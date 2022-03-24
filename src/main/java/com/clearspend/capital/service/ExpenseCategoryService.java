package com.clearspend.capital.service;

import com.clearspend.capital.common.typedid.data.ExpenseCategoryId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.ExpenseCategory;
import com.clearspend.capital.data.model.enums.ExpenseCategoryStatus;
import com.clearspend.capital.data.repository.ExpenseCategoryRepository;
import java.util.List;
import java.util.Optional;
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

  public Optional<ExpenseCategory> getExpenseCategoryById(TypedId<ExpenseCategoryId> id) {
    return categoryRepository.findById(id);
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
    defaultExpenseCategoryNames.stream()
        .forEach(
            categoryName -> {
              categoryRepository.save(
                  new ExpenseCategory(businessId, 0, categoryName, ExpenseCategoryStatus.ACTIVE));
            });
  }
}
