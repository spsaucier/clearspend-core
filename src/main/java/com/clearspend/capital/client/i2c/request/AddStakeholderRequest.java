package com.clearspend.capital.client.i2c.request;

import com.clearspend.capital.client.i2c.Acquirer;
import com.clearspend.capital.client.i2c.StakeholderInfo;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class AddStakeholderRequest {

  @JsonProperty("acquirer")
  private Acquirer acquirer;

  @JsonProperty("stakeholderInfo")
  private StakeholderInfo stakeholderInfo;
}
