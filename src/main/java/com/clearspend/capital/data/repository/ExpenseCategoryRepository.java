package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.ExpenseCategoryId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.ExpenseCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExpenseCategoryRepository
    extends JpaRepository<ExpenseCategory, TypedId<ExpenseCategoryId>> {}
