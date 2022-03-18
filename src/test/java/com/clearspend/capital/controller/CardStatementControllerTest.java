package com.clearspend.capital.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.controller.type.activity.CardStatementRequest;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.testutils.permission.PermissionValidationHelper;
import com.clearspend.capital.testutils.permission.PermissionValidationRole;
import com.clearspend.capital.testutils.permission.PermissionValidator;
import com.clearspend.capital.testutils.permission.RootAllocationRole;
import com.clearspend.capital.testutils.statement.StatementHelper;
import java.time.OffsetDateTime;
import java.util.Map;
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
import org.springframework.test.web.servlet.ResultMatcher;
import org.testcontainers.utility.ThrowingFunction;

@SuppressWarnings({"JavaTimeDefaultTimeZone", "StringSplitter"})
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
public class CardStatementControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final StatementHelper statementHelper;
  private final PermissionValidationHelper permissionValidationHelper;

  private CreateBusinessRecord createBusinessRecord;
  private Card card;
  private User businessOwnerUser;
  private PermissionValidator permissionValidator;

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
    permissionValidator = permissionValidationHelper.validator(createBusinessRecord);
  }

  @Test
  @SneakyThrows
  void getCardStatement_ValidateUserPermissions() {
    final RequestObjAndString request = getCardStatementRequest();

    final ThrowingFunction<Cookie, ResultActions> action =
        (cookie) ->
            mvc.perform(
                post("/card-statement")
                    .contentType("application/json")
                    .content(request.requestString())
                    .cookie(cookie));
    final Map<PermissionValidationRole, ResultMatcher> failingStatuses =
        Map.of(
            RootAllocationRole.ALLOCATION_EMPLOYEE, status().isForbidden(),
            RootAllocationRole.ALLOCATION_VIEW_ONLY, status().isForbidden());
    permissionValidator.validateMvcAllocationRoles(failingStatuses, action);
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
  @Test
  void getCardStatement_ByBusinessOwnerUser_ValidateResponse() {
    final RequestObjAndString request = getCardStatementRequest();
    MockHttpServletResponse response =
        mvc.perform(
                post("/card-statement")
                    .contentType("application/json")
                    .content(request.requestString())
                    .cookie(createBusinessRecord.authCookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    statementHelper.validatePdfContent(
        response.getContentAsByteArray(), createBusinessRecord.user(), card);
  }
}
