package com.tranwall.capital.client.i2c.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.client.i2c.Acquirer;
import com.tranwall.capital.client.i2c.StakeholderInfo;
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
