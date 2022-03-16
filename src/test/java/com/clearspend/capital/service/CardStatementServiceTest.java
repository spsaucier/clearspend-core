package com.clearspend.capital.service;

import static org.junit.jupiter.api.Assertions.assertThrows;

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
import com.clearspend.capital.testutils.statement.StatementHelper;
import java.time.OffsetDateTime;
import lombok.SneakyThrows;
import org.assertj.core.api.ThrowingConsumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;

public class CardStatementServiceTest extends BaseCapitalTest {
  @Autowired private TestHelper testHelper;
  @Autowired private CardStatementService cardStatementService;
  @Autowired private CardService cardService;
  @Autowired private StatementHelper statementHelper;
  private Card card;
  private TestHelper.CreateBusinessRecord createBusinessRecord;

  private CardStatementRequest getRequest(final TypedId<CardId> id) {
    final var request = new CardStatementRequest();
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
  void generatePdf_ValidateUserPermissions() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    final var request = getRequest(card.getId());
    final var cardDetails =
        cardService.getCard(createBusinessRecord.user().getBusinessId(), request.getCardId());

    final ThrowingConsumer<String> doGenerate =
        (role) -> {
          testHelper.setCurrentUser(createBusinessRecord.user());
          final var user =
              testHelper.createUserWithRole(
                  createBusinessRecord.allocationRecord().allocation(), role);
          testHelper.setCurrentUser(user.user());
          cardStatementService.generatePdf(request, cardDetails);
        };

    // If no exception thrown, permissions are good
    doGenerate.accept(DefaultRoles.ALLOCATION_ADMIN);
    doGenerate.accept(DefaultRoles.ALLOCATION_MANAGER);
    assertThrows(
        AccessDeniedException.class, () -> doGenerate.accept(DefaultRoles.ALLOCATION_EMPLOYEE));
    doGenerate.accept(DefaultRoles.ALLOCATION_VIEW_ONLY);
  }

  @Test
  @SneakyThrows
  void generatePdf_ValidatePdf() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    final var request = getRequest(card.getId());
    final var cardDetails =
        cardService.getCard(createBusinessRecord.user().getBusinessId(), request.getCardId());
    final var statementRecord = cardStatementService.generatePdf(request, cardDetails);
    statementHelper.validatePdfContent(statementRecord.pdf(), createBusinessRecord.user(), card);
  }
}
