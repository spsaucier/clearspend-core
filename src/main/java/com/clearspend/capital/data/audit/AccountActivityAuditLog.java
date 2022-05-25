package com.clearspend.capital.data.audit;

import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AccountActivityAuditLog {
  private String businessId;
  private String accountActivityId;
  private OffsetDateTime activityDate;
  private List<AccountActivityNotesChangeDetail> notesList;
  private List<AccountActivityReceiptChangeDetail> receiptList;
}
