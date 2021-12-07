package com.tranwall.capital.service;

import com.tranwall.capital.common.error.InvalidRequestException;
import com.tranwall.capital.common.error.RecordNotFoundException;
import com.tranwall.capital.common.error.RecordNotFoundException.Table;
import com.tranwall.capital.common.typedid.data.AccountActivityId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.ReceiptId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.data.model.AccountActivity;
import com.tranwall.capital.data.model.Receipt;
import com.tranwall.capital.data.model.embedded.ReceiptDetails;
import com.tranwall.capital.data.repository.AccountActivityRepository;
import com.tranwall.capital.data.repository.ReceiptRepository;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReceiptService {

  private final ReceiptRepository receiptRepository;
  private final AccountActivityRepository accountActivityRepository;

  private final AccountActivityService accountActivityService;
  private final ReceiptImageService receiptImageService;

  // creates new receipt record and uploads receipt image to GCS (Google Cloud Storage)
  public Receipt storeReceiptImage(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, byte[] receiptFile)
      throws IOException {
    Receipt receipt = new Receipt(businessId, userId);

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

  @Transactional
  public void linkReceipt(
      TypedId<BusinessId> businessId,
      TypedId<UserId> userId,
      TypedId<ReceiptId> receiptId,
      TypedId<AccountActivityId> accountActivityId) {
    Receipt receipt = getReceipt(businessId, userId, receiptId);

    // if this receipt is already linked to an existing adjustment, unlink it
    if (receipt.getAdjustmentId() != null) {
      AccountActivity previousAccountActivity =
          accountActivityRepository
              .findByBusinessIdAndUserIdAndAdjustmentId(
                  businessId, userId, receipt.getAdjustmentId())
              .orElseThrow(
                  () ->
                      new RecordNotFoundException(
                          Table.ACCOUNT_ACTIVITY, businessId, userId, receipt.getAdjustmentId()));
      previousAccountActivity.setReceipt(null);
      accountActivityRepository.save(previousAccountActivity);
    }

    AccountActivity accountActivity =
        accountActivityService.getUserAccountActivity(businessId, userId, accountActivityId);
    accountActivity.setReceipt(new ReceiptDetails(receipt.getId()));
    receipt.setAccountId(accountActivity.getAccountId());
    receipt.setAdjustmentId(accountActivity.getAdjustmentId());

    receiptRepository.save(receipt);
    accountActivityRepository.save(accountActivity);
  }

  @Transactional
  public void unlinkReceipt(
      TypedId<BusinessId> businessId,
      TypedId<UserId> userId,
      TypedId<ReceiptId> receiptId,
      TypedId<AccountActivityId> accountActivityId) {
    Receipt receipt = getReceipt(businessId, userId, receiptId);

    if (receipt.getAdjustmentId() == null) {
      throw new InvalidRequestException("Receipt not linked");
    }

    AccountActivity accountActivity =
        accountActivityService.getUserAccountActivity(businessId, userId, accountActivityId);
    accountActivity.setReceipt(null);
    accountActivityRepository.save(accountActivity);

    receipt.setAccountId(null);
    receipt.setAdjustmentId(null);
    receiptRepository.save(receipt);
  }
}
