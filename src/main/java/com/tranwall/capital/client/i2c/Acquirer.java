package com.tranwall.capital.client.i2c;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Acquirer {

  /** Acquirer Identification Code */
  @JsonProperty("id")
  private String id;

  /** Acquirer’s user ID assigned by MCP */
  @JsonProperty("userId")
  private String userId;

  /** Acquirer’s password assigned by MCP or encrypted string. */
  @JsonProperty("password")
  private String password;
}
