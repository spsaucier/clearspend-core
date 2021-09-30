package com.tranwall.capital.client.i2c;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Card {

    /**
     * Card account number. Only Digit values are allowed in this tag.
     *
     * If reference number is given, card number is optional. Otherwise, it is mandatory.
     */
    @JsonProperty("number")
    private String number;
}
