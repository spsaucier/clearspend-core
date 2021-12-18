package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.AlloyId;
import com.clearspend.capital.common.typedid.data.BusinessId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.Alloy;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlloyRepository extends JpaRepository<Alloy, TypedId<AlloyId>> {

  List<Alloy> findAllByBusinessId(TypedId<BusinessId> businessId);

  List<Alloy> findAllByEntityToken(String entityToken);
}
