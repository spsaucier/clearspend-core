package com.tranwall.capital.controller.type.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.controller.type.common.PageRequest;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class SearchUserRequest {

  @JsonProperty("pageRequest")
  PageRequest pageRequest;
}
