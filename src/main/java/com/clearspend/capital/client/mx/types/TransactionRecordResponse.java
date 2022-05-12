package com.clearspend.capital.client.mx.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
@ToString
public class TransactionRecordResponse implements Serializable {
  @JsonProperty("id")
  private final String id;

  @JsonProperty("description")
  private final String enhancedName;

  @JsonProperty("merchant_category_code")
  private final Integer enhancedCategoryCode;

  @JsonProperty("merchant_guid")
  private final String externalMerchantId;

  @JsonProperty("merchant_location_guid")
  private final String externalLocationId;

  @JsonProperty("category")
  private final String categoryDescription;
}
