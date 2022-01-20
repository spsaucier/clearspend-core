package com.clearspend.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.MockMvcHelper;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.controller.type.card.CardDetailsResponse;
import com.clearspend.capital.controller.type.card.IssueCardRequest;
import com.clearspend.capital.controller.type.card.IssueCardResponse;
import com.clearspend.capital.controller.type.card.RevealCardRequest;
import com.clearspend.capital.controller.type.card.RevealCardResponse;
import com.clearspend.capital.controller.type.card.UpdateCardRequest;
import com.clearspend.capital.controller.type.card.limits.CurrencyLimit;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.model.enums.TransactionChannel;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.service.MccGroupService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.github.javafaker.Faker;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.persistence.EntityManager;
import javax.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
public class CardControllerTest extends BaseCapitalTest {

  private final TestHelper testHelper;
  private final MockMvcHelper mockMvcHelper;
  private final MccGroupService mccGroupService;
  private final EntityManager entityManager;

  private final Faker faker = new Faker();

  private CreateBusinessRecord createBusinessRecord;
  private Business business;
  private TypedId<UserId> userId;
  private Cookie userCookie;
  private Card card;

  @SneakyThrows
  @BeforeEach
  public void setup() {
    if (createBusinessRecord == null) {
      createBusinessRecord = testHelper.createBusiness();
      business = createBusinessRecord.business();
      userId = createBusinessRecord.allocationRecord().allocation().getOwnerId();
      userCookie = testHelper.login(userId);
      card =
          testHelper.issueCard(
              business,
              createBusinessRecord.allocationRecord().allocation(),
              entityManager.getReference(User.class, userId),
              Currency.USD,
              FundingType.POOLED,
              CardType.PHYSICAL);
    }
  }

  @SneakyThrows
  @Test
  void createCard() {
    TypedId<AllocationId> allocationId =
        testHelper.createAllocationMvc(
            createBusinessRecord.allocationRecord().allocation().getOwnerId(),
            faker.name().name(),
            createBusinessRecord.allocationRecord().allocation().getId(),
            testHelper.createUser(business).user().getId());

    IssueCardRequest issueCardRequest =
        new IssueCardRequest(
            Set.of(CardType.VIRTUAL, CardType.PHYSICAL),
            allocationId,
            userId,
            Currency.USD,
            true,
            CurrencyLimit.ofMap(
                Map.of(
                    Currency.USD,
                    Map.of(
                        LimitType.PURCHASE,
                        Map.of(
                            LimitPeriod.DAILY,
                            BigDecimal.ONE,
                            LimitPeriod.MONTHLY,
                            BigDecimal.TEN)))),
            Collections.emptyList(),
            Set.of(TransactionChannel.MOTO));
    issueCardRequest.setShippingAddress(testHelper.generateApiAddress());

    List<IssueCardResponse> issueCardResponse =
        mockMvcHelper.queryList(
            "/cards", HttpMethod.POST, userCookie, issueCardRequest, new TypeReference<>() {});

    assertThat(issueCardResponse).hasSize(2);
  }

  @SneakyThrows
  @Test
  void getUserCard() {
    CardDetailsResponse cardDetailsResponse =
        mockMvcHelper.queryObject(
            "/cards/" + card.getId().toString(),
            HttpMethod.GET,
            userCookie,
            CardDetailsResponse.class);

    assertThat(cardDetailsResponse.getCard()).isNotNull();
    assertThat(cardDetailsResponse.getCard().getCardNumber()).isEqualTo(card.getLastFour());

    assertThat(cardDetailsResponse.getAvailableBalance()).isNotNull();
    assertThat(cardDetailsResponse.getAvailableBalance().getCurrency())
        .isEqualTo(business.getCurrency());

    assertThat(cardDetailsResponse.getLedgerBalance()).isNotNull();
    assertThat(cardDetailsResponse.getLedgerBalance().getCurrency())
        .isEqualTo(business.getCurrency());

    assertThat(cardDetailsResponse.getLimits()).isNotNull();
    CurrencyLimit currencyLimit = cardDetailsResponse.getLimits().get(0);
    Assertions.assertThat(currencyLimit.getCurrency()).isEqualTo(Currency.USD);

    assertThat(cardDetailsResponse.getLimits())
        .containsOnly(new CurrencyLimit(Currency.USD, new HashMap<>()));

    assertThat(cardDetailsResponse.getDisabledMccGroups()).isEmpty();
    assertThat(cardDetailsResponse.getDisabledTransactionChannels()).isEmpty();
  }

  @Test
  @SneakyThrows
  void updateCardLimits() {
    // given
    Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            testHelper.createUser(business).user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL);

    // when
    UpdateCardRequest updateCardRequest = new UpdateCardRequest();
    updateCardRequest.setLimits(
        CurrencyLimit.ofMap(
            Map.of(
                Currency.USD,
                Map.of(
                    LimitType.PURCHASE,
                    Map.of(
                        LimitPeriod.DAILY, BigDecimal.ONE, LimitPeriod.MONTHLY, BigDecimal.TEN)))));
    updateCardRequest.setDisabledMccGroups(
        Collections.singletonList(
            mccGroupService.retrieveMccGroups(business.getId()).get(0).getId()));
    updateCardRequest.setDisabledTransactionChannels(
        Set.of(TransactionChannel.MOTO, TransactionChannel.ONLINE));

    CardDetailsResponse cardDetailsResponse =
        mockMvcHelper.queryObject(
            "/cards/" + card.getId(),
            HttpMethod.PATCH,
            userCookie,
            updateCardRequest,
            CardDetailsResponse.class);

    // then
    assertThat(cardDetailsResponse.getLimits())
        .containsExactlyInAnyOrderElementsOf(updateCardRequest.getLimits());
    assertThat(cardDetailsResponse.getDisabledMccGroups())
        .containsExactlyInAnyOrderElementsOf((updateCardRequest.getDisabledMccGroups()));
    assertThat(cardDetailsResponse.getDisabledTransactionChannels())
        .containsExactlyInAnyOrderElementsOf((updateCardRequest.getDisabledTransactionChannels()));
  }

  @SneakyThrows
  @Test
  void revealCard() {
    RevealCardRequest revealCardRequest = new RevealCardRequest(card.getId(), "test-nonce");
    RevealCardResponse revealCardResponse =
        mockMvcHelper.queryObject(
            "/cards/reveal",
            HttpMethod.POST,
            userCookie,
            revealCardRequest,
            RevealCardResponse.class);
    assertThat(revealCardResponse.getExternalRef()).isNotNull();
    assertThat(revealCardResponse.getEphemeralKey()).isNotNull();
  }
}
