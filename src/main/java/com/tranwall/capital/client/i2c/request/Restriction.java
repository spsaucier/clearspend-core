package com.tranwall.capital.client.i2c.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.tranwall.capital.client.i2c.enums.AlertRecipient;
import com.tranwall.capital.client.i2c.enums.SpendingControl;
import com.tranwall.capital.client.i2c.util.I2CEnumSerializer;
import java.util.List;
import javax.validation.Valid;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Restriction {

  @JsonProperty("restrictionType")
  @JsonSerialize(using = I2CEnumSerializer.class)
  private SpendingControl spendingControl;

  @JsonProperty("alertRecipients")
  @JsonSerialize(using = I2CEnumSerializer.class)
  private AlertRecipient alertRecipient;

  @Valid
  @JsonProperty("restrictionParams")
  private List<RestrictionParameter> restrictionParameters;
}
