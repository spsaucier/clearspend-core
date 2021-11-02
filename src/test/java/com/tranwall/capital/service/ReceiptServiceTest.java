package com.tranwall.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.Receipt;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.service.UserService.CreateUserRecord;
import java.math.BigDecimal;
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

  private CreateUserRecord userRecord;
  private String fileContents;
  private Receipt receipt;

  @SneakyThrows
  @BeforeEach
  public void setup() {
    if (userRecord == null) {
      Business business = testHelper.createBusiness(testHelper.retrievePooledProgram()).business();
      userRecord = testHelper.createUser(business);
      fileContents = "Hello world " + UUID.randomUUID();
      receipt =
          receiptService.storeReceiptImage(
              userRecord.user().getBusinessId(),
              userRecord.user().getId(),
              Amount.of(Currency.USD, BigDecimal.TEN),
              fileContents.getBytes());
    }
  }

  @SneakyThrows
  @Test
  void storeReceiptImage_success() {
    Receipt receipt =
        receiptService.storeReceiptImage(
            userRecord.user().getBusinessId(),
            userRecord.user().getId(),
            Amount.of(Currency.USD, BigDecimal.TEN),
            fileContents.getBytes());
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
