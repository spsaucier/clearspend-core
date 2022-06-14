package com.clearspend.capital.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.client.twilio.TwilioServiceMock;
import com.clearspend.capital.client.twilio.TwilioServiceMock.LastLowBalanceEmail;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.util.MustacheResourceLoader;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.model.notifications.AllocationNotificationsSettings;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.data.repository.AccountRepository;
import com.clearspend.capital.data.repository.AllocationNotificationSettingRepository;
import com.samskivert.mustache.Template;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class LowBalanceNotificationServiceTest extends BaseCapitalTest {
  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final TwilioServiceMock twilioServiceMock;
  private final AllocationNotificationSettingRepository allocationNotificationSettingRepository;
  private final AccountRepository accountRepository;
  private final Template authorizationTemplate =
      MustacheResourceLoader.load("stripeEvents/authorizationTemplate.json");

  private CreateBusinessRecord createBusinessRecord;
  private Card card;

  @BeforeEach
  void setup() {
    createBusinessRecord = testHelper.createBusiness();
    testHelper.setCurrentUser(createBusinessRecord.user());
    testHelper.createUserWithRole(
        createBusinessRecord.allocationRecord().allocation(), DefaultRoles.ALLOCATION_MANAGER);
    card =
        testHelper.issueCard(
            createBusinessRecord.business(),
            createBusinessRecord.allocationRecord().allocation(),
            createBusinessRecord.user(),
            Currency.USD,
            FundingType.POOLED,
            CardType.VIRTUAL,
            false);
    twilioServiceMock.getLastLowBalanceEmail().clear();
  }

  private Map<String, Object> createAuthRequestParams(final int amount) {
    return Map.of(
        "cardExternalRef",
        card.getExternalRef(),
        "stripeAccountId",
        createBusinessRecord.business().getStripeData().getAccountRef(),
        "userId",
        createBusinessRecord.user().getId().toUuid().toString(),
        "businessId",
        createBusinessRecord.business().getId().toUuid().toString(),
        "cardId",
        card.getId().toUuid().toString(),
        "amount",
        amount);
  }

  private void enableLowBalanceSetting(final Amount lowBalanceLevel) {
    final AllocationNotificationsSettings settings = new AllocationNotificationsSettings();
    settings.setAllocationId(createBusinessRecord.allocationRecord().allocation().getId());
    settings.setLowBalance(true);
    settings.setLowBalanceLevel(lowBalanceLevel);
    settings.setRecipients(Set.of(createBusinessRecord.user().getId()));
    allocationNotificationSettingRepository.saveAndFlush(settings);
  }

  private void setAccountBalance(final Amount amount) {
    createBusinessRecord.allocationRecord().account().setLedgerBalance(amount);
    accountRepository.saveAndFlush(createBusinessRecord.allocationRecord().account());
  }

  @Test
  void sendsNotificationForLowBalance() {
    enableLowBalanceSetting(Amount.of(Currency.USD, 50));
    setAccountBalance(Amount.of(Currency.USD, 100));

    final String json = authorizationTemplate.execute(createAuthRequestParams(7000));
    sendStripeJson(json);

    verifyEmailSent(Amount.of(Currency.USD, 50));
  }

  private void verifyEmailSent(final Amount amount) {
    verifyLowBalanceEmail(true, amount);
  }

  private void verifyEmailNotSent() {
    verifyLowBalanceEmail(false, null);
  }

  private void verifyLowBalanceEmail(final boolean emailShouldHaveSent, final Amount amount) {
    if (emailShouldHaveSent) {
      assertThat(twilioServiceMock)
          .hasFieldOrPropertyWithValue(
              "lastLowBalanceEmail",
              List.of(
                  new LastLowBalanceEmail(
                      createBusinessRecord.user().getEmail().getEncrypted(), amount)));
    } else {
      assertThat(twilioServiceMock).hasFieldOrPropertyWithValue("lastLowBalanceEmail", List.of());
    }
  }

  @SneakyThrows
  private void sendStripeJson(final String json) {
    mvc.perform(
            post("/stripe/webhook/issuing")
                .content(json)
                .header("skip-stripe-header-verification", "true")
                .contentType("application/json"))
        .andExpect(status().isOk());
  }

  @Test
  void skipsNotificationWhenSettingOff() {
    setAccountBalance(Amount.of(Currency.USD, 100));

    final String json = authorizationTemplate.execute(createAuthRequestParams(7000));
    sendStripeJson(json);

    verifyEmailNotSent();
  }

  @Test
  void skipsNotificationWhenThresholdNotMet() {
    enableLowBalanceSetting(Amount.of(Currency.USD, 50));
    setAccountBalance(Amount.of(Currency.USD, 100));

    final String json = authorizationTemplate.execute(createAuthRequestParams(3000));
    sendStripeJson(json);

    verifyEmailNotSent();
  }

  @Test
  void skipsNotificationWhenTriggeredTooFrequently() {
    enableLowBalanceSetting(Amount.of(Currency.USD, 50));
    setAccountBalance(Amount.of(Currency.USD, 100));

    final String json = authorizationTemplate.execute(createAuthRequestParams(6000));
    sendStripeJson(json);

    verifyEmailSent(Amount.of(Currency.USD, 50));
    twilioServiceMock.getLastLowBalanceEmail().clear();

    final String json2 = authorizationTemplate.execute(createAuthRequestParams(1000));
    sendStripeJson(json2);

    verifyEmailNotSent();
  }
}
