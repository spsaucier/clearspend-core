package com.clearspend.capital.controller.type.activity;

import com.clearspend.capital.common.masking.annotation.Sensitive;
import com.clearspend.capital.common.typedid.data.ReceiptId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Set;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class ReceiptDetails {

  @Sensitive
  @JsonProperty("receiptId")
  private Set<TypedId<ReceiptId>> receiptIds;

  public static ReceiptDetails toReceiptDetails(
      com.clearspend.capital.data.model.embedded.ReceiptDetails in) {
    if (in == null) {
      return null;
    }

    return new ReceiptDetails(in.getReceiptIds());
  }
}
