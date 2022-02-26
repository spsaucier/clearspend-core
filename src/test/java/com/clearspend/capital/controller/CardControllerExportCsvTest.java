package com.clearspend.capital.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.controller.type.card.SearchCardRequest;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.card.CardType;
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
public class CardControllerExportCsvTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;

  @SneakyThrows
  @Test
  void exportCsv() {

    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business = createBusinessRecord.business();

    User user = createBusinessRecord.user();
    testHelper.issueCard(
        business,
        createBusinessRecord.allocationRecord().allocation(),
        user,
        Currency.USD,
        FundingType.POOLED,
        CardType.PHYSICAL,
        false);

    SearchCardRequest request = new SearchCardRequest(new PageRequest(0, Integer.MAX_VALUE));

    request.setSearchText(user.getLastName().getEncrypted());
    request.setUserId(user.getId());
    request.setAllocationId(createBusinessRecord.allocationRecord().allocation().getId());

    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                post("/cards/export-csv")
                    .contentType("application/json")
                    .content(body)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    String csvResult = response.getContentAsString();

    boolean foundHeader = false;
    boolean foundLine = false;

    for (String line : csvResult.split("\n")) {
      line = line.trim();
      if (StringUtils.isEmpty(line)) {
        continue;
      }
      if (line.equals("Card Number,Employee,Allocation,Balance,Status")) {
        foundHeader = true;
      } else if (line.contains(user.getFirstName() + " " + user.getLastName())) {
        foundLine = true;
      }
    }

    Assertions.assertTrue(foundHeader);
    Assertions.assertTrue(foundLine);
  }
}
