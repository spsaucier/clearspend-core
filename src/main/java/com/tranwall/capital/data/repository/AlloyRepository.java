package com.tranwall.capital.data.repository;

import com.tranwall.capital.common.typedid.data.AlloyId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.Alloy;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlloyRepository extends JpaRepository<Alloy, TypedId<AlloyId>> {

  List<Alloy> findAllByBusinessId(TypedId<BusinessId> businessId);

  List<Alloy> findAllByEntityToken(String entityToken);
}
