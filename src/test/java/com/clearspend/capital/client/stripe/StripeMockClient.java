package com.clearspend.capital.client.stripe;

import com.clearspend.capital.common.data.model.Address;
import com.clearspend.capital.common.data.model.ClearAddress;
import com.clearspend.capital.data.model.Business;
import com.clearspend.capital.data.model.BusinessOwner;
import com.clearspend.capital.data.model.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import com.stripe.model.Account;
import com.stripe.model.Person;
import com.stripe.model.issuing.Card;
import com.stripe.model.issuing.Cardholder;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;

@Component
@Profile("test")
public class StripeMockClient extends StripeClient {

  private static final String fakerRandom32SymbolsPattern = "????????????????????????????????";
  private static final Faker faker = new Faker();

  public StripeMockClient(ObjectMapper objectMapper, StripeProperties stripeProperties) {
    super(stripeProperties, objectMapper);
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

  @SneakyThrows
  @SuppressWarnings("unchecked")
  private <T> T generateEntityWithId(Class<T> entityClass) {
    T entity = (T) entityClass.getDeclaredConstructors()[0].newInstance();
    ReflectionUtils.findMethod(entityClass, "setId", String.class)
        .invoke(entity, faker.letterify(fakerRandom32SymbolsPattern));

    return entity;
  }
}
