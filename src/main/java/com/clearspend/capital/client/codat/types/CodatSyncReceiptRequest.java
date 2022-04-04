package com.clearspend.capital.client.codat.types;

import com.clearspend.capital.common.typedid.data.ReceiptId;
import com.clearspend.capital.common.typedid.data.TypedId;

public record CodatSyncReceiptRequest(
    String companyRef,
    String connectionId,
    String directCostId,
    byte[] imageData,
    String contentType,
    TypedId<ReceiptId> receiptId) {}
