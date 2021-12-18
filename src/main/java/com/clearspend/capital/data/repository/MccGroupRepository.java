package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.BusinessId;
import com.clearspend.capital.common.typedid.data.MccGroupId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.MccGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MccGroupRepository extends JpaRepository<MccGroup, TypedId<MccGroupId>> {

  long countByBusinessId(TypedId<BusinessId> businessId);

  List<MccGroup> findByBusinessId(TypedId<BusinessId> businessId);
}
