package com.clearspend.capital.controller.type.termsAndConditions;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TermsAndConditionsRequest {

  @JsonProperty("termsAndConditionsTimestamp")
  private LocalDateTime termsAndConditionsTimestamp;
}
