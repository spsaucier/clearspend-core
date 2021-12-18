package com.clearspend.capital.client.i2c;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StakeholderInfo {

  @JsonProperty("programId")
  private String programId;

  @JsonProperty("cardBin")
  private String cardBin;

  @JsonProperty("stakeholderName")
  private String stakeholderName;

  @JsonProperty("parentStakeholderId")
  private String parentStakeholderId;
}
