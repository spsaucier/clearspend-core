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
  @Schema(
      title = "No other owners to provide",
      description =
          "If this will be set to true, will send to stripe info that we have no owner to provide.",
      defaultValue = "false")
  private Boolean noOtherOwnersToProvide;

  @JsonProperty("noExecutiveToProvide")
  @Schema(
      title = "No executive to provide",
      description =
          "If this will be set to true, will send to stripe info that we have no executive to provide.",
      defaultValue = "false")
  private Boolean noExecutiveToProvide;
}
