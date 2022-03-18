package com.clearspend.capital.service;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.activity.CardStatementRequest;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.repository.CardRepositoryCustom;
import com.clearspend.capital.testutils.permission.PermissionValidationHelper;
import com.clearspend.capital.testutils.permission.PermissionValidationRole;
import com.clearspend.capital.testutils.permission.PermissionValidator;
import com.clearspend.capital.testutils.permission.RootAllocationRole;
import com.clearspend.capital.testutils.statement.StatementHelper;
import java.time.OffsetDateTime;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.function.ThrowingRunnable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;

public class CardStatementServiceTest extends BaseCapitalTest {
  @Autowired private TestHelper testHelper;
  @Autowired private CardStatementService cardStatementService;
  @Autowired private CardService cardService;
  @Autowired private StatementHelper statementHelper;
  @Autowired private PermissionValidationHelper permissionValidationHelper;
  private Card card;
  private TestHelper.CreateBusinessRecord createBusinessRecord;
  private PermissionValidator permissionValidator;

  private CardStatementRequest getRequest(final TypedId<CardId> id) {
    final CardStatementRequest request = new CardStatementRequest();
    request.setCardId(id);
    request.setStartDate(OffsetDateTime.now().minusDays(1));
    request.setEndDate(OffsetDateTime.now().plusDays(1));
    return request;
  }

  @BeforeEach
  public void setup() {
    createBusinessRecord = testHelper.createBusiness(1000L);
    testHelper.setCurrentUser(createBusinessRecord.user());
    card =
        testHelper.issueCard(
            createBusinessRecord.business(),
            createBusinessRecord.allocationRecord().allocation(),
            createBusinessRecord.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);
    statementHelper.setupStatementData(createBusinessRecord, card);
    permissionValidator = permissionValidationHelper.validator(createBusinessRecord);
  }

  @Test
  @SneakyThrows
  void generatePdf_ValidateUserPermissions() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    final CardStatementRequest request = getRequest(card.getId());
    final CardRepositoryCustom.CardDetailsRecord cardDetails =
        cardService.getCard(createBusinessRecord.user().getBusinessId(), request.getCardId());

    final Map<PermissionValidationRole, Class<? extends Exception>> failingRoles =
        Map.of(RootAllocationRole.ALLOCATION_EMPLOYEE, AccessDeniedException.class);

    final ThrowingRunnable action = () -> cardStatementService.generatePdf(request, cardDetails);
    permissionValidator.validateServiceAllocationRoles(failingRoles, action);
  }

  @Test
  @SneakyThrows
  void generatePdf_ValidatePdf() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    final CardStatementRequest request = getRequest(card.getId());
    final CardRepositoryCustom.CardDetailsRecord cardDetails =
        cardService.getCard(createBusinessRecord.user().getBusinessId(), request.getCardId());
    final CardStatementService.CardStatementRecord statementRecord =
        cardStatementService.generatePdf(request, cardDetails);
    statementHelper.validatePdfContent(statementRecord.pdf(), createBusinessRecord.user(), card);
  }
}
