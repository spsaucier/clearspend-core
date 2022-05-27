package com.clearspend.capital.data.model;

import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.typedid.data.BatchSummaryId;
import com.clearspend.capital.data.model.enums.BatchSummaryStatus;
import com.clearspend.capital.data.model.enums.BatchSummaryType;
import java.time.OffsetDateTime;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
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
  @Enumerated(EnumType.STRING)
  private BatchSummaryType batchType;

  @NonNull private Integer totalExecutions;

  @NonNull private OffsetDateTime lastRunDate;

  @NonNull private Integer lastRecordsProcessed;

  @NonNull private Integer totalRecordsProcessed;

  @NonNull private OffsetDateTime firstRecordDate;

  @NonNull private OffsetDateTime lastRecordDate;

  @NonNull
  @Enumerated(EnumType.STRING)
  private BatchSummaryStatus status;
}
