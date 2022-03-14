package com.clearspend.capital.service;

import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.AccountActivityId;
import com.clearspend.capital.common.typedid.data.ReceiptId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.Receipt;
import com.clearspend.capital.data.model.embedded.ReceiptDetails;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.data.repository.ReceiptRepository;
import java.io.IOException;
import java.util.List;
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
      TypedId<BusinessId> businessId,
      TypedId<UserId> userId,
      byte[] receiptFile,
      String contentType)
      throws IOException {
    Receipt receipt = new Receipt(businessId, userId);

    String receiptPath = getReceiptPath(businessId, userId, receipt.getId());
    receiptImageService.storeReceiptImage(
        businessId, userId, receipt.getId(), receiptFile, contentType);
    receipt.setPath(receiptPath);
    receipt.setContentType(contentType);

    return receiptRepository.save(receipt);
  }

  // returns data held in GCS (Google Cloud Storage)
  public byte[] getReceiptImage(TypedId<BusinessId> businessId, TypedId<ReceiptId> receiptId) {
    return receiptImageService.getReceiptImage(getReceipt(businessId, receiptId).getPath());
  }

  public Receipt getReceipt(TypedId<BusinessId> businessId, TypedId<ReceiptId> receiptId) {
    return receiptRepository
        .findReceiptByBusinessIdAndId(businessId, receiptId)
        .orElseThrow(() -> new RecordNotFoundException(Table.RECEIPT, businessId, receiptId));
  }

  public List<Receipt> getReceipts(TypedId<BusinessId> businessId, TypedId<UserId> userId) {
    return receiptRepository.findReceiptByBusinessIdAndUserIdAndLinked(businessId, userId, false);
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
    Receipt receipt = getReceipt(businessId, receiptId);

    // if this receipt is already linked to an existing adjustment, unlink it
    if (receipt.isLinked()) {
      AccountActivity previousAccountActivity =
          accountActivityService.findByReceiptId(businessId, receipt.getId());
      receipt.setAllocationId(null);
      receipt.setAccountId(null);
      receipt.setLinked(true);
      previousAccountActivity.getReceipt().getReceiptIds().remove(receiptId);
      accountActivityRepository.save(previousAccountActivity);
    }
    AccountActivity accountActivity =
        accountActivityService.retrieveAccountActivity(businessId, accountActivityId);
    ReceiptDetails receiptDetails =
        accountActivity.getReceipt() != null ? accountActivity.getReceipt() : new ReceiptDetails();
    receiptDetails.getReceiptIds().add(receipt.getId());
    accountActivity.setReceipt(receiptDetails);
    receipt.setAllocationId(accountActivity.getAllocationId());
    receipt.setAccountId(accountActivity.getAccountId());
    receipt.setLinked(true);

    receipt = receiptRepository.save(receipt);
    accountActivity = accountActivityRepository.save(accountActivity);
    log.debug(
        "Linked receipt {} to accountActivity {} ({})",
        receipt.getId(),
        accountActivity.getId(),
        accountActivity.getReceipt());
  }

  @Transactional
  public void unlinkReceipt(
      TypedId<BusinessId> businessId,
      TypedId<UserId> userId,
      TypedId<ReceiptId> receiptId,
      TypedId<AccountActivityId> accountActivityId) {
    Receipt receipt = getReceipt(businessId, receiptId);
    if (!receipt.isLinked()) {
      throw new InvalidRequestException("Receipt not linked");
    }
    AccountActivity accountActivity =
        accountActivityService.retrieveAccountActivity(businessId, accountActivityId);
    accountActivity.getReceipt().getReceiptIds().remove(receiptId);
    accountActivityRepository.save(accountActivity);

    receipt.setAllocationId(null);
    receipt.setAccountId(null);
    receipt.setLinked(false);
    receiptRepository.save(receipt);
    log.debug(
        "Unlinked receipt {} to accountActivity {} ({})",
        receipt.getId(),
        accountActivity.getId(),
        accountActivity.getReceipt());
  }

  @Transactional
  public void deleteReceipt(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, TypedId<ReceiptId> receiptId) {
    Receipt receipt = getReceipt(businessId, receiptId);

    if (receipt.isLinked()) {
      AccountActivity accountActivity =
          accountActivityService.findByReceiptId(businessId, receipt.getId());
      accountActivity.getReceipt().getReceiptIds().remove(receiptId);
      accountActivityRepository.save(accountActivity);
      log.debug(
          "unlinked (delete) receipt {} from accountActivity {} ({})",
          receipt.getId(),
          accountActivity.getId(),
          accountActivity.getReceipt());
    }

    receiptRepository.delete(receipt);
    receiptImageService.deleteReceiptImage(receipt.getPath());
    log.debug("deleted receipt {} {}", receipt.getId(), receipt.getPath());
  }
}
