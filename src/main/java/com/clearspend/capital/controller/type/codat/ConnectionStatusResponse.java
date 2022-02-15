package com.clearspend.capital.controller.type.codat;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ConnectionStatusResponse {
  @JsonProperty("results")
  @NonNull
  private List<ConnectionStatus> results;
}
