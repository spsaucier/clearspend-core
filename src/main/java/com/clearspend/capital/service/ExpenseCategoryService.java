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
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExpenseCategoryService {
  private final ExpenseCategoryRepository expenseCategoryRepository;

  @PreAuthorize("hasRootPermission(#businessId, 'CATEGORIZE')")
  public List<ExpenseCategory> retrieveExpenseCategoriesForBusiness(
      TypedId<BusinessId> businessId) {
    return expenseCategoryRepository.findByBusinessId(businessId);
  }

  @PostAuthorize("hasPermission(returnObject.orElse(null), 'CATEGORIZE')")
  public Optional<ExpenseCategory> getExpenseCategoryById(TypedId<ExpenseCategoryId> id) {
    return expenseCategoryRepository.findById(id);
  }

  void createDefaultCategoriesForBusiness(TypedId<BusinessId> businessId) {
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
  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_CATEGORIES')")
  public ExpenseCategory addExpenseCategory(
      TypedId<BusinessId> businessId, String categoryName, List<String> parentPath) {
    return expenseCategoryRepository
        .findFirstCategoryByNameAndStatus(categoryName, ExpenseCategoryStatus.ACTIVE)
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
  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_CATEGORIES|APPLICATION')")
  public List<ExpenseCategory> disableExpenseCategories(
      final TypedId<BusinessId> businessId, final List<TypedId<ExpenseCategoryId>> categoryIds) {
    final List<ExpenseCategory> categories =
        expenseCategoryRepository.findAllByBusinessIdAndIdIn(businessId, categoryIds).stream()
            .map(
                category -> {
                  category.setStatus(ExpenseCategoryStatus.DISABLED);
                  return category;
                })
            .toList();
    return expenseCategoryRepository.saveAll(categories);
  }

  @Transactional
  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_CATEGORIES')")
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

  @Transactional
  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_CATEGORIES')")
  public List<ExpenseCategory> enableDefaultExpenseCategories(TypedId<BusinessId> businessId) {
    List<ExpenseCategory> disabledCategories =
        expenseCategoryRepository.findByBusinessIdAndStatusAndIsDefaultCategory(
            businessId, ExpenseCategoryStatus.DISABLED, Boolean.TRUE);
    return disabledCategories.stream()
        .map(
            category -> {
              ExpenseCategory currentCategory = expenseCategoryRepository.getById(category.getId());
              currentCategory.setStatus(ExpenseCategoryStatus.ACTIVE);
              return expenseCategoryRepository.save(currentCategory);
            })
        .collect(Collectors.toList());
  }

  @Transactional
  @PreAuthorize("hasRootPermission(#businessId, 'MANAGE_CATEGORIES')")
  public List<ExpenseCategory> disableQboExpenseCategories(TypedId<BusinessId> businessId) {
    List<ExpenseCategory> disabledCategories =
        expenseCategoryRepository.findByBusinessIdAndStatusAndIsDefaultCategory(
            businessId, ExpenseCategoryStatus.ACTIVE, Boolean.FALSE);
    return disabledCategories.stream()
        .map(
            category -> {
              ExpenseCategory currentCategory = expenseCategoryRepository.getById(category.getId());
              currentCategory.setStatus(ExpenseCategoryStatus.DISABLED);
              return expenseCategoryRepository.save(currentCategory);
            })
        .collect(Collectors.toList());
  }
}
