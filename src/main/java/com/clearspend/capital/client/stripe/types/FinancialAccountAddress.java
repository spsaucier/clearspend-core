package com.clearspend.capital.client.stripe.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data
public class FinancialAccountAddress {

  @JsonProperty("type")
  private String type;

  @JsonProperty("supported_networks")
  private List<String> supportedNetworks;

  @JsonProperty("aba")
  private FinancialAccountAbaAddress abaAddress;
}
