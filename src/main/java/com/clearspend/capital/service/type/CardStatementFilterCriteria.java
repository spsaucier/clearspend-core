package com.clearspend.capital.service.type;

import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;

@AllArgsConstructor
@Getter
public class CardStatementFilterCriteria {
  @NonNull private TypedId<CardId> cardId;
  @NonNull private OffsetDateTime from;
  @NonNull private OffsetDateTime to;
}
