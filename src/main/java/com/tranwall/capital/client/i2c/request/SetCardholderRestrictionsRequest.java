package com.tranwall.capital.client.i2c.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.client.i2c.Acquirer;
import com.tranwall.capital.client.i2c.Card;
import java.util.ArrayList;
import java.util.List;
import javax.validation.Valid;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SetCardholderRestrictionsRequest {

  @JsonProperty("acquirer")
  private Acquirer acquirer;

  @JsonProperty("card")
  private Card card;

  @Valid
  @Builder.Default
  @JsonProperty("restrictions")
  private List<Restriction> restrictions = new ArrayList<>();
}
