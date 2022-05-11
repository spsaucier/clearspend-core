package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.CodatCategoryId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.CodatCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodatCategoryRepository
    extends JpaRepository<CodatCategory, TypedId<CodatCategoryId>> {}
