package com.clearspend.capital.data.model;

import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.typedid.data.BatchSummaryId;
import java.time.OffsetDateTime;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.DynamicUpdate;

@Entity
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@RequiredArgsConstructor
@DynamicUpdate
@Slf4j
@Table(name = "batch_summary")
public class BatchSummary extends TypedMutable<BatchSummaryId> {

  @NonNull
  @Column(name = "batch_type")
  private String batchType;

  @NonNull
  @Column(name = "total_executions")
  private Integer totalExecutions;

  @NonNull
  @Column(name = "last_run_date")
  private OffsetDateTime lastRunDate;

  @NonNull
  @Column(name = "last_records_processed")
  private Integer lastRecordsProcessed;

  @NonNull
  @Column(name = "total_records_processed")
  private Integer totalRecordsProcessed;

  @NonNull
  @Column(name = "first_record_date")
  private OffsetDateTime firstRecordDate;

  @NonNull
  @Column(name = "last_record_date")
  private OffsetDateTime lastRecordDate;

  @NonNull
  @Column(name = "status")
  private String status;
}
