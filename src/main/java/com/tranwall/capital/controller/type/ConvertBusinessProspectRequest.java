package com.tranwall.capital.controller.type;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.data.model.enums.BusinessType;
import java.time.LocalDate;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Data
@NoArgsConstructor
@RequiredArgsConstructor
public class ConvertBusinessProspectRequest {

  @JsonProperty("legalName")
  private String legalName;

  @JsonProperty("businessType")
  private BusinessType businessType;

  @JsonProperty("formationDate")
  private LocalDate formationDate;

  @JsonProperty("address")
  private Address address;
}
