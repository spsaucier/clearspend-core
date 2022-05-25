package com.clearspend.capital.data.audit;

import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountActivityNotesChangeDetail {
  private String userId;
  private String notesValue;
  private OffsetDateTime changeTime;
  private Long bigTableTimestamp;
}
