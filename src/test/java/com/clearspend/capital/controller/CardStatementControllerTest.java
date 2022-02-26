package com.clearspend.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.controller.nonprod.TestDataController;
import com.clearspend.capital.controller.nonprod.TestDataController.NetworkCommonAuthorization;
import com.clearspend.capital.controller.type.activity.CardStatementRequest;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.service.NetworkMessageService;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@SuppressWarnings({"JavaTimeDefaultTimeZone", "StringSplitter"})
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
@Transactional
public class CardStatementControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final NetworkMessageService networkMessageService;

  @SneakyThrows
  @Test
  void getCardStatement() {
    String email = testHelper.generateEmail();
    String password = testHelper.generatePassword();
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness(1000L);
    Business business = createBusinessRecord.business();

    User user = createBusinessRecord.user();
    testHelper.setCurrentUser(user);
    Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            user,
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);

    testHelper.setCurrentUser(user);
    NetworkCommonAuthorization networkCommonAuthorization =
        TestDataController.generateAuthorizationNetworkCommon(
            user,
            card,
            createBusinessRecord.allocationRecord().account(),
            Amount.of(Currency.USD, new BigDecimal(900)));
    networkMessageService.processNetworkMessage(networkCommonAuthorization.networkCommon());
    assertThat(networkCommonAuthorization.networkCommon().isPostAdjustment()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostDecline()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostHold()).isTrue();

    networkCommonAuthorization =
        TestDataController.generateAuthorizationNetworkCommon(
            user,
            card,
            createBusinessRecord.allocationRecord().account(),
            Amount.of(Currency.USD, new BigDecimal(9)));
    networkMessageService.processNetworkMessage(networkCommonAuthorization.networkCommon());

    assertThat(networkCommonAuthorization.networkCommon().isPostAdjustment()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostDecline()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostHold()).isTrue();

    CardStatementRequest cardStatementRequest = new CardStatementRequest();
    cardStatementRequest.setCardId(card.getId());
    cardStatementRequest.setStartDate(OffsetDateTime.now().minusDays(1));
    cardStatementRequest.setEndDate(OffsetDateTime.now().plusDays(1));

    String body = objectMapper.writeValueAsString(cardStatementRequest);

    MockHttpServletResponse response =
        mvc.perform(
                post("/card-statement")
                    .contentType("application/json")
                    .content(body)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

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

    PdfTextExtractor pdfTextExtractor =
        new PdfTextExtractor(new PdfReader(response.getContentAsByteArray()));

    String pdfParsed = pdfTextExtractor.getTextFromPage(1);
    boolean foundAvailableToSpend = false;
    boolean foundHeader = false;
    boolean foundLine1 = false;
    boolean foundLine2 = false;
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
      } else if (line.contains("$91.00")) {
        foundAvailableToSpend = true;
      } else if (line.contains(user.getFirstName() + " " + user.getLastName())) {
        foundCardholder = true;
      } else if (line.contains("**** " + card.getLastFour())) {
        foundCardNumber = true;
      }
      lastWord = line;
    }

    Assertions.assertTrue(foundHeader);
    Assertions.assertTrue(foundLine1);
    Assertions.assertTrue(foundLine2);
    Assertions.assertTrue(foundAvailableToSpend);
    Assertions.assertTrue(foundCardholder);
    Assertions.assertTrue(foundCardNumber);
    Assertions.assertEquals("$909.00", lastWord);
  }
}
