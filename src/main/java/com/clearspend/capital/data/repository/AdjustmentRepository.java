package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.AdjustmentId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.Adjustment;
import com.clearspend.capital.data.model.enums.AdjustmentType;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdjustmentRepository
    extends JpaRepository<Adjustment, TypedId<AdjustmentId>>, AdjustmentRepositoryCustom {

  List<Adjustment> findByBusinessIdAndTypeInAndEffectiveDateAfter(
      TypedId<BusinessId> businessId, List<AdjustmentType> types, OffsetDateTime after);
}
