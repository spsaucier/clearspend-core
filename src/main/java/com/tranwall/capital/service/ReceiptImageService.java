package com.tranwall.capital.service;

import com.tranwall.capital.client.gcs.GoogleCloudStorageClient;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.ReceiptId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptImageService {

  private final GoogleCloudStorageClient googleCloudStorageClient;

  // creates new receipt record and uploads receipt image to GCS (Google Cloud Storage)
  public String storeReceiptImage(
      TypedId<BusinessId> businessId,
      TypedId<UserId> userId,
      TypedId<ReceiptId> receiptId,
      byte[] receiptFile)
      throws IOException {

    String receiptPath = getReceiptPath(businessId, userId, receiptId);
    googleCloudStorageClient.writeFile(receiptPath, receiptFile);

    return receiptPath;
  }

  // returns data held in GCS (Google Cloud Storage)
  public byte[] getReceiptImage(String receiptPath) {
    return googleCloudStorageClient.readFile(receiptPath);
  }

  private String getReceiptPath(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, TypedId<ReceiptId> receiptId) {
    return String.format("/receipts/bid_%s/uid_%s/rid_%s", businessId, userId, receiptId);
  }
}
