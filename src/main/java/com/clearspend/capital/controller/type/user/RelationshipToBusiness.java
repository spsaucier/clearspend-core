package com.clearspend.capital.controller.type.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class RelationshipToBusiness {

  @JsonProperty("owner")
  private final Boolean owner;

  @JsonProperty("executive")
  private final Boolean executive;

  @JsonProperty("representative")
  private final Boolean representative;

  @JsonProperty("director")
  private final Boolean director;
}
