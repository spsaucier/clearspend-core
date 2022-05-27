package com.clearspend.capital.controller.type.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@Data
@NoArgsConstructor
public class BusinessStatementRequest {

  @JsonProperty("startDate")
  @NonNull
  private OffsetDateTime startDate;

  @JsonProperty("endDate")
  @NonNull
  private OffsetDateTime endDate;
}
