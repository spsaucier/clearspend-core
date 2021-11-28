package com.tranwall.capital.controller.nonprod.type.testdata;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.controller.type.business.Business;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GetBusinessesResponse {

  @JsonProperty("businesses")
  List<Business> businesses;
}
