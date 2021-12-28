package com.clearspend.capital.controller.nonprod;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.controller.nonprod.type.networkmessage.NetworkMessageRequest;
import com.clearspend.capital.controller.nonprod.type.networkmessage.NetworkMessageResponse;
import com.clearspend.capital.controller.type.Amount;
import com.clearspend.capital.data.model.Bin;
import com.clearspend.capital.data.model.Business;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.Program;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.network.NetworkMessageType;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import com.fasterxml.jackson.core.type.TypeReference;
import java.math.BigDecimal;
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
class NetworkMessageDemoControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;

  private Bin bin;
  private CreateBusinessRecord createBusinessRecord;
  private Business business;
  private Program program;
  private CreateUpdateUserRecord user;
  private Card card;

  @SneakyThrows
  @BeforeEach
  public void setup() {
    if (bin == null) {
      bin = testHelper.createBin();
      program = testHelper.createProgram(bin);
      createBusinessRecord = testHelper.createBusiness();
      business = createBusinessRecord.business();
      user = testHelper.createUser(createBusinessRecord.business());
      card =
          testHelper.issueCard(
              business,
              createBusinessRecord.allocationRecord().allocation(),
              user.user(),
              program,
              Currency.USD);
    }
  }

  @SneakyThrows
  @Test
  void processNetworkMessage() {
    NetworkMessageRequest request =
        new NetworkMessageRequest(
            card.getId(),
            NetworkMessageType.FINANCIAL_AUTH,
            new Amount(business.getCurrency(), BigDecimal.TEN));

    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                post("/non-production/network-messages")
                    .contentType("application/json")
                    .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    NetworkMessageResponse networkMessageResponse =
        objectMapper.readValue(response.getContentAsString(), new TypeReference<>() {});

    log.info("response: " + networkMessageResponse.toString());
  }
}
