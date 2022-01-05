package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.data.model.Business;
import com.clearspend.capital.data.model.Receipt;
import com.clearspend.capital.data.repository.ReceiptRepository;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class ReceiptServiceTest extends BaseCapitalTest {

  @Autowired private TestHelper testHelper;
  @Autowired private ReceiptImageService receiptImageService;
  @Autowired private ReceiptService receiptService;

  @Autowired private ReceiptRepository receiptRepository;

  private CreateUpdateUserRecord userRecord;
  private String fileContents;
  private Receipt receipt;
  private Receipt unlinkedReceipt;

  @SneakyThrows
  @BeforeEach
  public void setup() {
    if (userRecord == null) {
      Business business = testHelper.createBusiness().business();
      userRecord = testHelper.createUser(business);
      fileContents = "Hello world " + UUID.randomUUID();
      receipt =
          receiptService.storeReceiptImage(
              userRecord.user().getBusinessId(),
              userRecord.user().getId(),
              fileContents.getBytes());
      unlinkedReceipt = new Receipt(userRecord.user().getBusinessId(), userRecord.user().getId());
      unlinkedReceipt.setPath(
          receiptImageService.getReceiptPath(
              userRecord.user().getBusinessId(), userRecord.user().getId(), receipt.getId()));
      receiptRepository.save(unlinkedReceipt);
    }
  }

  @SneakyThrows
  @Test
  void storeReceiptImage_success() {
    Receipt receipt =
        receiptService.storeReceiptImage(
            userRecord.user().getBusinessId(), userRecord.user().getId(), fileContents.getBytes());
    assertThat(receipt).isNotNull();
  }

  @SneakyThrows
  @Test
  void getReceiptImage_success() {
    byte[] bytes =
        receiptService.getReceiptImage(userRecord.user().getBusinessId(), receipt.getId());
    assertThat(bytes).isEqualTo(fileContents.getBytes());
  }

  @Test
  void getReceipt_success() {
    Receipt foundReceipt =
        receiptService.getReceipt(userRecord.user().getBusinessId(), receipt.getId());
    assertThat(foundReceipt.getPath()).isEqualTo(receipt.getPath());
  }

  @Test
  void getReceipts_success() {
    List<Receipt> foundReceipts =
        receiptService.getReceipts(userRecord.user().getBusinessId(), userRecord.user().getId());
    assertThat(foundReceipts).hasSize(2);
    assertThat(foundReceipts).hasSameElementsAs(List.of(receipt, unlinkedReceipt));
  }

  @Test
  void deleteReceipt_success() {
    Receipt receipt = new Receipt(userRecord.user().getBusinessId(), userRecord.user().getId());
    receipt.setPath(
        receiptImageService.getReceiptPath(
            userRecord.user().getBusinessId(), userRecord.user().getId(), this.receipt.getId()));
    receiptRepository.save(receipt);

    receiptService.deleteReceipt(receipt.getBusinessId(), receipt.getUserId(), receipt.getId());
  }
}
