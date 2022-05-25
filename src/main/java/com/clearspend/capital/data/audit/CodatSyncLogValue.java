package com.clearspend.capital.data.audit;

import java.io.Serializable;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CodatSyncLogValue implements Serializable {

  private OffsetDateTime CodatSyncDate;

  private String businessId;

  private String userId;

  private List<CodatSyncLogValueDetail> details;
}
