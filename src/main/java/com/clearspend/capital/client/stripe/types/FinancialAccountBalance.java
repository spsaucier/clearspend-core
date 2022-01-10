package com.clearspend.capital.client.stripe.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Data;

@Data
public class FinancialAccountBalance {

  @JsonProperty("cash")
  private Map<String, Long> cash;

  @JsonProperty("inbound_pending")
  private Map<String, Long> inboundPending;

  @JsonProperty("outbound_pending")
  private Map<String, Long> outboundPending;
}
