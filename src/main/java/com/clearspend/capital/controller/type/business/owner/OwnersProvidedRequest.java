package com.clearspend.capital.controller.type.business.owner;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@AllArgsConstructor
public class OwnersProvidedRequest {

  @JsonProperty("noOtherOwnersToProvide")
  @Schema(title = "No other owners to provide")
  private Boolean noOtherOwnersToProvide;

  @JsonProperty("noExecutiveToProvide")
  @Schema(title = "No executive to provide")
  private Boolean noExecutiveToProvide;
}
