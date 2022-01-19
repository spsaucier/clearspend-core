package com.clearspend.capital.controller.type.activity;

import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
public class CardStatementRequest {

  @JsonProperty("cardId")
  @NonNull
  private TypedId<CardId> cardId;

  @JsonProperty("startDate")
  @NonNull
  private OffsetDateTime startDate;

  @JsonProperty("endDate")
  @NonNull
  private OffsetDateTime endDate;
}
