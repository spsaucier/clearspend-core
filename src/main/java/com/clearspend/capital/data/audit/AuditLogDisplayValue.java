package com.clearspend.capital.data.audit;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@ToString
public class AuditLogDisplayValue {

  private String firstName;
  private String lastName;
  private String email;
  private String eventType;
  private String changedValue;
  private String transactionId;
  private OffsetDateTime auditTime;
  private long timestamp;
  private String userId;
  private List<String> groupSyncActivityIds;
}
