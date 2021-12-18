package com.clearspend.capital.client.i2c.request;

import com.clearspend.capital.client.i2c.enums.Frequency;
import com.clearspend.capital.client.i2c.enums.ParameterValueType;
import com.clearspend.capital.client.i2c.enums.SpendingControl;
import com.clearspend.capital.client.i2c.util.I2CBooleanSerializer;
import com.clearspend.capital.client.i2c.util.I2CEnumSerializer;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(Include.NON_NULL)
public class RestrictionParameter {

  @JsonProperty("restrictionId")
  @JsonSerialize(using = I2CEnumSerializer.class)
  private SpendingControl spendingControl;

  @JsonProperty("paramValueType")
  @JsonSerialize(using = I2CEnumSerializer.class)
  private ParameterValueType parameterValueType;

  @JsonProperty("cardParamValue")
  private String cardParameterValue;

  @JsonProperty("cardParamMaxValue")
  private String cardParamMaxValue;

  @JsonProperty("cardParamMinValue")
  private String cardParamMinValue;

  @JsonProperty("frequency")
  @JsonSerialize(using = I2CEnumSerializer.class)
  private Frequency frequency;

  @JsonProperty("sendEmailFlag")
  @JsonSerialize(using = I2CBooleanSerializer.class)
  private Boolean sendEmailFlag;

  @JsonProperty("sendSmsFlag")
  @JsonSerialize(using = I2CBooleanSerializer.class)
  private Boolean sendSmsFlag;
}
