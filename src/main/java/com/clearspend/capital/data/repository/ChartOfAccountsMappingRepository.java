package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.ChartOfAccountsMappingId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.ChartOfAccountsMapping;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChartOfAccountsMappingRepository
    extends JpaRepository<ChartOfAccountsMapping, TypedId<ChartOfAccountsMappingId>> {

  Optional<ChartOfAccountsMapping> findByBusinessIdAndAccountRefId(
      TypedId<BusinessId> businessId, String accountRefId);

  List<ChartOfAccountsMapping> findAllByBusinessId(TypedId<BusinessId> businessId);
}
