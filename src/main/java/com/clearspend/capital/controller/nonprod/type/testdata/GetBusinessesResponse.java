package com.clearspend.capital.controller.nonprod.type.testdata;

import com.clearspend.capital.controller.type.business.Business;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GetBusinessesResponse {

  @JsonProperty("businesses")
  List<Business> businesses;
}
