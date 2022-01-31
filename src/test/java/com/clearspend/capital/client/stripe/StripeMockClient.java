package com.clearspend.capital.client.stripe;

import com.clearspend.capital.client.stripe.types.FinancialAccount;
import com.clearspend.capital.client.stripe.types.InboundTransfer;
import com.clearspend.capital.client.stripe.types.OutboundPayment;
import com.clearspend.capital.client.stripe.types.OutboundTransfer;
import com.clearspend.capital.common.data.model.Address;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.model.ClearAddress;
import com.clearspend.capital.common.typedid.data.AdjustmentId;
import com.clearspend.capital.common.typedid.data.HoldId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.enums.card.CardStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import com.stripe.model.Account;
import com.stripe.model.Person;
import com.stripe.model.SetupIntent;
import com.stripe.model.issuing.Card;
import com.stripe.model.issuing.Cardholder;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@Profile("test")
public class StripeMockClient extends StripeClient {

  private static final String fakerRandom32SymbolsPattern = "????????????????????????????????";
  private static final Faker faker = new Faker();

  public StripeMockClient(
      StripeProperties stripeProperties,
      ObjectMapper objectMapper,
      WebClient stripeTreasuryWebClient) {
    super(stripeProperties, objectMapper, stripeTreasuryWebClient);
  }

  @Override
  public Account createAccount(Business business) {
    return generateEntityWithId(Account.class);
  }

  @Override
  public Cardholder createCardholder(User user, ClearAddress billingAddress) {
    return generateEntityWithId(Cardholder.class);
  }

  @Override
  public Person createPerson(BusinessOwner businessOwner, String businessExternalRef) {
    return generateEntityWithId(Person.class);
  }

  @Override
  public Card createVirtualCard(
      com.clearspend.capital.data.model.Card card, String userExternalRef) {
    Card result = generateEntityWithId(Card.class);
    result.setLast4(faker.numerify("####"));

    return result;
  }

  @Override
  public Card createPhysicalCard(
      com.clearspend.capital.data.model.Card card,
      Address shippingAddress,
      String userExternalRef) {
    Card result = generateEntityWithId(Card.class);
    result.setLast4(faker.numerify("####"));

    return result;
  }

  @Override
  public FinancialAccount createFinancialAccount(
      TypedId<BusinessId> businessId, String accountExternalRef) {
    return generateEntityWithId(FinancialAccount.class);
  }

  @Override
  public Card updateCard(String cardExternalRef, CardStatus cardStatus) {
    return generateEntityWithId(Card.class, cardExternalRef);
  }

  private <T> T generateEntityWithId(Class<T> entityClass) {
    return generateEntityWithId(entityClass, faker.letterify(fakerRandom32SymbolsPattern));
  }

  @SneakyThrows
  private <T> T generateEntityWithId(Class<T> entityClass, String id) {
    T entity = (T) entityClass.getDeclaredConstructors()[0].newInstance();
    ReflectionUtils.findMethod(entityClass, "setId", String.class).invoke(entity, id);

    return entity;
  }

  @Override
  public String getEphemeralKey(String cardId, String nonce) {
    return "dummy_ephemeral_key";
  }

  @Override
  public Account setExternalAccount(String accountId, String btok) {
    return generateEntityWithId(Account.class);
  }

  @Override
  public SetupIntent createSetupIntent(
      String stripeAccountId,
      String bankAccountId,
      String customerAcceptanceIpAddress,
      String customerAcceptanceUserAgent) {
    return generateEntityWithId(SetupIntent.class);
  }

  @SneakyThrows
  @SuppressWarnings("unchecked")
  private <T> T generateEntityWithIdAndStatus(Class<T> entityClass, String status) {
    T entity = (T) entityClass.getDeclaredConstructors()[0].newInstance();
    ReflectionUtils.findMethod(entityClass, "setId", String.class)
        .invoke(entity, faker.letterify(fakerRandom32SymbolsPattern));
    ReflectionUtils.findMethod(entityClass, "setStatus", String.class).invoke(entity, status);

    return entity;
  }

  @Override
  public InboundTransfer executeInboundTransfer(
      TypedId<BusinessId> businessId,
      TypedId<AdjustmentId> adjustmentId,
      TypedId<HoldId> holdId,
      String stripeAccountRef,
      String stripeBankAccountRef,
      String stripeFinancialAccountRef,
      Amount amount,
      String description,
      String statementDescriptor) {
    return generateEntityWithIdAndStatus(InboundTransfer.class, "processing");
  }

  @Override
  public OutboundTransfer executeOutboundTransfer(
      TypedId<BusinessId> businessId,
      String stripeAccountRef,
      String stripeFinancialAccountRef,
      String stripeBankAccountRef,
      Amount amount,
      String description,
      String statementDescriptor) {

    return generateEntityWithIdAndStatus(OutboundTransfer.class, "processing");
  }

  @Override
  public OutboundPayment pushFundsToConnectedFinancialAccount(
      TypedId<BusinessId> businessId,
      String toStripeFinancialAccountRef,
      TypedId<AdjustmentId> adjustmentId,
      Amount amount,
      String description,
      String statementDescriptor) {

    return generateEntityWithIdAndStatus(OutboundPayment.class, "processing");
  }

  @Override
  public OutboundPayment pushFundsToClearspendFinancialAccount(
      TypedId<BusinessId> businessId,
      String fromAccountRef,
      String fromFinancialAccountRef,
      TypedId<AdjustmentId> adjustmentId,
      Amount amount,
      String description,
      String statementDescriptor) {
    return super.pushFundsToClearspendFinancialAccount(
        businessId,
        fromAccountRef,
        fromFinancialAccountRef,
        adjustmentId,
        amount,
        description,
        statementDescriptor);
  }
}
