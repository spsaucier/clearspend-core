package com.clearspend.capital.controller.type.activity;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
public class GraphDataRequest {

  @JsonProperty("allocationId")
  private TypedId<AllocationId> allocationId;

  @JsonProperty("userId")
  private TypedId<UserId> userId;

  @JsonProperty("cardId")
  private TypedId<CardId> cardId;

  @JsonProperty("from")
  @NonNull
  @NotNull
  private OffsetDateTime from;

  @JsonProperty("to")
  @NonNull
  @NotNull
  private OffsetDateTime to;
}
