package com.tranwall.capital.data.repository;

import com.tranwall.capital.common.typedid.data.AdjustmentId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.Adjustment;
import com.tranwall.capital.data.model.enums.AdjustmentType;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdjustmentRepository extends JpaRepository<Adjustment, TypedId<AdjustmentId>> {

  List<Adjustment> findByBusinessIdAndTypeInAndEffectiveDateAfter(
      TypedId<BusinessId> businessId, List<AdjustmentType> types, OffsetDateTime after);
}
