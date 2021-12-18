package com.clearspend.capital.client.i2c.push.controller.type;

import com.clearspend.capital.data.model.enums.Country;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CardAcceptor {

  @ApiModelProperty(value = "Acquirer Identifier. " + "Type: Str 44")
  @JsonProperty("AcquirerId")
  private String acquirerRef;

  @ApiModelProperty(value = "Merchant Identifier. " + "Type: Str 15")
  @JsonProperty("MerchantCode")
  private String merchantCode;

  @ApiModelProperty(
      value =
          "Merchant’s name and location can be 43 characters long: "
              + "1-25 contain the merchant’s name "
              + "26-38 contain the city name "
              + "39-40 contain state code "
              + "41-43 contain country code")
  @JsonProperty("NameAndLocation")
  private String merchantNameAndLocation;

  @ApiModelProperty(value = "Merchant's city coming from the 'Name And Location' string")
  @JsonProperty("MerchantCity")
  private String merchantLocality;

  public String getMerchantName() {
    return merchantNameAndLocation == null
        ? ""
        : merchantNameAndLocation.length() < 25
            ? merchantNameAndLocation.trim()
            : merchantNameAndLocation.substring(0, 25).trim();
  }

  @ApiModelProperty(value = "Merchant's state coming from the 'Name And Location' string")
  @JsonProperty("MerchantState")
  private String merchantRegion;

  @ApiModelProperty(value = "Merchant's zip code")
  @JsonProperty("MerchantZipCode")
  private String merchantPostalCode;

  public Country getMerchantCountry() {
    return merchantNameAndLocation == null || merchantNameAndLocation.length() < 43
        ? Country.UNSPECIFIED
        : Country.of(merchantNameAndLocation.substring(40).trim());
  }

  @ApiModelProperty(value = "Merchant Category Code")
  @JsonProperty("MCC")
  private Integer mcc;

  @ApiModelProperty(value = "Merchant Device Identifier. " + "Type: Str 8")
  @JsonProperty("DeviceId")
  private String deviceRef;

  @ApiModelProperty(
      value =
          "Device Type Identifier Refer to "
              + "Appendix Device Types to see the possible values for this field. "
              + "Type: char")
  @JsonProperty("DeviceType")
  private String deviceType; // on common

  @ApiModelProperty(
      value =
          "Date and Time of the Device from which Transaction was Received "
              + "Date Format: YYYY-MM-DDTHH:MM:SS")
  @JsonProperty("LocalDateTime")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
  // TODO(kuchlein): is this the same as the date/time on Transaction?
  private OffsetDateTime localDateTime;
}
