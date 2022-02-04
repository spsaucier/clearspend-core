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
import com.clearspend.capital.data.repository.business.BusinessOwnerRepository;
import com.clearspend.capital.data.repository.business.BusinessRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import com.stripe.model.Account;
import com.stripe.model.Account.Requirements;
import com.stripe.model.Account.Requirements.Errors;
import com.stripe.model.Person;
import com.stripe.model.SetupIntent;
import com.stripe.model.issuing.Card;
import com.stripe.model.issuing.Cardholder;
import java.util.List;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@Profile("test")
public class StripeMockClient extends StripeClient {

  private static final String fakerRandom32SymbolsPattern = "????????????????????????????????";
  private static final Faker faker = new Faker();
  @Autowired private BusinessRepository businessRepository;
  @Autowired private BusinessOwnerRepository businessOwnerRepository;

  public StripeMockClient(
      StripeProperties stripeProperties,
      ObjectMapper objectMapper,
      WebClient stripeTreasuryWebClient) {
    super(stripeProperties, objectMapper, stripeTreasuryWebClient);
  }

  @Override
  public Account createAccount(Business business) {
    Account account = generateEntityWithId(Account.class);

    if ("Review".equals(business.getLegalName())) {
      Requirements requirements = new Requirements();
      Errors failed_address_match = new Errors();
      failed_address_match.setCode("verification_failed_address_match");
      requirements.setErrors(List.of(failed_address_match));
      requirements.setPastDue(List.of("verification.document"));
      account.setRequirements(requirements);
    } else if ("Denied".equals(business.getLegalName())) {
      Requirements requirements = new Requirements();
      requirements.setDisabledReason("rejected.other");
      account.setRequirements(requirements);
    }

    return account;
  }

  @Override
  public Cardholder createCardholder(
      User user, ClearAddress billingAddress, String stripeAccountId) {
    return generateEntityWithId(Cardholder.class);
  }

  @Override
  public Person createPerson(BusinessOwner businessOwner, String stripeAccountId) {
    Person person = generateEntityWithId(Person.class);
    if (businessOwner.getTitle() != null) {
      if ("Review".equals(businessOwner.getTitle())) {
        Person.Requirements requirements = new Person.Requirements();
        Errors failed_address_match = new Errors();
        failed_address_match.setCode("verification_document_id_number_mismatch");
        requirements.setErrors(List.of(failed_address_match));
        requirements.setPastDue(List.of("verification.document"));
        person.setRequirements(requirements);
      } else if ("Fraud".equals(businessOwner.getTitle())) {
        Person.Requirements requirements = new Person.Requirements();
        Errors failed_address_match = new Errors();
        failed_address_match.setCode("verification_failed_other");
        requirements.setErrors(List.of(failed_address_match));
        person.setRequirements(requirements);
      }
    }
    return person;
  }

  public Account updateAccount(Account account, BusinessOwner owner) {
    Account account1 = generateEntityWithId(Account.class);
    if (owner.getTitle() != null) {
      if ("Fraud".equals(owner.getTitle())) {
        Requirements requirements = new Requirements();
        requirements.setDisabledReason("rejected.fraud");
        account1.setRequirements(requirements);
      } else if ("Review".equals(owner.getTitle())) {
        Requirements requirements = new Requirements();
        Errors failed_address_match = new Errors();
        failed_address_match.setCode("verification_failed_address_match");
        requirements.setErrors(List.of(failed_address_match));
        requirements.setPastDue(List.of("verification.document"));
        account1.setRequirements(requirements);
      }
    }
    return account1;
  }

  public Account triggerAccountValidationAfterPersonsProvided(
      String stripeAccountId, Boolean ownersProvided, Boolean executiveProvided) {
    Account account1 = generateEntityWithId(Account.class);
    TypedId<BusinessId> id =
        businessRepository.findByStripeAccountReference(stripeAccountId).orElseThrow().getId();
    List<BusinessOwner> businessOwnerByBusinessId = businessOwnerRepository.findByBusinessId(id);
    boolean fraud =
        businessOwnerByBusinessId.stream()
            .anyMatch(businessOwner -> "Fraud".equals(businessOwner.getTitle()));
    boolean review =
        businessOwnerByBusinessId.stream()
            .anyMatch(businessOwner -> "Review".equals(businessOwner.getTitle()));
    if (fraud) {
      Requirements requirements = new Requirements();
      requirements.setDisabledReason("rejected.fraud");
      account1.setRequirements(requirements);
    } else if (review) {
      Requirements requirements = new Requirements();
      Errors failed_address_match = new Errors();
      failed_address_match.setCode("verification_failed_address_match");
      requirements.setErrors(List.of(failed_address_match));
      requirements.setPastDue(List.of("verification.document"));
      account1.setRequirements(requirements);
    }
    return account1;
  }

  public Person createPersonOnboardRepresentative(
      BusinessOwner businessOwner, String businessExternalRef) {
    return generateEntityWithId(Person.class);
  }

  public Person retrievePerson(String businessOwnerExternalRef, String businessExternalRef) {
    return generateEntityWithId(Person.class);
  }

  public Person updatePerson(Person person, BusinessOwner businessOwner) {
    Person person1 = generateEntityWithId(Person.class);
    if (businessOwner.getTitle() != null && "Review".equals(businessOwner.getTitle())) {
      Person.Requirements requirements = new Person.Requirements();
      Errors failed_address_match = new Errors();
      failed_address_match.setCode("verification_document_id_number_mismatch");
      requirements.setErrors(List.of(failed_address_match));
      requirements.setPastDue(List.of("verification.document"));
      person1.setRequirements(requirements);
    }
    return person1;
  }

  public Account retrieveAccount(String stripeAccountId) {
    return generateEntityWithId(Account.class);
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
  public Card updateCard(String stripeCardId, CardStatus cardStatus) {
    return generateEntityWithId(Card.class, stripeCardId);
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
