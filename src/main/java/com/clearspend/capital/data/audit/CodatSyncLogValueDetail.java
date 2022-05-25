package com.clearspend.capital.data.audit;

import java.io.Serializable;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CodatSyncLogValueDetail implements Serializable {

  private String syncType;

  private String syncDetail;

  private String accountActivityId;

  private OffsetDateTime CodatSyncDate;

  // internal value for reference only
  private long bigTableTimestamp;
}
