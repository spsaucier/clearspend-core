package com.tranwall.capital.service;

import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.error.RecordNotFoundException;
import com.tranwall.capital.common.error.RecordNotFoundException.Table;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.ReceiptId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.data.model.Receipt;
import com.tranwall.capital.data.repository.ReceiptRepository;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptService {

  private final ReceiptRepository receiptRepository;

  private final ReceiptImageService receiptImageService;

  // creates new receipt record and uploads receipt image to GCS (Google Cloud Storage)
  public Receipt storeReceiptImage(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, Amount amount, byte[] receiptFile)
      throws IOException {
    Receipt receipt = new Receipt(businessId, userId, amount);

    String receiptPath = getReceiptPath(businessId, userId, receipt.getId());
    receiptImageService.storeReceiptImage(businessId, userId, receipt.getId(), receiptFile);
    receipt.setPath(receiptPath);

    return receiptRepository.save(receipt);
  }

  // returns data held in GCS (Google Cloud Storage)
  public byte[] getReceiptImage(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, TypedId<ReceiptId> receiptId) {
    return receiptImageService.getReceiptImage(getReceipt(businessId, userId, receiptId).getPath());
  }

  public Receipt getReceipt(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, TypedId<ReceiptId> receiptId) {
    return receiptRepository
        .findReceiptByBusinessIdAndUserIdAndId(businessId, userId, receiptId)
        .orElseThrow(
            () -> new RecordNotFoundException(Table.RECEIPT, businessId, userId, receiptId));
  }

  private String getReceiptPath(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, TypedId<ReceiptId> receiptId) {
    return String.format("/receipts/bid_%s/uid_%s/rid_%s", businessId, userId, receiptId);
  }
}
