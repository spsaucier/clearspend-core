package com.tranwall.capital.controller.type.receipt;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.typedid.data.ReceiptId;
import com.tranwall.capital.common.typedid.data.TypedId;
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
