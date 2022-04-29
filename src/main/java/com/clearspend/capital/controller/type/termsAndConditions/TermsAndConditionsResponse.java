package com.clearspend.capital.controller.type.termsAndConditions;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.service.TermsAndConditionsService;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TermsAndConditionsResponse {

  @JsonProperty("userId")
  private TypedId<?> userId;

  @JsonProperty("acceptedTimestampByUser")
  private LocalDateTime acceptedTimestampByUser;

  @JsonProperty("isAcceptedTermsAndConditions")
  private boolean isAcceptedTermsAndConditions;

  @JsonProperty("documentTimestamp")
  private LocalDateTime documentTimestamp;

  public static TermsAndConditionsResponse of(
      TermsAndConditionsService.TermsAndConditionsRecord termsAndConditionsRecord) {

    return new TermsAndConditionsResponse(
        termsAndConditionsRecord.userId(),
        termsAndConditionsRecord.acceptedTimestampByUser(),
        termsAndConditionsRecord.isAcceptedTermsAndConditions(),
        termsAndConditionsRecord.maxDocumentTimestamp());
  }
}
