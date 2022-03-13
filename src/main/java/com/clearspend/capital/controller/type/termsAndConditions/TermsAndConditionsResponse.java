package com.clearspend.capital.controller.type.termsAndConditions;

import com.clearspend.capital.data.model.User;
import com.clearspend.capital.service.TermsAndConditionsService;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TermsAndConditionsResponse {

  @JsonProperty("user")
  private User user;

  @JsonProperty("isAcceptedTermsAndConditions")
  private boolean isAcceptedTermsAndConditions;

  @JsonProperty("documentTimestamp")
  private LocalDateTime documentTimestamp;

  public static TermsAndConditionsResponse of(
      TermsAndConditionsService.TermsAndConditionsRecord termsAndConditionsRecord) {

    return new TermsAndConditionsResponse(
        termsAndConditionsRecord.user(),
        termsAndConditionsRecord.isAcceptedTermsAndConditions(),
        termsAndConditionsRecord.documentTimestamp());
  }
}
