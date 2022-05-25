package com.clearspend.capital.data.audit;

import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountActivityReceiptChangeDetail {
  private String userId;
  private String receiptListValue;
  private OffsetDateTime changeTime;
  private long bigTableTimestamp;
}
