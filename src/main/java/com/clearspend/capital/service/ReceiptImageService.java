package com.clearspend.capital.service;

import com.clearspend.capital.client.gcs.GoogleCloudStorageClient;
import com.clearspend.capital.common.typedid.data.ReceiptId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
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
  String storeReceiptImage(
      TypedId<BusinessId> businessId,
      TypedId<UserId> userId,
      TypedId<ReceiptId> receiptId,
      byte[] receiptFile,
      String contentType)
      throws IOException {

    String receiptPath = getReceiptPath(businessId, userId, receiptId);
    googleCloudStorageClient.writeFile(receiptPath, receiptFile);

    return receiptPath;
  }

  // returns data held in GCS (Google Cloud Storage)
  byte[] getReceiptImage(String receiptPath) {
    return googleCloudStorageClient.readFile(receiptPath);
  }

  boolean deleteReceiptImage(String receiptPath) {
    return googleCloudStorageClient.deleteFile(receiptPath);
  }

  String getReceiptPath(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, TypedId<ReceiptId> receiptId) {
    return String.format("/receipts/bid_%s/uid_%s/rid_%s", businessId, userId, receiptId);
  }
}
