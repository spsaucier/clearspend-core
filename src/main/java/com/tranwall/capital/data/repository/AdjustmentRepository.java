package com.tranwall.capital.data.repository;

import com.tranwall.capital.common.typedid.data.AdjustmentId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.Adjustment;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdjustmentRepository extends JpaRepository<Adjustment, TypedId<AdjustmentId>> {}
