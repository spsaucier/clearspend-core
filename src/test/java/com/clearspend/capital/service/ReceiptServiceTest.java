package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.data.model.Business;
import com.clearspend.capital.data.model.Receipt;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import java.util.UUID;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class ReceiptServiceTest extends BaseCapitalTest {

  @Autowired private TestHelper testHelper;
  @Autowired private ReceiptService receiptService;

  private CreateUpdateUserRecord userRecord;
  private String fileContents;
  private Receipt receipt;

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
        receiptService.getReceiptImage(
            userRecord.user().getBusinessId(), userRecord.user().getId(), receipt.getId());
    assertThat(bytes).isEqualTo(fileContents.getBytes());
  }

  @Test
  void getReceipt_success() {
    Receipt foundReceipt =
        receiptService.getReceipt(
            userRecord.user().getBusinessId(), userRecord.user().getId(), receipt.getId());
    assertThat(foundReceipt.getPath()).isEqualTo(receipt.getPath());
  }
}
