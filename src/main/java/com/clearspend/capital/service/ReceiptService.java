package com.clearspend.capital.service;

import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.ReceiptId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.Receipt;
import com.clearspend.capital.data.model.embedded.ReceiptDetails;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.data.repository.ReceiptRepository;
import com.clearspend.capital.service.type.CurrentUser;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PostFilter;
import org.springframework.security.access.prepost.PreAuthorize;
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
  @PreAuthorize("isSelfOwned(#receipt) or hasAllocationPermission(#receipt.allocationId, 'READ')")
  public byte[] getReceiptImage(Receipt receipt) {
    return receiptImageService.getReceiptImage(receipt.getPath());
  }

  // FIXME: PRM - Permissions?
  public byte[] getReceiptImage(TypedId<ReceiptId> receiptId) {
    Optional<Receipt> receipt = receiptRepository.findById(receiptId);
    if (receipt.isPresent()) {
      return receiptImageService.getReceiptImage(receipt.get().getPath());
    }
    return null;
  }

  @PostAuthorize(
      "isSelfOwned(returnObject) or hasAllocationPermission(returnObject.allocationId, 'READ')")
  public Receipt getReceipt(TypedId<ReceiptId> receiptId) {
    return receiptRepository
        .findReceiptByBusinessIdAndId(CurrentUser.getBusinessId(), receiptId)
        .orElseThrow(
            () ->
                new RecordNotFoundException(Table.RECEIPT, CurrentUser.getBusinessId(), receiptId));
  }

  @PostFilter("isSelfOwned(filterObject)")
  public List<Receipt> getReceiptsForCurrentUser() {
    return receiptRepository.findReceiptByBusinessIdAndUserIdAndLinked(
        CurrentUser.getBusinessId(), CurrentUser.getUserId(), false);
  }

  private String getReceiptPath(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, TypedId<ReceiptId> receiptId) {
    return String.format("/receipts/bid_%s/uid_%s/rid_%s", businessId, userId, receiptId);
  }

  @Transactional
  @PreAuthorize(
      "(isSelfOwned(#accountActivity) and isSelfOwned(#receipt)) or "
          + "(hasAllocationPermission(#accountActivity.allocationId, 'LINK_RECEIPTS') and hasAllocationPermission(#receipt.allocationId, 'READ'))")
  public void linkReceipt(Receipt receipt, AccountActivity accountActivity) {

    // if this receipt is already linked to an existing adjustment, unlink it
    if (receipt.isLinked()) {
      AccountActivity previousAccountActivity =
          accountActivityService.findByReceiptId(CurrentUser.getBusinessId(), receipt.getId());
      receipt.setAllocationId(null);
      receipt.setAccountId(null);
      receipt.setLinked(true);
      previousAccountActivity.getReceipt().getReceiptIds().remove(receipt.getId());
      accountActivityRepository.save(previousAccountActivity);
    }

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
  @PreAuthorize(
      "(isSelfOwned(#accountActivity) and isSelfOwned(#receipt)) or "
          + "(hasAllocationPermission(#accountActivity.allocationId, 'LINK_RECEIPTS') and hasAllocationPermission(#receipt.allocationId, 'READ'))")
  public void unlinkReceipt(Receipt receipt, AccountActivity accountActivity) {

    if (!receipt.isLinked()) {
      throw new InvalidRequestException("Receipt not linked");
    }

    accountActivity.getReceipt().getReceiptIds().remove(receipt.getId());
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
  @PreAuthorize("isSelfOwned(#receipt)")
  public void deleteReceipt(Receipt receipt) {
    if (receipt.isLinked()) {
      AccountActivity accountActivity =
          accountActivityService.findByReceiptId(CurrentUser.getBusinessId(), receipt.getId());
      accountActivity.getReceipt().getReceiptIds().remove(receipt.getId());
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
