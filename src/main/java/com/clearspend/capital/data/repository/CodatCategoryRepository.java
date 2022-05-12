package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.CodatCategoryId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.CodatCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodatCategoryRepository
    extends JpaRepository<CodatCategory, TypedId<CodatCategoryId>> {

  Optional<CodatCategory> findByCodatId(String codatId);

  List<CodatCategory> findByBusinessId(TypedId<BusinessId> businessId);
}
