package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.BatchSummaryId;
import com.clearspend.capital.data.model.BatchSummary;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BatchSummaryRepository extends JpaRepository<BatchSummary, BatchSummaryId> {
  BatchSummary findByBatchType(String batchType);
}
