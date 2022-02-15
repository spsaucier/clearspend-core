package com.clearspend.capital.client.plaid;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.plaid.client.model.Error.ErrorTypeEnum;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@Getter
@ToString
@RequiredArgsConstructor
@SuppressWarnings("SameNameButDifferent")
public class PlaidError {

  @JsonProperty("error_code")
  private final PlaidErrorCode errorCode;

  @JsonProperty("error_type")
  private final ErrorTypeEnum errorType;

  @JsonProperty("error_message")
  private final String errorMessage;

  @JsonProperty("display_message")
  private final String displayMessage;

  @JsonProperty("request_id")
  private final String requestId;

  @JsonProperty("causes")
  private final PlaidError[] causes;

  @JsonProperty("status")
  private final int httpStatus;

  @JsonProperty("documentation_url")
  private final String documentationUrl;

  @JsonProperty("suggested_action")
  private final String suggestedAction;
}
