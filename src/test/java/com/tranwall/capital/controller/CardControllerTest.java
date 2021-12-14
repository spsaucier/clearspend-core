package com.tranwall.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.javafaker.Faker;
import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.MockMvcHelper;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.TestHelper.CreateBusinessRecord;
import com.tranwall.capital.controller.type.card.CardDetailsResponse;
import com.tranwall.capital.controller.type.card.IssueCardRequest;
import com.tranwall.capital.controller.type.card.IssueCardResponse;
import com.tranwall.capital.controller.type.card.limits.CurrencyLimit;
import com.tranwall.capital.data.model.Bin;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.Card;
import com.tranwall.capital.data.model.Program;
import com.tranwall.capital.data.model.enums.CardType;
import com.tranwall.capital.data.model.enums.Currency;
import com.tranwall.capital.service.AllocationService.AllocationRecord;
import com.tranwall.capital.service.UserService.CreateUpdateUserRecord;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import javax.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
public class CardControllerTest extends BaseCapitalTest {

  private final TestHelper testHelper;
  private final MockMvcHelper mockMvcHelper;

  private final Faker faker = new Faker();

  private Bin bin;
  private CreateBusinessRecord createBusinessRecord;
  private Business business;
  private Program program;
  private CreateUpdateUserRecord user;
  private Cookie userCookie;
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
      userCookie = testHelper.login(user.user().getEmail().getEncrypted(), user.password());
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
  void createCard() {
    AllocationRecord allocationRecord =
        testHelper.createAllocation(
            business.getId(),
            faker.name().name(),
            createBusinessRecord.allocationRecord().allocation().getId(),
            testHelper.createUser(business).user());

    IssueCardRequest issueCardRequest =
        new IssueCardRequest(
            program.getId(),
            allocationRecord.allocation().getId(),
            user.user().getId(),
            Currency.USD,
            Set.of(CardType.VIRTUAL, CardType.PLASTIC),
            true);

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
    assertThat(cardDetailsResponse.getCard().getCardNumber())
        .isEqualTo(card.getCardNumber().getEncrypted());

    assertThat(cardDetailsResponse.getAvailableBalance()).isNotNull();
    assertThat(cardDetailsResponse.getAvailableBalance().getCurrency())
        .isEqualTo(business.getCurrency());

    assertThat(cardDetailsResponse.getLedgerBalance()).isNotNull();
    assertThat(cardDetailsResponse.getLedgerBalance().getCurrency())
        .isEqualTo(business.getCurrency());

    assertThat(cardDetailsResponse.getLimits()).isNotNull();
    CurrencyLimit currencyLimit = cardDetailsResponse.getLimits().get(0);
    assertThat(currencyLimit.getCurrency()).isEqualTo(Currency.USD);

    assertThat(cardDetailsResponse.getLimits())
        .containsOnly(new CurrencyLimit(Currency.USD, new HashMap<>()));

    assertThat(cardDetailsResponse.getDisabledMccGroups()).isEmpty();
    assertThat(cardDetailsResponse.getDisabledTransactionChannels()).isEmpty();
  }
}
