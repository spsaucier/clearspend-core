package com.tranwall.capital.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.github.javafaker.Faker;
import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.controller.type.card.IssueCardRequest;
import com.tranwall.capital.controller.type.card.IssueCardResponse;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.enums.CardType;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.service.AllocationService.AllocationRecord;
import com.tranwall.capital.service.UserService.CreateUserRecord;
import java.util.List;
import java.util.Set;
import javax.servlet.http.Cookie;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
@Transactional
public class CardControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;

  private final Faker faker = new Faker();

  private Cookie authCookie;

  @BeforeEach
  void init() {
    testHelper.init();
    this.authCookie = testHelper.login("business-owner-tester@clearspend.com", "Password1!");
  }

  @SneakyThrows
  @Test
  void createCard() {
    Bin bin = testHelper.createBin();
    Program program = testHelper.createProgram(bin);
    Business business = testHelper.retrieveBusiness();
    AllocationRecord allocationRecord =
        testHelper.createAllocation(program.getId(), business.getId(), "", null);
    CreateUserRecord user = testHelper.createUser(business);

    IssueCardRequest issueCardRequest =
        new IssueCardRequest(
            program.getId(),
            allocationRecord.allocation().getId(),
            user.user().getId(),
            Currency.USD,
            Set.of(CardType.VIRTUAL, CardType.PLASTIC),
            true);

    String body = objectMapper.writeValueAsString(issueCardRequest);

    MockHttpServletResponse response =
        mvc.perform(
                post("/cards")
                    .header("businessId", business.getId().toString())
                    .contentType("application/json")
                    .content(body)
                    .cookie(authCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    List<IssueCardResponse> issueCardResponse =
        objectMapper.readValue(response.getContentAsString(), List.class);

    log.info(response.getContentAsString());
  }
}
