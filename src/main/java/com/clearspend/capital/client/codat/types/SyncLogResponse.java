package com.clearspend.capital.client.codat.types;

import com.clearspend.capital.data.model.TransactionSyncLog;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;
import lombok.NonNull;

public class SyncLogResponse {
  @JsonProperty("startTime")
  @NonNull
  private OffsetDateTime startTime;

  @JsonProperty("firstName")
  @NonNull
  private String firstName;

  @JsonProperty("lastName")
  @NonNull
  private String lastName;

  @JsonProperty("status")
  @NonNull
  private String status;

  @JsonProperty("transactionId")
  @NonNull
  private String transactionId;

  public SyncLogResponse(@NonNull TransactionSyncLog transactionSyncLog) {
    this.startTime = transactionSyncLog.getCreated();
    this.firstName = transactionSyncLog.getFirstName().getEncrypted();
    this.lastName = transactionSyncLog.getLastName().getEncrypted();
    this.status = transactionSyncLog.getStatus().name();
    this.transactionId = transactionSyncLog.getAccountActivityId().toString();
  }
}
