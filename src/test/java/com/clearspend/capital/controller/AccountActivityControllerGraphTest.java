package com.clearspend.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.controller.nonprod.TestDataController;
import com.clearspend.capital.controller.nonprod.TestDataController.NetworkCommonAuthorization;
import com.clearspend.capital.controller.type.activity.DashboardGraphData;
import com.clearspend.capital.controller.type.activity.GraphDataRequest;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.service.NetworkMessageService;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import com.clearspend.capital.service.type.NetworkCommon;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
@Transactional
public class AccountActivityControllerGraphTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final NetworkMessageService networkMessageService;

  @SneakyThrows
  @Test
  void getGraphData() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness(1000L);
    Business business = createBusinessRecord.business();

    CreateUpdateUserRecord user = testHelper.createUser(business);
    Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            user.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);

    List<Integer> integers = List.of(10, 2, 8, 20, 9);
    for (int amount : integers) {
      NetworkCommonAuthorization networkCommonAuthorization =
          TestDataController.generateAuthorizationNetworkCommon(
              user.user(),
              card,
              createBusinessRecord.allocationRecord().account(),
              Amount.of(Currency.USD, BigDecimal.valueOf(amount)));
      NetworkCommon common = networkCommonAuthorization.networkCommon();
      networkMessageService.processNetworkMessage(common);
      assertThat(networkCommonAuthorization.networkCommon().isPostAdjustment()).isFalse();
      assertThat(networkCommonAuthorization.networkCommon().isPostDecline()).isFalse();
      assertThat(networkCommonAuthorization.networkCommon().isPostHold()).isTrue();
      assertThat(common.getNetworkMessage().getHoldId()).isNotNull();

      common =
          TestDataController.generateCaptureNetworkCommon(
              business, networkCommonAuthorization.authorization());
      networkMessageService.processNetworkMessage(common);
      assertThat(common.isPostAdjustment()).isTrue();
      assertThat(common.isPostDecline()).isFalse();
      assertThat(common.isPostHold()).isFalse();
      assertThat(common.getNetworkMessage().getAdjustmentId()).isNotNull();
    }

    GraphDataRequest graphDataRequest = new GraphDataRequest();
    graphDataRequest.setAllocationId(createBusinessRecord.allocationRecord().allocation().getId());
    graphDataRequest.setFrom(OffsetDateTime.now().minusDays(1));
    graphDataRequest.setTo(OffsetDateTime.now().plusDays(1));

    String body = objectMapper.writeValueAsString(graphDataRequest);

    MockHttpServletResponse response =
        mvc.perform(
                post("/account-activity/graph-data")
                    .contentType("application/json")
                    .content(body)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    DashboardGraphData dashboardGraphData =
        objectMapper.readValue(response.getContentAsString(), DashboardGraphData.class);
    assertEquals(7, dashboardGraphData.getGraphData().size());
    assertEquals(
        BigDecimal.valueOf(integers.stream().mapToInt(Integer::intValue).sum(), 0).negate(),
        dashboardGraphData.getTotalSpend().setScale(0, RoundingMode.DOWN));
    assertEquals(
        dashboardGraphData.getTotalSpend().divide(new BigDecimal(7), 2, RoundingMode.DOWN),
        dashboardGraphData.getAverageSpend());
    log.info(response.getContentAsString());
  }

  @SneakyThrows
  @Test
  void getGraphDataWhenNoTransactionsArePresent() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();

    GraphDataRequest graphDataRequest = new GraphDataRequest();
    graphDataRequest.setAllocationId(createBusinessRecord.allocationRecord().allocation().getId());
    graphDataRequest.setFrom(OffsetDateTime.now().minusDays(1));
    graphDataRequest.setTo(OffsetDateTime.now().plusDays(1));

    String body = objectMapper.writeValueAsString(graphDataRequest);

    MockHttpServletResponse response =
        mvc.perform(
                post("/account-activity/graph-data")
                    .contentType("application/json")
                    .content(body)
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    DashboardGraphData dashboardGraphData =
        objectMapper.readValue(response.getContentAsString(), DashboardGraphData.class);
    assertEquals(7, dashboardGraphData.getGraphData().size());
    assertEquals(
        BigDecimal.valueOf(0).setScale(2, RoundingMode.DOWN),
        dashboardGraphData.getTotalSpend().setScale(2, RoundingMode.DOWN));
    assertEquals(
        BigDecimal.valueOf(0).setScale(2, RoundingMode.DOWN),
        dashboardGraphData.getAverageSpend().setScale(2, RoundingMode.DOWN));
    log.info(response.getContentAsString());
  }
}
