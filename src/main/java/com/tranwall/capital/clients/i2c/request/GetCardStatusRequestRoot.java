package com.tranwall.capital.clients.i2c.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GetCardStatusRequestRoot {

    @JsonProperty("getCardStatus")
    private GetCardStatusRequest request;
}
