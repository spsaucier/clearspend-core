package com.clearspend.capital.controller.type.business;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

@Value
@Builder(access = AccessLevel.PRIVATE)
public class BusinessLimit {

  @NonNull
  @JsonProperty("issuedPhysicalCardsLimit")
  private Integer issuedPhysicalCardsLimit;

  @JsonProperty("issuedPhysicalCardsTotal")
  private int issuedPhysicalCardsTotal;

  public static BusinessLimit of(
      com.clearspend.capital.data.model.business.BusinessLimit businessLimit) {
    return BusinessLimit.builder()
        .issuedPhysicalCardsLimit(businessLimit.getIssuedPhysicalCardsLimit())
        .issuedPhysicalCardsTotal(businessLimit.getIssuedPhysicalCardsTotal())
        .build();
  }
}
