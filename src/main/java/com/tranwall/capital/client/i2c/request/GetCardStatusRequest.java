package com.tranwall.capital.client.i2c.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.client.i2c.Acquirer;
import com.tranwall.capital.client.i2c.Card;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class GetCardStatusRequest {

    @JsonProperty("acquirer")
    private Acquirer acquirer;

    @JsonProperty("card")
    private Card card;
}
