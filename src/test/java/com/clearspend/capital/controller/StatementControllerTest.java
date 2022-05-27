package com.clearspend.capital.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.controller.type.activity.BusinessStatementRequest;
import com.clearspend.capital.controller.type.activity.CardStatementRequest;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.testutils.permission.PermissionValidationHelper;
import com.clearspend.capital.testutils.statement.StatementHelper;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.Set;
import javax.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.utility.ThrowingFunction;

@SuppressWarnings({"JavaTimeDefaultTimeZone", "StringSplitter"})
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
public class StatementControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final StatementHelper statementHelper;
  private final PermissionValidationHelper permissionValidationHelper;

  private CreateBusinessRecord createBusinessRecord;
  private Card card;
  private User businessOwnerUser;

  private record RequestObjAndString(CardStatementRequest request, String requestString) {}

  @BeforeEach
  public void setup() {
    createBusinessRecord = testHelper.createBusiness(1000L);
    Business business = createBusinessRecord.business();

    businessOwnerUser = createBusinessRecord.user();
    testHelper.setCurrentUser(businessOwnerUser);
    card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            businessOwnerUser,
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);

    testHelper.setCurrentUser(businessOwnerUser);
    statementHelper.setupStatementData(createBusinessRecord, card);
  }

  @Test
  @SneakyThrows
  void getCardStatement_ValidateUserPermissions() {
    final RequestObjAndString request = getCardStatementRequest();

    final ThrowingFunction<Cookie, ResultActions> action =
        (cookie) ->
            mvc.perform(
                post("/statements/card")
                    .contentType("application/json")
                    .content(request.requestString())
                    .cookie(cookie));
    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowRolesOnAllocation(
            Set.of(DefaultRoles.ALLOCATION_ADMIN, DefaultRoles.ALLOCATION_MANAGER))
        .allowGlobalRoles(
            Set.of(
                DefaultRoles.GLOBAL_CUSTOMER_SERVICE, DefaultRoles.GLOBAL_CUSTOMER_SERVICE_MANAGER))
        .build()
        .validateMockMvcCall(action);
  }

  @SneakyThrows
  private RequestObjAndString getCardStatementRequest() {
    final CardStatementRequest cardStatementRequest = new CardStatementRequest();
    cardStatementRequest.setCardId(card.getId());
    cardStatementRequest.setStartDate(OffsetDateTime.now().minusDays(1));
    cardStatementRequest.setEndDate(OffsetDateTime.now().plusDays(1));

    final String body = objectMapper.writeValueAsString(cardStatementRequest);
    return new RequestObjAndString(cardStatementRequest, body);
  }

  @SneakyThrows
  private String getBusinessStatementRequest() {
    BusinessStatementRequest businessStatementRequest = new BusinessStatementRequest();
    businessStatementRequest.setStartDate(
        OffsetDateTime.now().with(TemporalAdjusters.firstDayOfMonth()));
    businessStatementRequest.setEndDate(
        OffsetDateTime.now().with(TemporalAdjusters.lastDayOfMonth()));

    return objectMapper.writeValueAsString(businessStatementRequest);
  }

  @SneakyThrows
  @Test
  void getCardStatement_ByBusinessOwnerUser_ValidateResponse() {
    final RequestObjAndString request = getCardStatementRequest();
    MockHttpServletResponse response =
        mvc.perform(
                post("/statements/card")
                    .contentType("application/json")
                    .content(request.requestString())
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    statementHelper.validateCardPdfContent(
        response.getContentAsByteArray(), createBusinessRecord.user(), card);
  }

  @SneakyThrows
  @Test
  void getBusinessStatement_ByBusinessOwnerUser_ValidateResponse() {
    MockHttpServletResponse response =
        mvc.perform(
                post("/statements/business")
                    .contentType("application/json")
                    .content(getBusinessStatementRequest())
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    statementHelper.validateBusinessPdfContent(
        response.getContentAsByteArray(), createBusinessRecord.user(), card);
  }
}
