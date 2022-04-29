package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.ExpenseCategoryId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.ExpenseCategory;
import com.clearspend.capital.data.model.enums.ExpenseCategoryStatus;
import com.clearspend.capital.service.type.CurrentUser;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseCategoryRepository
    extends JpaRepository<ExpenseCategory, TypedId<ExpenseCategoryId>> {

  List<ExpenseCategory> findByBusinessId(TypedId<BusinessId> businessId);

  List<ExpenseCategory> findByBusinessIdAndStatus(
      TypedId<BusinessId> businessId, ExpenseCategoryStatus status);

  List<ExpenseCategory> findAllByBusinessIdAndIdIn(
      final TypedId<BusinessId> businessId,
      final List<TypedId<ExpenseCategoryId>> expenseCategoryId);

  default Optional<ExpenseCategory> findFirstCategoryByName(String categoryName) {
    return findFirstByBusinessIdAndCategoryName(CurrentUser.getBusinessId(), categoryName);
  }

  default Optional<ExpenseCategory> findFirstCategoryByNameAndStatus(
      String categoryName, ExpenseCategoryStatus status) {
    return findFirstByBusinessIdAndCategoryNameAndStatus(
        CurrentUser.getBusinessId(), categoryName, status);
  }

  Optional<ExpenseCategory> findFirstByBusinessIdAndCategoryNameAndStatus(
      TypedId<BusinessId> businessId, String categoryName, ExpenseCategoryStatus status);

  Optional<ExpenseCategory> findFirstByBusinessIdAndCategoryName(
      TypedId<BusinessId> businessIdTypedId, String categoryName);

  List<ExpenseCategory> findByBusinessIdAndStatusAndIsDefaultCategory(
      TypedId<BusinessId> businessId, ExpenseCategoryStatus status, Boolean isDefaultCategory);
}
