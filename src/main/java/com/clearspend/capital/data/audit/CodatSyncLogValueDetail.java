package com.clearspend.capital.data.audit;

import java.io.Serializable;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CodatSyncLogValueDetail implements Serializable {

  private String syncType;

  private String syncDetail;

  private String accountActivityId;

  private OffsetDateTime CodatSyncDate;

  // internal value for reference only
  private long bigTableTimestamp;
}
