package com.clearspend.capital.testutils.statement;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.TestHelper;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.controller.nonprod.TestDataController;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.service.NetworkMessageService;
import com.clearspend.capital.service.type.NetworkCommon;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class StatementHelper {

  private final NetworkMessageService networkMessageService;

  public void setupStatementData(
      final TestHelper.CreateBusinessRecord createBusinessRecord, final Card card) {
    // generate auth 1
    TestDataController.NetworkCommonAuthorization networkCommonAuthorization1 =
        TestDataController.generateAuthorizationNetworkCommon(
            createBusinessRecord.user(),
            card,
            createBusinessRecord.allocationRecord().account(),
            Amount.of(Currency.USD, new BigDecimal(900)));
    networkMessageService.processNetworkMessage(networkCommonAuthorization1.networkCommon());
    assertThat(networkCommonAuthorization1.networkCommon().isPostAdjustment()).isFalse();
    assertThat(networkCommonAuthorization1.networkCommon().isPostDecline()).isFalse();
    assertThat(networkCommonAuthorization1.networkCommon().isPostHold()).isTrue();

    // generate capture 1
    NetworkCommon common1 =
        TestDataController.generateCaptureNetworkCommon(
            createBusinessRecord.business(), networkCommonAuthorization1.authorization());
    networkMessageService.processNetworkMessage(common1);

    // generate auth 2
    TestDataController.NetworkCommonAuthorization networkCommonAuthorization2 =
        TestDataController.generateAuthorizationNetworkCommon(
            createBusinessRecord.user(),
            card,
            createBusinessRecord.allocationRecord().account(),
            Amount.of(Currency.USD, new BigDecimal(9)));
    networkMessageService.processNetworkMessage(networkCommonAuthorization2.networkCommon());

    assertThat(networkCommonAuthorization2.networkCommon().isPostAdjustment()).isFalse();
    assertThat(networkCommonAuthorization2.networkCommon().isPostDecline()).isFalse();
    assertThat(networkCommonAuthorization2.networkCommon().isPostHold()).isTrue();

    // generate capture 2
    NetworkCommon common2 =
        TestDataController.generateCaptureNetworkCommon(
            createBusinessRecord.business(), networkCommonAuthorization2.authorization());
    networkMessageService.processNetworkMessage(common2);

    // generate auth 3 without capture, should not be found in PDF below
    TestDataController.NetworkCommonAuthorization networkCommonAuthorization3 =
        TestDataController.generateAuthorizationNetworkCommon(
            createBusinessRecord.user(),
            card,
            createBusinessRecord.allocationRecord().account(),
            Amount.of(Currency.USD, new BigDecimal(13)));
    networkMessageService.processNetworkMessage(networkCommonAuthorization3.networkCommon());

    assertThat(networkCommonAuthorization3.networkCommon().isPostAdjustment()).isFalse();
    assertThat(networkCommonAuthorization3.networkCommon().isPostDecline()).isFalse();
    assertThat(networkCommonAuthorization3.networkCommon().isPostHold()).isTrue();
  }

  @SneakyThrows
  public void validatePdfContent(
      final byte[] content, final User businessOwnerUser, final Card card) {
    /*
      Parse PDF back into text, and try to find several pieces which we know should be there
      Expected string should look like below:

       Monthly Statement
       Total amount spent this period:VISA Statement 01/30/2022 - 02/01/2022
       $909.00
       Cardholder: Armand O'Conner
       Card number: **** 4489
       Allocation: Brekke, Franecki and Turner 32b546ee-c7a7-4823-ac3d-
       79dca14502e9 - root
       Available to spend as of 02/01/2022:
       $91.00
       Thank you for using ClearSpend. For details and upcoming payments,
       log into your ClearSpend account
       Transactions
       DATE Merchant AMOUNT
       01/31/2022 Tuscon Bakery$900.00
       01/31/2022 Tuscon Bakery$9.00
       $909.00

    */

    PdfTextExtractor pdfTextExtractor = new PdfTextExtractor(new PdfReader(content));

    String pdfParsed = pdfTextExtractor.getTextFromPage(1);
    System.out.println("PDF"); // TODO delete this
    System.out.println(pdfParsed); // TODO delete this
    boolean foundAvailableToSpend = false;
    boolean foundHeader = false;
    boolean foundLine1 = false;
    boolean foundLine2 = false;
    boolean foundLineWithoutCapture = false;
    boolean foundCardholder = false;
    boolean foundCardNumber = false;

    String lastWord = null;

    for (String line : pdfParsed.split("\n")) {

      line = line.trim();
      if (StringUtils.isEmpty(line)) {
        continue;
      }

      if (line.contains("DATE Merchant AMOUNT")) {
        foundHeader = true;
      } else if (line.contains("$900.00")) {
        foundLine1 = true;
      } else if (line.contains("$9.00")) {
        foundLine2 = true;
      } else if (line.contains("$13.00")) {
        foundLineWithoutCapture = true;
      } else if (line.contains("$78.00")) {
        foundAvailableToSpend = true;
      } else if (line.contains(
          businessOwnerUser.getFirstName() + " " + businessOwnerUser.getLastName())) {
        foundCardholder = true;
      } else if (line.contains("**** " + card.getLastFour())) {
        foundCardNumber = true;
      }
      lastWord = line;
    }

    Assertions.assertTrue(foundHeader);
    Assertions.assertTrue(foundLine1);
    Assertions.assertTrue(foundLine2);
    Assertions.assertFalse(foundLineWithoutCapture);
    Assertions.assertTrue(foundAvailableToSpend);
    Assertions.assertTrue(foundCardholder);
    Assertions.assertTrue(foundCardNumber);
    Assertions.assertEquals("$909.00", lastWord);
  }
}
