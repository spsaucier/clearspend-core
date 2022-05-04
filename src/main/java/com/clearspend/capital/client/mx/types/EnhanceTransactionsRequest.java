package com.clearspend.capital.client.mx.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
@ToString
public class EnhanceTransactionsRequest {
  @JsonProperty("transactions")
  private final List<TransactionRecordRequest> transactions;
}
