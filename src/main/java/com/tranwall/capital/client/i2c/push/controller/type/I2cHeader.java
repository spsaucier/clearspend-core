package com.tranwall.capital.client.i2c.push.controller.type;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class I2cHeader {

  @ApiModelProperty(value = "Acquirer Identification Code assigned to MCP Application by  Client")
  @JsonProperty("Id")
  String id;

  @ApiModelProperty(value = "User ID assigned to MCP Application by Client")
  @JsonProperty("UserId")
  String userId;

  @ApiModelProperty(value = "Password Code assigned to MCP Application by Client")
  @JsonProperty("Password")
  String password;

  @ApiModelProperty(
      value =
          "Date and time of the device sending the request "
              + "Date format: YYYY-MM DDTHH:MM:SS") // this may be YYYY-MM-DDTHH:MM:SS
  @JsonProperty("MessageCreation")
  @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
  LocalDateTime messageCreation;
}
