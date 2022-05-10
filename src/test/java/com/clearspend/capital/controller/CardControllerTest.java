package com.clearspend.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.MockMvcHelper;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.controller.type.PagedData;
import com.clearspend.capital.controller.type.business.BusinessLimit;
import com.clearspend.capital.controller.type.card.CardDetailsResponse;
import com.clearspend.capital.controller.type.card.EphemeralKeyRequest;
import com.clearspend.capital.controller.type.card.IssueCardRequest;
import com.clearspend.capital.controller.type.card.IssueCardResponse;
import com.clearspend.capital.controller.type.card.RevealCardRequest;
import com.clearspend.capital.controller.type.card.RevealCardResponse;
import com.clearspend.capital.controller.type.card.SearchCardData;
import com.clearspend.capital.controller.type.card.SearchCardRequest;
import com.clearspend.capital.controller.type.card.UpdateCardRequest;
import com.clearspend.capital.controller.type.card.UpdateCardStatusRequest;
import com.clearspend.capital.controller.type.card.limits.CurrencyLimit;
import com.clearspend.capital.controller.type.common.PageRequest;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.TransactionLimit;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.LimitPeriod;
import com.clearspend.capital.data.model.enums.LimitType;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.PaymentType;
import com.clearspend.capital.data.model.enums.TransactionLimitType;
import com.clearspend.capital.data.model.enums.card.CardStatus;
import com.clearspend.capital.data.model.enums.card.CardStatusReason;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.CardRepository;
import com.clearspend.capital.data.repository.TransactionLimitRepository;
import com.clearspend.capital.testutils.permission.PermissionValidationHelper;
import com.clearspend.capital.util.function.ThrowableFunctions.ThrowingFunction;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.github.javafaker.Faker;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
public class CardControllerTest extends BaseCapitalTest {

  private final TestHelper testHelper;
  private final MockMvcHelper mockMvcHelper;
  private final MockMvc mockMvc;
  private final EntityManager entityManager;
  private final CardRepository cardRepository;
  private final TransactionLimitRepository transactionLimitRepository;
  private final PermissionValidationHelper permissionValidationHelper;

  private final Faker faker = new Faker();

  private CreateBusinessRecord createBusinessRecord;
  private Business business;
  private TypedId<UserId> userId;
  private Cookie userCookie;
  private Card card;

  @SneakyThrows
  @BeforeEach
  public void setup() {
    createBusinessRecord = testHelper.createBusiness();
    business = createBusinessRecord.business();
    userId = createBusinessRecord.user().getId();
    userCookie = createBusinessRecord.authCookie();
    testHelper.setCurrentUser(createBusinessRecord.user());
    card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            entityManager.getReference(User.class, userId),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            false);
  }

  @SneakyThrows
  @Test
  void createCard() {
    TypedId<AllocationId> allocationId =
        testHelper.createAllocationMvc(
            userId,
            faker.name().name(),
            createBusinessRecord.allocationRecord().allocation().getId());

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
            Collections.emptySet(),
            Set.of(PaymentType.MANUAL_ENTRY),
            false);
    issueCardRequest.setShippingAddress(testHelper.generateApiAddress());

    entityManager.flush();

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
    assertThat(cardDetailsResponse.getCard().isActivated()).isFalse();
    assertThat(cardDetailsResponse.getCard().getCardNumber()).isNull();

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
    assertThat(cardDetailsResponse.getDisabledPaymentTypes()).isEmpty();
  }

  @Test
  @SneakyThrows
  void updateCard() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    final Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            createBusinessRecord.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);
    final Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> limits =
        Map.of(
            Currency.USD,
            Map.of(LimitType.ACH_DEPOSIT, Map.of(LimitPeriod.DAILY, BigDecimal.ZERO)));
    final Set<MccGroup> disabledCategories = Set.of(MccGroup.CHILD_CARE, MccGroup.FOOD_BEVERAGE);
    final Set<PaymentType> disabledPaymentTypes =
        Set.of(PaymentType.ONLINE, PaymentType.MANUAL_ENTRY);
    final boolean disableForeign = false;
    final UpdateCardRequest request = new UpdateCardRequest();
    request.setLimits(CurrencyLimit.ofMap(limits));
    request.setDisabledMccGroups(disabledCategories);
    request.setDisabledPaymentTypes(disabledPaymentTypes);
    request.setDisableForeign(disableForeign);

    final String content =
        mockMvc
            .perform(
                patch("/cards/%s".formatted(card.getId()))
                    .cookie(createBusinessRecord.authCookie())
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();
    final CardDetailsResponse response = objectMapper.readValue(content, CardDetailsResponse.class);
    assertThat(response)
        .hasFieldOrPropertyWithValue("limits", CurrencyLimit.ofMap(limits))
        .hasFieldOrPropertyWithValue("disabledMccGroups", disabledCategories)
        .hasFieldOrPropertyWithValue("disabledPaymentTypes", disabledPaymentTypes)
        .hasFieldOrPropertyWithValue("disableForeign", disableForeign)
        .hasFieldOrPropertyWithValue(
            "card", new com.clearspend.capital.controller.type.card.Card(card));

    final TransactionLimit transactionLimit =
        transactionLimitRepository
            .findByBusinessIdAndTypeAndOwnerId(
                createBusinessRecord.business().getId(),
                TransactionLimitType.CARD,
                card.getId().toUuid())
            .orElseThrow();
    assertThat(transactionLimit)
        .hasFieldOrPropertyWithValue("limits", limits)
        .hasFieldOrPropertyWithValue("disabledMccGroups", disabledCategories)
        .hasFieldOrPropertyWithValue("disabledPaymentTypes", disabledPaymentTypes)
        .hasFieldOrPropertyWithValue("disableForeign", disableForeign);
  }

  @Test
  @SneakyThrows
  void updateCard_CancelledCard() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            createBusinessRecord.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);
    card.setStatus(CardStatus.CANCELLED);
    card = cardRepository.saveAndFlush(card);
    final Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> limits =
        Map.of(
            Currency.USD,
            Map.of(LimitType.ACH_DEPOSIT, Map.of(LimitPeriod.DAILY, BigDecimal.ZERO)));
    final Set<MccGroup> disabledCategories = Set.of(MccGroup.CHILD_CARE, MccGroup.FOOD_BEVERAGE);
    final Set<PaymentType> disabledPaymentTypes =
        Set.of(PaymentType.ONLINE, PaymentType.MANUAL_ENTRY);
    final boolean disableForeign = false;
    final UpdateCardRequest request = new UpdateCardRequest();
    request.setLimits(CurrencyLimit.ofMap(limits));
    request.setDisabledMccGroups(disabledCategories);
    request.setDisabledPaymentTypes(disabledPaymentTypes);
    request.setDisableForeign(disableForeign);

    mockMvc
        .perform(
            patch("/cards/%s".formatted(card.getId()))
                .cookie(createBusinessRecord.authCookie())
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void updateCard_UserPermissions() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    final Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            createBusinessRecord.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);
    final Map<Currency, Map<LimitType, Map<LimitPeriod, BigDecimal>>> limits =
        Map.of(
            Currency.USD,
            Map.of(LimitType.ACH_DEPOSIT, Map.of(LimitPeriod.DAILY, BigDecimal.ZERO)));
    final Set<MccGroup> disabledCategories = Set.of(MccGroup.CHILD_CARE, MccGroup.FOOD_BEVERAGE);
    final Set<PaymentType> disabledPaymentTypes =
        Set.of(PaymentType.ONLINE, PaymentType.MANUAL_ENTRY);
    final boolean disableForeign = false;
    final UpdateCardRequest request = new UpdateCardRequest();
    request.setLimits(CurrencyLimit.ofMap(limits));
    request.setDisabledMccGroups(disabledCategories);
    request.setDisabledPaymentTypes(disabledPaymentTypes);
    request.setDisableForeign(disableForeign);

    final ThrowingFunction<Cookie, ResultActions> action =
        cookie ->
            mockMvc.perform(
                patch("/cards/%s".formatted(card.getId()))
                    .cookie(cookie)
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(request)));

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

  @Test
  @SneakyThrows
  void updateCardLimits() {
    testHelper.setCurrentUser(createBusinessRecord.user());
    // given
    Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            testHelper.createUser(business).user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            false);

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
    updateCardRequest.setDisabledMccGroups(Set.of(MccGroup.CHILD_CARE));
    updateCardRequest.setDisabledPaymentTypes(Set.of(PaymentType.MANUAL_ENTRY, PaymentType.ONLINE));
    updateCardRequest.setDisableForeign(true);

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
        .containsExactlyInAnyOrderElementsOf(updateCardRequest.getDisabledMccGroups());
    assertThat(cardDetailsResponse.getDisabledPaymentTypes())
        .containsExactlyInAnyOrderElementsOf(updateCardRequest.getDisabledPaymentTypes());
    assertThat(cardDetailsResponse.getDisableForeign()).isTrue();
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

  @SneakyThrows
  @Test
  void ephemeralKey() {
    EphemeralKeyRequest ephemeralKeyRequest = new EphemeralKeyRequest(card.getId(), "2020-03-02");
    String ephemeralKeyResponse =
        mockMvcHelper.queryObject(
            "/cards/ephemeral-key", HttpMethod.POST, userCookie, ephemeralKeyRequest, String.class);
    assertThat(ephemeralKeyResponse).isEqualTo("true");
  }

  @Test
  void physicalCardsIssuanceLimit() {
    testHelper.setIssuedPhysicalCardsLimit(createBusinessRecord.business().getId(), 10);

    BusinessLimit businessLimit =
        mockMvcHelper.queryObject(
            "/businesses/business-limit", HttpMethod.GET, userCookie, BusinessLimit.class);

    IssueCardRequest issueCardRequest =
        new IssueCardRequest(
            Set.of(CardType.PHYSICAL),
            createBusinessRecord.allocationRecord().allocation().getId(),
            userId,
            Currency.USD,
            true,
            CurrencyLimit.ofMap(Map.of(Currency.USD, Map.of())),
            Collections.emptySet(),
            Set.of(PaymentType.ONLINE),
            false);
    issueCardRequest.setShippingAddress(testHelper.generateApiAddress());

    for (int i = businessLimit.getIssuedPhysicalCardsTotal();
        i < businessLimit.getIssuedPhysicalCardsLimit();
        i++) {
      mockMvcHelper.queryList(
          "/cards", HttpMethod.POST, userCookie, issueCardRequest, new TypeReference<>() {});
    }

    assertThatThrownBy(
            () ->
                mockMvcHelper.queryList(
                    "/cards",
                    HttpMethod.POST,
                    userCookie,
                    issueCardRequest,
                    new TypeReference<>() {}))
        .isInstanceOf(AssertionError.class)
        .hasMessage("Status expected:<200> but was:<400>");
  }

  @Test
  void updateCard_CardIsUnlinked() {
    final Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            createBusinessRecord.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);
    card.setAllocationId(null);
    card.setAccountId(null);
    cardRepository.saveAndFlush(card);

    final UpdateCardRequest request = new UpdateCardRequest();
    request.setDisableForeign(true);
    final CardDetailsResponse response =
        mockMvcHelper.queryObject(
            "/cards/%s".formatted(card.getId()),
            HttpMethod.PATCH,
            createBusinessRecord.authCookie(),
            request,
            CardDetailsResponse.class);
    assertThat(response.getCard())
        .hasFieldOrPropertyWithValue("allocationId", null)
        .hasFieldOrPropertyWithValue("accountId", null)
        .hasFieldOrPropertyWithValue("cardId", card.getId());
    assertThat(response).hasFieldOrPropertyWithValue("disableForeign", true);
  }

  @Test
  void getCard_CardIsUnlinked() {
    final Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            createBusinessRecord.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);
    card.setAllocationId(null);
    card.setAccountId(null);
    cardRepository.saveAndFlush(card);
    final CardDetailsResponse result =
        mockMvcHelper.queryObject(
            "/cards/%s".formatted(card.getId()),
            HttpMethod.GET,
            createBusinessRecord.authCookie(),
            new UpdateCardStatusRequest(CardStatusReason.CARDHOLDER_REQUESTED),
            CardDetailsResponse.class);

    assertThat(result.getCard())
        .hasFieldOrPropertyWithValue("allocationId", null)
        .hasFieldOrPropertyWithValue("accountId", null)
        .hasFieldOrPropertyWithValue("cardId", card.getId());
    assertThat(result)
        .hasFieldOrPropertyWithValue("ledgerBalance", null)
        .hasFieldOrPropertyWithValue("availableBalance", null)
        .hasFieldOrPropertyWithValue("allocationName", null);
  }

  @Test
  void search_OneCardIsUnlinked() {
    final Card card =
        testHelper.issueCard(
            business,
            createBusinessRecord.allocationRecord().allocation(),
            createBusinessRecord.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.PHYSICAL,
            true);
    card.setAllocationId(null);
    card.setAccountId(null);
    cardRepository.saveAndFlush(card);

    final SearchCardRequest request = new SearchCardRequest(new PageRequest(0, 100));
    final JavaType responseType =
        objectMapper
            .getTypeFactory()
            .constructParametricType(PagedData.class, SearchCardData.class);
    final PagedData<SearchCardData> result =
        mockMvcHelper.queryObject(
            "/cards/search",
            HttpMethod.POST,
            createBusinessRecord.authCookie(),
            request,
            responseType);
    assertThat(result).hasFieldOrPropertyWithValue("totalElements", 2L);
    final List<SearchCardData> unlinkedCards =
        result.getContent().stream()
            .filter(data -> data.getAllocation() == null)
            .collect(Collectors.toList());
    assertThat(unlinkedCards).hasSize(1);
    assertThat(unlinkedCards.get(0)).hasFieldOrPropertyWithValue("cardId", card.getId());
  }
}
