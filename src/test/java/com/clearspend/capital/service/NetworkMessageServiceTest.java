package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.MerchantType;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.model.enums.network.NetworkMessageType;
import com.clearspend.capital.data.model.network.StripeWebhookLog;
import com.clearspend.capital.service.type.NetworkCommon;
import java.math.BigDecimal;
import java.util.stream.Stream;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

@Slf4j
class NetworkMessageServiceTest extends BaseCapitalTest {

  @Autowired private TestHelper testHelper;

  @Autowired private NetworkMessageService networkMessageService;

  private CreateBusinessRecord createBusinessRecord;
  private Allocation allocation;
  private Business business;
  private Card card;
  private User user;

  @BeforeEach
  public void setup() {
    if (createBusinessRecord == null) {
      createBusinessRecord = testHelper.createBusiness();
      business = createBusinessRecord.business();
      allocation = createBusinessRecord.allocationRecord().allocation();
      user = createBusinessRecord.user();
      card =
          testHelper.issueCard(
              business,
              allocation,
              user,
              business.getCurrency(),
              FundingType.POOLED,
              CardType.VIRTUAL);
    }
  }

  private static Stream<Arguments> buildCardStatuses() {
    return Stream.of(
        Arguments.of(MerchantType.TRANSPORTATION_SERVICES, BigDecimal.TEN, BigDecimal.TEN.negate()),
        Arguments.of(
            MerchantType.HOTELS_MOTELS_AND_RESORTS, BigDecimal.TEN, BigDecimal.valueOf(-11.5)),
        Arguments.of(
            MerchantType.EATING_PLACES_RESTAURANTS, BigDecimal.TEN, BigDecimal.valueOf(-12)),
        Arguments.of(
            MerchantType.AUTOMATED_FUEL_DISPENSERS, BigDecimal.TEN, BigDecimal.valueOf(-100)));
  }

  @ParameterizedTest
  @MethodSource("buildCardStatuses")
  void determinePaddedAmount(
      MerchantType merchantType, BigDecimal amount, BigDecimal expectedPaddedAmount) {

    Amount authAmount = Amount.of(Currency.USD, amount);
    NetworkCommon networkCommon =
        new NetworkCommon(
            NetworkMessageType.AUTH_CREATED,
            testHelper.getAuthorization(
                business, user, card, merchantType, authAmount.toStripeAmount(), 0, null),
            new StripeWebhookLog());

    assertThat(networkCommon.getRequestedAmount().getAmount())
        .isEqualByComparingTo(amount.negate());
    networkMessageService.setPaddedAmountAndHoldPeriod(networkCommon);
    BigDecimal paddedAmount = networkCommon.getPaddedAmount().getAmount();

    log.debug("{} - {} - {} - {}", merchantType, amount, expectedPaddedAmount, paddedAmount);
    assertThat(paddedAmount).isEqualByComparingTo(expectedPaddedAmount);
  }
}
