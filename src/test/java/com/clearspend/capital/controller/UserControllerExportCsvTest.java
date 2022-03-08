package com.clearspend.capital.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.controller.type.user.SearchUserRequest;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.card.CardType;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@SuppressWarnings("StringSplitter")
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
public class UserControllerExportCsvTest extends BaseCapitalTest {

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

    SearchUserRequest request = new SearchUserRequest();

    request.setSearchText(user.getLastName().getEncrypted());

    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                post("/users/export-csv")
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
      if (line.equals("Employee,Card Info,Email Address")) {
        foundHeader = true;
      } else if (line.contains(user.getFirstName() + " " + user.getLastName())) {
        foundLine = true;
      }
    }
    Assertions.assertTrue(foundHeader);
    Assertions.assertTrue(foundLine);
  }

  @SneakyThrows
  @Test
  void exportCsvNoCards() {

    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    createBusinessRecord.business();
    User user = createBusinessRecord.user();

    SearchUserRequest request = new SearchUserRequest();

    request.setSearchText(user.getLastName().getEncrypted());

    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                post("/users/export-csv")
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
      if (line.equals("Employee,Card Info,Email Address")) {
        foundHeader = true;
      } else if (line.contains(
          user.getFirstName()
              + " "
              + user.getLastName()
              + ","
              + "No cards"
              + ","
              + user.getEmail())) {
        foundLine = true;
      }
    }
    Assertions.assertTrue(foundHeader);
    Assertions.assertTrue(foundLine);
  }
}
