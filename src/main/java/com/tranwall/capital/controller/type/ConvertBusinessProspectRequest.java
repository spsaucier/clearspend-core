package com.tranwall.capital.controller.type;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.data.model.enums.BusinessType;
import java.time.LocalDate;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ConvertBusinessProspectRequest {

  @JsonProperty("legalName")
  @NonNull
  private String legalName;

  @JsonProperty("businessType")
  @NonNull
  private BusinessType businessType;

  @JsonProperty("formationDate")
  @NonNull
  private LocalDate formationDate;

  @JsonProperty("address")
  @NonNull
  private Address address;
}
