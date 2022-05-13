package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.CodatCategoryId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.CodatCategory;
import com.clearspend.capital.data.model.enums.CodatCategoryType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CodatCategoryRepository
    extends JpaRepository<CodatCategory, TypedId<CodatCategoryId>> {

  List<CodatCategory> findByBusinessId(TypedId<BusinessId> businessId);

  List<CodatCategory> findByBusinessIdAndType(
      TypedId<BusinessId> businessId, CodatCategoryType type);

  List<CodatCategory> findByBusinessIdAndCodatCategoryId(TypedId<BusinessId> businessId, String id);
}
