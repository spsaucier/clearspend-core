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
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.CardRepositoryCustom;
import com.clearspend.capital.testutils.permission.PermissionValidationHelper;
import com.clearspend.capital.testutils.statement.StatementHelper;
import java.time.OffsetDateTime;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.function.ThrowingRunnable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

public class StatementServiceTest extends BaseCapitalTest {
  @Autowired private TestHelper testHelper;
  @Autowired private StatementService statementService;
  @Autowired private CardService cardService;
  @Autowired private StatementHelper statementHelper;
  @Autowired private PermissionValidationHelper permissionValidationHelper;
  private Card card;
  private TestHelper.CreateBusinessRecord createBusinessRecord;

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
  }

  @Test
  @SneakyThrows
  void generateCardStatementPdf_ValidateUserPermissions() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    final CardStatementRequest request = getRequest(card.getId());
    final CardRepositoryCustom.CardDetailsRecord cardDetails =
        cardService.getCard(createBusinessRecord.user().getBusinessId(), request.getCardId());

    final ThrowingRunnable action =
        () -> statementService.generateCardStatementPdf(request, cardDetails);

    permissionValidationHelper
        .buildValidator(createBusinessRecord)
        .allowRolesOnAllocation(
            Set.of(
                DefaultRoles.ALLOCATION_ADMIN,
                DefaultRoles.ALLOCATION_MANAGER,
                DefaultRoles.ALLOCATION_VIEW_ONLY))
        .allowGlobalRoles(
            Set.of(
                DefaultRoles.GLOBAL_CUSTOMER_SERVICE,
                DefaultRoles.GLOBAL_CUSTOMER_SERVICE_MANAGER,
                DefaultRoles.GLOBAL_VIEWER))
        .build()
        .validateServiceMethod(action);
  }

  @Test
  @SneakyThrows
  void generateCardStatementPdf_ValidatePdf() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    final CardStatementRequest request = getRequest(card.getId());
    final CardRepositoryCustom.CardDetailsRecord cardDetails =
        cardService.getCard(createBusinessRecord.user().getBusinessId(), request.getCardId());
    final StatementService.StatementRecord statementRecord =
        statementService.generateCardStatementPdf(request, cardDetails);
    statementHelper.validateCardPdfContent(
        statementRecord.pdf(), createBusinessRecord.user(), card);
  }
}
