package com.tranwall.capital.controller.type.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.common.masking.annotation.Sensitive;
import com.tranwall.capital.common.typedid.data.ReceiptId;
import com.tranwall.capital.common.typedid.data.TypedId;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ReceiptDetails {

  @Sensitive
  @JsonProperty("receiptId")
  private TypedId<ReceiptId> receiptId;

  public static ReceiptDetails toReceiptDetails(
      com.tranwall.capital.data.model.embedded.ReceiptDetails in) {
    if (in == null) {
      return null;
    }

    return new ReceiptDetails(in.getReceiptId());
  }
}
