package com.tranwall.capital.controller.type.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.AccountId;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.enums.AccountActivityType;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class AccountActivityRequest {

  @JsonProperty("allocationId")
  private TypedId<AllocationId> allocationId;

  @JsonProperty("accountId")
  private TypedId<AccountId> accountId;

  @JsonProperty("type")
  private AccountActivityType type;

  @JsonProperty("from")
  private OffsetDateTime from;

  @JsonProperty("to")
  private OffsetDateTime to;

  @JsonProperty("pageRequest")
  PageRequestDTO pageRequestDTO;
}
