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
import java.util.regex.Pattern;
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

    testHelper.createBusinessOwner(business.getId(), email, password);

    User user = createBusinessRecord.user();
    Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            user,
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);

    NetworkCommonAuthorization networkCommonAuthorization =
        TestDataController.generateAuthorizationNetworkCommon(
            user,
            card,
            createBusinessRecord.allocationRecord().account(),
            Amount.of(Currency.USD, new BigDecimal(100)));
    networkMessageService.processNetworkMessage(networkCommonAuthorization.networkCommon());
    assertThat(networkCommonAuthorization.networkCommon().isPostAdjustment()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostDecline()).isFalse();
    assertThat(networkCommonAuthorization.networkCommon().isPostHold()).isTrue();

    networkCommonAuthorization =
        TestDataController.generateAuthorizationNetworkCommon(
            user,
            card,
            createBusinessRecord.allocationRecord().account(),
            Amount.of(Currency.USD, new BigDecimal(200)));
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
      Total: 300.00
      Lowe LLC f014af24-bf28-4661-9a91-f51425b4853c
      01/17/2022 - 01/19/2022
      Date Description Amount
      01/18/2022Tuscon Bakery100.00
      01/18/2022Tuscon Bakery200.00
    */

    PdfTextExtractor pdfTextExtractor =
        new PdfTextExtractor(new PdfReader(response.getContentAsByteArray()));

    String pdfParsed = pdfTextExtractor.getTextFromPage(1);
    boolean foundTotal = false;
    boolean foundHeader = false;
    boolean foundLine1 = false;
    boolean foundLine2 = false;

    Pattern patternTotal = Pattern.compile("Total:\\s300\\.00");
    Pattern patternHeader = Pattern.compile("DATE\\sDESCRIPTION\\sAMOUNT");
    Pattern patternLine1 = Pattern.compile("^.*100\\.00$");
    Pattern patternLine2 = Pattern.compile("^.*200\\.00$");

    for (String line : pdfParsed.split("\n")) {

      line = line.trim();
      if (StringUtils.isEmpty(line)) {
        continue;
      }

      if (patternTotal.matcher(line).matches()) {
        foundTotal = true;
      } else if (patternHeader.matcher(line).matches()) {
        foundHeader = true;
      } else if (patternLine1.matcher(line).matches()) {
        foundLine1 = true;
      } else if (patternLine2.matcher(line).matches()) {
        foundLine2 = true;
      }
    }

    Assertions.assertTrue((foundTotal && foundHeader && foundLine1 && foundLine2));
  }
}
