package com.tranwall.capital.client.i2c.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddStakeholderResponse extends BaseI2CResponse {

  @JsonProperty("stakeholderId")
  private String stakeholderId;
}
