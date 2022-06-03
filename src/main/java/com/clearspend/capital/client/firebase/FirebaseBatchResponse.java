package com.clearspend.capital.client.firebase;

import com.google.firebase.messaging.BatchResponse;
import java.util.List;
import lombok.Data;

@Data
public class FirebaseBatchResponse {

  public record FirebaseResponse(String messageId, String errorCode) {}

  int successCount;
  int failureCount;
  List<FirebaseResponse> responsesList;

  public FirebaseBatchResponse of(BatchResponse batchResponse) {
    this.setSuccessCount(batchResponse.getSuccessCount());
    this.setFailureCount(batchResponse.getFailureCount());
    if (batchResponse.getResponses() != null) {
      this.setResponsesList(
          batchResponse.getResponses().stream()
              .map(
                  sendResponse ->
                      new FirebaseResponse(
                          sendResponse.getMessageId(),
                          sendResponse.getException() != null
                              ? sendResponse.getException().getMessagingErrorCode().name()
                              : null))
              .toList());
    }
    return this;
  }
}
