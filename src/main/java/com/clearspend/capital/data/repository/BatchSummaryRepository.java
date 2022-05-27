package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.BatchSummaryId;
import com.clearspend.capital.data.model.BatchSummary;
import com.clearspend.capital.data.model.enums.BatchSummaryType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchSummaryRepository extends JpaRepository<BatchSummary, BatchSummaryId> {
  Optional<BatchSummary> findByBatchType(BatchSummaryType batchType);
}
