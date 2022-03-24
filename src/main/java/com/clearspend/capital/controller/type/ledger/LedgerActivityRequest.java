package com.clearspend.capital.controller.type.ledger;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.activity.FilterAmount;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LedgerActivityRequest {

  @JsonProperty("allocationId")
  private TypedId<AllocationId> allocationId;

  @JsonProperty("searchText")
  private String searchText;

  @JsonProperty("types")
  private List<AccountActivityType> types;

  @JsonProperty("from")
  private OffsetDateTime from;

  @JsonProperty("to")
  private OffsetDateTime to;

  @JsonProperty("statuses")
  private List<AccountActivityStatus> statuses;

  @JsonProperty("amount")
  private FilterAmount filterAmount;

  @NonNull
  @NotNull(message = "Page request is mandatory")
  @JsonProperty("pageRequest")
  PageRequest pageRequest;
}
