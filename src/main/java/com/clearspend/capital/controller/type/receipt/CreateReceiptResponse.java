package com.clearspend.capital.controller.type.receipt;

import com.clearspend.capital.common.typedid.data.ReceiptId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class CreateReceiptResponse {

  @JsonProperty("receiptId")
  @NonNull
  private TypedId<ReceiptId> receiptId;
}
