package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.ExpenseCategoryId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.ExpenseCategory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseCategoryRepository
    extends JpaRepository<ExpenseCategory, TypedId<ExpenseCategoryId>> {
  List<ExpenseCategory> findAll();
}
