package com.clearspend.capital.client.mx.types;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

@RequiredArgsConstructor
@Getter
@ToString
public class EnhanceTransactionResponse implements Serializable {

  @JsonProperty("transactions")
  private final List<TransactionRecordResponse> transactions;
}
