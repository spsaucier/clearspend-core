package com.tranwall.capital.client.i2c.push.controller.type;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
public class Header {

  @JsonProperty("Id")
  private String id;

  @JsonProperty("UserId")
  private String userId;

  @JsonProperty("Password")
  private String password;

  @JsonProperty("MessageCreation")
  private OffsetDateTime messageCreation;
}
