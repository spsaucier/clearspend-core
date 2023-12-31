package com.clearspend.capital.client.stripe;

import com.clearspend.capital.client.stripe.types.FinancialAccount;
import com.clearspend.capital.client.stripe.types.FinancialAccountAbaAddress;
import com.clearspend.capital.client.stripe.types.FinancialAccountAddress;
import com.clearspend.capital.client.stripe.types.FinancialAccountBalance;
import com.clearspend.capital.client.stripe.types.InboundTransfer;
import com.clearspend.capital.client.stripe.types.OutboundPayment;
import com.clearspend.capital.client.stripe.types.OutboundTransfer;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.model.ClearAddress;
import com.clearspend.capital.common.typedid.data.AdjustmentId;
import com.clearspend.capital.common.typedid.data.HoldId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessBankAccountId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.ReplacementReason;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.card.CardStatus;
import com.clearspend.capital.data.model.enums.card.CardStatusReason;
import com.clearspend.capital.data.repository.business.BusinessOwnerRepository;
import com.clearspend.capital.data.repository.business.BusinessRepository;
import com.clearspend.capital.service.type.NetworkCommon;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.stripe.model.Account;
import com.stripe.model.Account.Requirements;
import com.stripe.model.Account.Requirements.Errors;
import com.stripe.model.BankAccount;
import com.stripe.model.EphemeralKey;
import com.stripe.model.Event;
import com.stripe.model.ExternalAccountCollection;
import com.stripe.model.File;
import com.stripe.model.Person;
import com.stripe.model.SetupIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.issuing.Authorization;
import com.stripe.model.issuing.Card;
import com.stripe.model.issuing.Cardholder;
import com.stripe.net.ApiResource;
import com.stripe.param.AccountUpdateParams;
import com.stripe.param.FileCreateParams.Purpose;
import com.stripe.param.PersonUpdateParams.Verification.Document;
import com.stripe.param.issuing.CardUpdateParams.CancellationReason;
import java.io.FileReader;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.NonNull;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

@SuppressWarnings("StringSplitter")
@Slf4j
@Component
@Profile("test")
public class StripeMockClient extends StripeClient {

  private static final String PERSON = "person";

  private static final String fakerRandom32SymbolsPattern = "????????????????????????????????";
  private static final Faker faker = new Faker();

  @Autowired private BusinessRepository businessRepository;
  @Autowired private BusinessOwnerRepository businessOwnerRepository;

  private final ConcurrentMap<String, Object> createdObjects = new ConcurrentHashMap<>();

  private final Resource successOnboarding;
  private final Resource accountAddRepresentative;
  private final Resource addressNotCorrectForPerson;
  private final Resource addressNotCorrectForPersonAndDocumentRequired;
  private final Resource createAccount;
  private final Resource createAccount_secondEventFromStripe;
  private final Resource invalidAccountAddressInvalidPersonAddressRequirePersonDocument;
  private final Resource secondEventAfterSuccessUploadRequiredDocuments;
  private final Resource uploadRequiredDocuments;
  private final Resource ownersAndRepresentativeProvided;
  private final Resource ownersAndRepresentativeProvided_event2fromStripe;
  private final Resource requiredDocumentsForPersonAndSSNLast4;
  private final Resource ownerRepresentativeAditionaCompanyAndSettingDetailsRequired;
  private final Resource personRelationShipTitleRequired;
  private final Resource documentVerificationForTwoPersons;
  private final Resource individualDetailsRequired;
  private final Resource ivalidAddressPoBoxesDisallowed;
  private final Resource LLC_ownersRequired;
  private final List<MockAuthorization> mockAuthorizations =
      Collections.synchronizedList(new ArrayList<>());

  @Setter private Amount clearspendFinancialAccountBalance = Amount.of(Currency.USD);

  public StripeMockClient(
      StripeProperties stripeProperties,
      ObjectMapper objectMapper,
      WebClient stripeTreasuryWebClient,
      @Value("classpath:stripeResponses/successOnboarding.json") @NonNull
          Resource successOnboarding,
      @Value("classpath:stripeResponses/accountAddRepresentative.json") @NonNull
          Resource accountAddRepresentative,
      @Value("classpath:stripeResponses/addressNotCorrectForPerson.json") @NonNull
          Resource addressNotCorrectForPerson,
      @Value("classpath:stripeResponses/addressNotCorrectForPersonAndDocumentRequired.json")
          @NonNull
          Resource addressNotCorrectForPersonAndDocumentRequired,
      @Value("classpath:stripeResponses/createAccount.json") @NonNull Resource createAccount,
      @Value("classpath:stripeResponses/createAccount_secondEventFromStripe.json") @NonNull
          Resource createAccount_secondEventFromStripe,
      @Value(
              "classpath:stripeResponses/invalidAccountAddressInvalidPersonAddressRequirePersonDocument.json")
          @NonNull
          Resource invalidAccountAddressInvalidPersonAddressRequirePersonDocument,
      @Value("classpath:stripeResponses/secondEventAfterSuccessUploadRequiredDocuments.json")
          @NonNull
          Resource secondEventAfterSuccessUploadRequiredDocuments,
      @Value("classpath:stripeResponses/uploadRequiredDocuments.json") @NonNull
          Resource uploadRequiredDocuments,
      @Value("classpath:stripeResponses/ownersAndRepresentativeProvided.json") @NonNull
          Resource ownersAndRepresentativeProvided,
      @Value("classpath:stripeResponses/ownersAndRepresentativeProvided_event2fromStripe.json")
          @NonNull
          Resource ownersAndRepresentativeProvided_event2fromStripe,
      @Value("classpath:stripeResponses/requiredDocumentsForPersonAndSSNLast4.json") @NonNull
          Resource requiredDocumentsForPersonAndSSNLast4,
      @Value("classpath:stripeResponses/personRelationShipTitleRequired.json") @NonNull
          Resource personRelationShipTitleRequired,
      @Value("classpath:stripeResponses/documentVerificationForTwoPersons.json") @NonNull
          Resource documentVerificationForTwoPersons,
      @Value("classpath:stripeResponses/individualDetailsRequired.json") @NonNull
          Resource individualDetailsRequired,
      @Value("classpath:stripeResponses/ivalidAddressPoBoxesDisallowed.json") @NonNull
          Resource ivalidAddressPoBoxesDisallowed,
      @Value("classpath:stripeResponses/LLC_ownersRequired.json") @NonNull
          Resource LLC_ownersRequired,
      @Value(
              "classpath:stripeResponses/ownerRepresentativeAditionaCompanyAndSettingDetailsRequired.json")
          @NonNull
          Resource ownerRepresentativeAditionaCompanyAndSettingDetailsRequired) {
    super(stripeProperties, objectMapper, stripeTreasuryWebClient);

    this.createAccount = createAccount;
    this.createAccount_secondEventFromStripe = createAccount_secondEventFromStripe;
    this.uploadRequiredDocuments = uploadRequiredDocuments;
    this.secondEventAfterSuccessUploadRequiredDocuments =
        secondEventAfterSuccessUploadRequiredDocuments;
    this.accountAddRepresentative = accountAddRepresentative;
    this.addressNotCorrectForPerson = addressNotCorrectForPerson;
    this.addressNotCorrectForPersonAndDocumentRequired =
        addressNotCorrectForPersonAndDocumentRequired;
    this.invalidAccountAddressInvalidPersonAddressRequirePersonDocument =
        invalidAccountAddressInvalidPersonAddressRequirePersonDocument;
    this.ownersAndRepresentativeProvided = ownersAndRepresentativeProvided;
    this.ownersAndRepresentativeProvided_event2fromStripe =
        ownersAndRepresentativeProvided_event2fromStripe;
    this.requiredDocumentsForPersonAndSSNLast4 = requiredDocumentsForPersonAndSSNLast4;
    this.successOnboarding = successOnboarding;
    this.ownerRepresentativeAditionaCompanyAndSettingDetailsRequired =
        ownerRepresentativeAditionaCompanyAndSettingDetailsRequired;
    this.personRelationShipTitleRequired = personRelationShipTitleRequired;
    this.documentVerificationForTwoPersons = documentVerificationForTwoPersons;
    this.individualDetailsRequired = individualDetailsRequired;
    this.ivalidAddressPoBoxesDisallowed = ivalidAddressPoBoxesDisallowed;
    this.LLC_ownersRequired = LLC_ownersRequired;
  }

  public List<MockAuthorization> getMockAuthorizations() {
    return mockAuthorizations;
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
  public Cardholder createIndividualCardholder(
      User user, ClearAddress billingAddress, String stripeAccountId) {
    return generateEntityWithId(Cardholder.class);
  }

  @Override
  public Cardholder createCompanyCardholder(Business business, TypedId<UserId> ownerId) {
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

  @Override
  public Cardholder updateIndividualCardholder(User user) {
    return generateEntityWithId(Cardholder.class, user.getExternalRef());
  }

  @Override
  public Cardholder updateCompanyCardholder(final Business business) {
    final Cardholder cardholder =
        generateEntityWithId(Cardholder.class, business.getCardholderExternalRef());
    cardholder.setStatus(businessStatusToCardholderStatus(business.getStatus()).getValue());
    return cardholder;
  }

  @Override
  public Person updatePerson(BusinessOwner businessOwner, String stripeAccountId) {
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

  @Override
  public Account triggerAccountValidationAfterPersonsProvided(
      String stripeAccountId, Boolean ownersProvided, Boolean executiveProvided) {
    Account account1 = generateEntityWithId(Account.class);
    TypedId<BusinessId> id =
        businessRepository.findByStripeAccountRef(stripeAccountId).orElseThrow().getId();
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

  @Override
  public Person retrievePerson(String businessOwnerExternalRef, String businessExternalRef) {
    return generateEntityWithId(Person.class);
  }

  public com.clearspend.capital.client.stripe.types.Account retrieveCompleteAccount(
      String stripeAccountId) {
    Optional<Business> byStripeAccountReference =
        businessRepository.findByStripeAccountRef(stripeAccountId);
    if (byStripeAccountReference.isPresent()) {
      Business business = byStripeAccountReference.get();
      String legalName = business.getLegalName();
      switch (legalName) {
        case "createAccount" -> {
          return getCompleteAccountFromJson(createAccount, business);
        }
        case "createAccount_secondEventFromStripe" -> {
          return getCompleteAccountFromJson(createAccount_secondEventFromStripe, business);
        }
        case "uploadRequiredDocuments" -> {
          return getCompleteAccountFromJson(uploadRequiredDocuments, business);
        }
        case "secondEventAfterSuccessUploadRequiredDocuments" -> {
          return getCompleteAccountFromJson(
              secondEventAfterSuccessUploadRequiredDocuments, business);
        }
        case "accountAddRepresentative" -> {
          return getCompleteAccountFromJson(accountAddRepresentative, business);
        }
        case "addressNotCorrectForPerson" -> {
          return getCompleteAccountFromJson(addressNotCorrectForPerson, business);
        }
        case "addressNotCorrectForPersonAndDocumentRequired" -> {
          return getCompleteAccountFromJson(
              addressNotCorrectForPersonAndDocumentRequired, business);
        }
        case "invalidAccountAddressInvalidPersonAddressRequirePersonDocument" -> {
          return getCompleteAccountFromJson(
              invalidAccountAddressInvalidPersonAddressRequirePersonDocument, business);
        }
        case "ownersAndRepresentativeProvided" -> {
          return getCompleteAccountFromJson(ownersAndRepresentativeProvided, business);
        }
        case "ownersAndRepresentativeProvided_event2fromStripe" -> {
          return getCompleteAccountFromJson(
              ownersAndRepresentativeProvided_event2fromStripe, business);
        }
        case "requiredDocumentsForPersonAndSSNLast4" -> {
          return getCompleteAccountFromJson(requiredDocumentsForPersonAndSSNLast4, business);
        }
        case "successOnboarding" -> {
          return getCompleteAccountFromJson(successOnboarding, business);
        }
        case "personRelationShipTitleRequired" -> {
          return getCompleteAccountFromJson(personRelationShipTitleRequired, business);
        }
        case "documentVerificationForTwoPersons" -> {
          return getCompleteAccountFromJson(documentVerificationForTwoPersons, business);
        }
        case "individualDetailsRequired" -> {
          return getCompleteAccountFromJson(individualDetailsRequired, business);
        }
        case "ivalidAddressPoBoxesDisallowed" -> {
          return getCompleteAccountFromJson(ivalidAddressPoBoxesDisallowed, business);
        }
        case "LLC_ownersRequired" -> {
          return getCompleteAccountFromJson(LLC_ownersRequired, business);
        }
        case "ownerRepresentativeAditionaCompanyAndSettingDetailsRequired" -> {
          return getCompleteAccountFromJson(
              ownerRepresentativeAditionaCompanyAndSettingDetailsRequired, business);
        }
        case "Review" -> {
          com.clearspend.capital.client.stripe.types.Account account =
              new com.clearspend.capital.client.stripe.types.Account();
          Requirements requirements = new Requirements();
          Errors failed_address_match = new Errors();
          failed_address_match.setCode("verification_failed_address_match");
          requirements.setErrors(List.of(failed_address_match));
          requirements.setPastDue(List.of("verification.document"));
          account.setRequirements(requirements);
          return account;
        }
        case "Denied" -> {
          com.clearspend.capital.client.stripe.types.Account account =
              new com.clearspend.capital.client.stripe.types.Account();
          Requirements requirements = new Requirements();
          requirements.setDisabledReason("rejected.other");
          account.setRequirements(requirements);
          return account;
        }
      }
    }
    return generateEntityWithId(com.clearspend.capital.client.stripe.types.Account.class);
  }

  public Account retrieveAccount(String stripeAccountId) {
    Optional<Business> byStripeAccountReference =
        businessRepository.findByStripeAccountRef(stripeAccountId);
    if (byStripeAccountReference.isPresent()) {
      Business business = byStripeAccountReference.get();
      String legalName = business.getLegalName();
      switch (legalName) {
        case "createAccount" -> {
          return getAccountFromJson(createAccount, business);
        }
        case "createAccount_secondEventFromStripe" -> {
          return getAccountFromJson(createAccount_secondEventFromStripe, business);
        }
        case "uploadRequiredDocuments" -> {
          return getAccountFromJson(uploadRequiredDocuments, business);
        }
        case "secondEventAfterSuccessUploadRequiredDocuments" -> {
          return getAccountFromJson(secondEventAfterSuccessUploadRequiredDocuments, business);
        }
        case "accountAddRepresentative" -> {
          return getAccountFromJson(accountAddRepresentative, business);
        }
        case "addressNotCorrectForPerson" -> {
          return getAccountFromJson(addressNotCorrectForPerson, business);
        }
        case "addressNotCorrectForPersonAndDocumentRequired" -> {
          return getAccountFromJson(addressNotCorrectForPersonAndDocumentRequired, business);
        }
        case "invalidAccountAddressInvalidPersonAddressRequirePersonDocument" -> {
          return getAccountFromJson(
              invalidAccountAddressInvalidPersonAddressRequirePersonDocument, business);
        }
        case "ownersAndRepresentativeProvided" -> {
          return getAccountFromJson(ownersAndRepresentativeProvided, business);
        }
        case "ownersAndRepresentativeProvided_event2fromStripe" -> {
          return getAccountFromJson(ownersAndRepresentativeProvided_event2fromStripe, business);
        }
        case "requiredDocumentsForPersonAndSSNLast4" -> {
          return getAccountFromJson(requiredDocumentsForPersonAndSSNLast4, business);
        }
        case "successOnboarding" -> {
          return getAccountFromJson(successOnboarding, business);
        }
        case "personRelationShipTitleRequired" -> {
          return getAccountFromJson(personRelationShipTitleRequired, business);
        }
        case "documentVerificationForTwoPersons" -> {
          return getAccountFromJson(documentVerificationForTwoPersons, business);
        }
        case "individualDetailsRequired" -> {
          return getAccountFromJson(individualDetailsRequired, business);
        }
        case "ivalidAddressPoBoxesDisallowed" -> {
          return getAccountFromJson(ivalidAddressPoBoxesDisallowed, business);
        }
        case "LLC_ownersRequired" -> {
          return getAccountFromJson(LLC_ownersRequired, business);
        }
        case "ownerRepresentativeAditionaCompanyAndSettingDetailsRequired" -> {
          return getAccountFromJson(
              ownerRepresentativeAditionaCompanyAndSettingDetailsRequired, business);
        }
        case "Review" -> {
          Account account = new Account();
          Requirements requirements = new Requirements();
          Errors failed_address_match = new Errors();
          failed_address_match.setCode("verification_failed_address_match");
          requirements.setErrors(List.of(failed_address_match));
          requirements.setPastDue(List.of("verification.document"));
          account.setRequirements(requirements);
          return account;
        }
        case "Denied" -> {
          Account account = new Account();
          Requirements requirements = new Requirements();
          requirements.setDisabledReason("rejected.other");
          account.setRequirements(requirements);
          return account;
        }
      }
    }
    return generateEntityWithId(Account.class);
  }

  @Override
  public Card createVirtualCard(final CreateCardConfig config) {
    final Card result = generateEntityWithId(Card.class);
    populateFields(config, result);
    return result;
  }

  private void populateFields(final CreateCardConfig config, final Card card) {
    final Cardholder cardholder = new Cardholder();
    cardholder.setId(config.getStripeUserRef());
    card.setCardholder(cardholder);
    card.setLast4(faker.numerify("####"));
    card.setReplacementFor(config.getReplacementFor());
    card.setReplacementReason(
        Optional.ofNullable(config.getReplacementReason())
            .map(ReplacementReason::getValue)
            .orElse(null));
  }

  @Override
  public Card createPhysicalCard(final CreatePhysicalCardConfig config) {
    final Card result = generateEntityWithId(Card.class);
    populateFields(config, result);
    return result;
  }

  @Override
  public FinancialAccount createFinancialAccount(
      TypedId<BusinessId> businessId, String accountExternalRef) {
    return generateEntityWithId(FinancialAccount.class);
  }

  @Override
  public Card updateCard(
      @NonNull final String stripeCardId,
      @NonNull final CardStatus cardStatus,
      @NonNull final CardStatusReason reason) {
    final Card card = generateEntityWithId(Card.class, stripeCardId);
    card.setStatus(getCardStatus(cardStatus).name());
    card.setCancellationReason(
        Optional.ofNullable(getCancellationReason(cardStatus, reason))
            .map(CancellationReason::getValue)
            .orElse(null));
    createdObjects.put(stripeCardId, card);
    return card;
  }

  @Nullable
  public Object getCreatedObject(@NonNull final String id) {
    return createdObjects.get(id);
  }

  private <T> T generateEntityWithId(Class<T> entityClass) {
    return generateEntityWithId(entityClass, faker.letterify(fakerRandom32SymbolsPattern));
  }

  @SneakyThrows
  @SuppressWarnings("unchecked")
  private <T> T generateEntityWithId(Class<T> entityClass, String id) {
    T entity = (T) entityClass.getDeclaredConstructors()[0].newInstance();
    ReflectionUtils.findMethod(entityClass, "setId", String.class).invoke(entity, id);
    createdObjects.put(id, entity);

    return entity;
  }

  @Override
  public String getEphemeralKey(String cardId, String nonce) {
    return "dummy_ephemeral_key";
  }

  @Override
  public EphemeralKey getEphemeralKeyObjectForCard(String cardId, String apiVersion) {
    EphemeralKey ephemeralKey = new EphemeralKey();
    ephemeralKey.setRawJson("true");
    return ephemeralKey;
  }

  @Override
  public Account setExternalAccount(String accountId, String btok) {
    Account account = generateEntityWithId(Account.class);

    ExternalAccountCollection externalAccounts = new ExternalAccountCollection();
    account.setExternalAccounts(externalAccounts);

    BankAccount bankAccount = new BankAccount();
    bankAccount.setId(faker.letterify("????????????????"));
    bankAccount.setDefaultForCurrency(true);
    externalAccounts.setData(List.of(bankAccount));

    return account;
  }

  @Override
  public void deleteExternalAccount(String accountId, String externalAccountId) {}

  @Override
  public FinancialAccount getFinancialAccount(
      TypedId<BusinessId> businessId, String stripeAccountRef, String stripeFinancialAccountRef) {
    FinancialAccount financialAccount = new FinancialAccount();
    financialAccount.setId(stripeFinancialAccountRef);

    FinancialAccountAddress financialAccountAddress = new FinancialAccountAddress();
    financialAccountAddress.setType("aba");
    financialAccountAddress.setAbaAddress(
        new FinancialAccountAbaAddress(
            "2323",
            faker.numerify("##########2323"),
            faker.numerify("##############"),
            faker.name().name()));

    financialAccount.setFinancialAddresses(List.of(financialAccountAddress));

    return financialAccount;
  }

  @Override
  public SetupIntent createSetupIntent(
      String stripeAccountId,
      String bankAccountId,
      String customerAcceptanceIpAddress,
      String customerAcceptanceUserAgent) {
    SetupIntent setupIntent = generateEntityWithId(SetupIntent.class);
    setupIntent.setStatus("succeeded");

    return setupIntent;
  }

  @SneakyThrows
  private <T> T generateEntityWithIdAndStatus(Class<T> entityClass, String status) {
    T entity = generateEntityWithId(entityClass);
    ReflectionUtils.findMethod(entityClass, "setStatus", String.class).invoke(entity, status);

    return entity;
  }

  @Override
  public InboundTransfer executeInboundTransfer(
      TypedId<BusinessId> businessId,
      TypedId<BusinessBankAccountId> businessBankAccountId,
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
      TypedId<BusinessBankAccountId> businessBankAccountId,
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
    return generateEntityWithId(OutboundPayment.class);
  }

  @SneakyThrows
  private Account getAccountFromJson(Resource resource, Business business) {
    Account account;
    try {
      Event event = ApiResource.GSON.fromJson(new FileReader(resource.getFile()), Event.class);
      StripeObject stripeObject = event.getDataObjectDeserializer().deserializeUnsafe();
      account = (Account) stripeObject;
    } catch (Exception e) {
      account = ApiResource.GSON.fromJson(new FileReader(resource.getFile()), Account.class);
    }

    Requirements accountRequirements = account.getRequirements();
    if (accountRequirements == null) {
      return account;
    }
    Set<String> accountRequiredFields = new HashSet<>();
    accountRequiredFields.addAll(accountRequirements.getCurrentlyDue());
    accountRequiredFields.addAll(accountRequirements.getEventuallyDue());
    accountRequiredFields.addAll(accountRequirements.getPastDue());
    accountRequiredFields.addAll(accountRequirements.getPendingVerification());

    Set<String> personList =
        accountRequiredFields.stream()
            .filter(accountRequiredField -> accountRequiredField.startsWith(PERSON))
            .map(accountRequiredField -> accountRequiredField.split("\\.")[0])
            .collect(Collectors.toSet());

    List<BusinessOwner> byBusinessId = businessOwnerRepository.findByBusinessId(business.getId());
    AtomicInteger i = new AtomicInteger();
    personList.forEach(
        personL -> {
          if (byBusinessId.size() > i.get()) {
            BusinessOwner businessOwner = byBusinessId.get(i.getAndIncrement());
            businessOwner.setStripePersonReference(personL);
            businessOwnerRepository.save(businessOwner);
          }
        });

    return account;
  }

  @SneakyThrows
  private com.clearspend.capital.client.stripe.types.Account getCompleteAccountFromJson(
      Resource resource, Business business) {
    Gson gson =
        new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();
    com.clearspend.capital.client.stripe.types.Account account;
    try {
      Event event = gson.fromJson(new FileReader(resource.getFile()), Event.class);
      account =
          gson.fromJson(
              event.getDataObjectDeserializer().getRawJson(),
              com.clearspend.capital.client.stripe.types.Account.class);
    } catch (Exception e) {
      account =
          gson.fromJson(
              new FileReader(resource.getFile()),
              com.clearspend.capital.client.stripe.types.Account.class);
    }

    Requirements accountRequirements = account.getRequirements();
    if (accountRequirements == null) {
      return account;
    }
    Set<String> accountRequiredFields = new HashSet<>();
    accountRequiredFields.addAll(accountRequirements.getCurrentlyDue());
    accountRequiredFields.addAll(accountRequirements.getEventuallyDue());
    accountRequiredFields.addAll(accountRequirements.getPastDue());
    accountRequiredFields.addAll(accountRequirements.getPendingVerification());

    Set<String> personList =
        accountRequiredFields.stream()
            .filter(accountRequiredField -> accountRequiredField.startsWith(PERSON))
            .map(accountRequiredField -> accountRequiredField.split("\\.")[0])
            .collect(Collectors.toSet());

    List<BusinessOwner> byBusinessId = businessOwnerRepository.findByBusinessId(business.getId());
    AtomicInteger i = new AtomicInteger();
    personList.forEach(
        personL -> {
          if (byBusinessId.size() > i.get()) {
            BusinessOwner businessOwner = byBusinessId.get(i.getAndIncrement());
            businessOwner.setStripePersonReference(personL);
            businessOwnerRepository.save(businessOwner);
          }
        });

    return account;
  }

  public File uploadFile(MultipartFile file, Purpose purpose, String accountId) {
    File file1 = new File();
    file1.setId("id");
    return file1;
  }

  public File uploadFile(InputStream inputStream, Purpose purpose, String accountId) {
    File file1 = new File();
    file1.setId("id");
    return file1;
  }

  public Account updateAccountDocument(
      String accountId, AccountUpdateParams.Company.Verification.Document document) {
    return new Account();
  }

  @Override
  public Account updateAccount(final Business business) {
    return new Account();
  }

  public Person updatePersonDocuments(String personId, String accountId, Document document) {
    return new Person();
  }

  public void reset() {
    createdObjects.clear();
    mockAuthorizations.clear();
    clearspendFinancialAccountBalance = Amount.of(Currency.USD);
  }

  public long countCreatedObjectsByType(Class<?> clazz) {
    return createdObjects.values().stream()
        .filter(v -> clazz.isAssignableFrom(v.getClass()))
        .count();
  }

  @Override
  public void declineAuthorization(Authorization authorization, NetworkCommon networkCommon) {
    log.debug(
        "Stripe authorization {} for {} declined in {} for {}",
        authorization.getId(),
        networkCommon.getRequestedAmount(),
        networkCommon.getNetworkMessage() != null
            ? networkCommon.getNetworkMessage().getId()
            : "n/a",
        networkCommon.getRequestedAmount());
    mockAuthorizations.add(
        new MockAuthorization(MockAuthorizationStatus.DECLINED, authorization, networkCommon));
  }

  @Override
  public void approveAuthorization(Authorization authorization, NetworkCommon networkCommon) {
    authorization.setApproved(true);
    log.debug(
        "Stripe authorization {} for {} approved in {} for {}",
        authorization.getId(),
        networkCommon.getRequestedAmount(),
        networkCommon.getNetworkMessage().getId(),
        // amounts going back to Stripe for authorizations should be positive
        networkCommon.getRequestedAmount().abs());
    mockAuthorizations.add(
        new MockAuthorization(MockAuthorizationStatus.APPROVED, authorization, networkCommon));
  }

  @Override
  public Account updateAccountTosAcceptance(Business business) {
    return new Account();
  }

  public enum MockAuthorizationStatus {
    APPROVED,
    DECLINED
  }

  public record MockAuthorization(
      MockAuthorizationStatus status, Authorization authorization, NetworkCommon networkCommon) {}

  @Override
  public FinancialAccount getClearspendFinancialAccount() {
    FinancialAccount financialAccount =
        getFinancialAccount(new TypedId<>(), "stripeAccountId", "stripeFinancialAccount");

    FinancialAccountBalance balance = new FinancialAccountBalance();
    balance.setCash(
        Map.of(
            clearspendFinancialAccountBalance.getCurrency().toStripeCurrency(),
            clearspendFinancialAccountBalance.toStripeAmount()));

    financialAccount.setBalance(balance);

    return financialAccount;
  }
}
