package com.clearspend.capital.client.plaid;

import com.plaid.client.model.Error.ErrorTypeEnum;
import java.io.IOException;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@SuppressWarnings("unused")
@RequiredArgsConstructor
@Getter
public class PlaidClientException extends IOException {

  private final String responseStr;
  @NonNull private final PlaidError plaidError;
  private final boolean canReInitialize;

  public PlaidClientException(
      @NonNull PlaidError plaidError, String errorBody, String responseStr) {
    super(errorBody);
    this.plaidError = plaidError;
    this.responseStr = responseStr;
    this.canReInitialize = plaidError.getErrorCode().equals(PlaidErrorCode.ITEM_LOGIN_REQUIRED);
    // should also deal with PENDING_EXPIRATION webhook https://plaid.com/docs/link/update-mode/
    // when we take Plaid callbacks
  }

  // convenience methods follow to fetch info from the plaidError

  public PlaidErrorCode getErrorCode() {
    return plaidError.getErrorCode();
  }

  public ErrorTypeEnum getErrorTypeEnum() {
    return plaidError.getErrorType();
  }

  public String getErrorMessage() {
    return plaidError.getErrorMessage();
  }

  public String getDisplayMessage() {
    return plaidError.getDisplayMessage();
  }

  public String getRequestId() {
    return plaidError.getRequestId();
  }

  public PlaidError[] getCauses() {
    return plaidError.getCauses();
  }

  public int getHttpStatus() {
    return plaidError.getHttpStatus();
  }

  public String getDocumentationUrl() {
    return plaidError.getDocumentationUrl();
  }

  public String getSuggestedAction() {
    return plaidError.getSuggestedAction();
  }
}
