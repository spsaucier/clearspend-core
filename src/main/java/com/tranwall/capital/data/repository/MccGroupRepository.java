package com.tranwall.capital.data.repository;

import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.MccGroupId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.MccGroup;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MccGroupRepository extends JpaRepository<MccGroup, TypedId<MccGroupId>> {

  long countByBusinessId(TypedId<BusinessId> businessId);

  List<MccGroup> findByBusinessId(TypedId<BusinessId> businessId);
}
