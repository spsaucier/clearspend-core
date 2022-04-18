package com.clearspend.capital.client.stripe.webhook.controller;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.TestHelper.OnboardBusinessRecord;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.StripeRequirements;
import com.clearspend.capital.data.model.enums.BusinessOnboardingStep;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.clearspend.capital.data.model.enums.KnowYourBusinessStatus;
import com.clearspend.capital.data.repository.AccountRepository;
import com.clearspend.capital.data.repository.AllocationRepository;
import com.clearspend.capital.data.repository.ExpenseCategoryRepository;
import com.clearspend.capital.data.repository.TransactionLimitRepository;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.data.repository.business.BusinessLimitRepository;
import com.clearspend.capital.data.repository.business.BusinessOwnerRepository;
import com.clearspend.capital.data.repository.business.BusinessRepository;
import com.clearspend.capital.data.repository.business.StripeRequirementsRepository;
import com.clearspend.capital.service.BusinessService;
import com.clearspend.capital.service.ServiceHelper;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.stripe.model.Account;
import com.stripe.model.Event;
import java.io.FileReader;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.transaction.TestTransaction;

@Slf4j
@ExtendWith(SpringExtension.class)
class StripeConnectHandler_UpdateAccountTest extends BaseCapitalTest {

  @Autowired private TestHelper testHelper;
  @Autowired private BusinessService businessService;
  @Autowired private StripeConnectHandler stripeConnectHandler;
  @Autowired private EntityManager entityManager;
  @Autowired private StripeRequirementsRepository stripeRequirementsRepository;
  @Autowired private BusinessOwnerRepository businessOwnerRepository;
  @Autowired private ServiceHelper serviceHelper;
  @Autowired private ExpenseCategoryRepository expenseCategoryRepository;
  @Autowired private BusinessRepository businessRepository;
  @Autowired private BusinessLimitRepository businessLimitRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private AllocationRepository allocationRepository;
  @Autowired private AccountRepository accountRepository;
  @Autowired private TransactionLimitRepository transactionLimitRepository;

  private final Resource successOnboarding;
  private final Resource accountAddRepresentative;
  private final Resource addressNotCorrectForPerson;
  private final Resource addressNotCorrectForPersonAndDocumentRequired;
  private final Resource createAccount;
  private final Resource createAccount_secondEventFromStripe;
  private final Resource invalidAccountAddressInvalidPersonAddressRequirePersonDocument;
  private final Resource secondEventAfterSuccessUploadRequiredDocuments;
  private final Resource uploadRequiredDocuments;
  private final Resource ownersAndRepresentativeAndDocumentProvided;
  private final Resource requiredDocumentsForPersonAndSSNLast4;
  private final Resource ownersAndRepresentativeProvided_step2inStripe;
  private final Resource controllerError;
  Gson gson =
      new GsonBuilder()
          .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
          .create();

  StripeConnectHandler_UpdateAccountTest(
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
          Resource ownersAndRepresentativeAndDocumentProvided,
      @Value("classpath:stripeResponses/requiredDocumentsForPersonAndSSNLast4.json") @NonNull
          Resource requiredDocumentsForPersonAndSSNLast4,
      @Value("classpath:stripeResponses/ownersAndRepresentativeProvided_event2fromStripe.json")
          @NonNull
          Resource ownersAndRepresentativeProvided_step2inStripe,
      @Value("classpath:stripeResponses/controllerError.json") @NonNull Resource controllerError) {

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
    this.ownersAndRepresentativeAndDocumentProvided = ownersAndRepresentativeAndDocumentProvided;
    this.ownersAndRepresentativeProvided_step2inStripe =
        ownersAndRepresentativeProvided_step2inStripe;
    this.requiredDocumentsForPersonAndSSNLast4 = requiredDocumentsForPersonAndSSNLast4;
    this.successOnboarding = successOnboarding;
    this.controllerError = controllerError;
  }

  @Test
  @SneakyThrows
  void accountUpdate_firstStepCreateValidAccountForOnboarding_expectedBusinessOwnersStep() {
    OnboardBusinessRecord onboardBusinessRecord = testHelper.onboardBusiness();
    Event event = gson.fromJson(new FileReader(createAccount.getFile()), Event.class);

    event
        .getData()
        .setObject(
            JsonParser.parseString(
                    event
                        .getDataObjectDeserializer()
                        .getRawJson()
                        .replace(
                            event.getAccount(),
                            onboardBusinessRecord.business().getStripeData().getAccountRef()))
                .getAsJsonObject());
    final User user =
        userRepository
            .findById(new TypedId<>(onboardBusinessRecord.businessOwner().getId().toUuid()))
            .orElseThrow(() -> new RuntimeException("Could not find user"));
    testHelper.setCurrentUserAsWebhook(user);
    stripeConnectHandler.accountUpdated(
        event, (Account) event.getDataObjectDeserializer().deserializeUnsafe());

    Business business =
        serviceHelper
            .businessService()
            .getBusiness(onboardBusinessRecord.business().getId())
            .business();

    Assertions.assertEquals(BusinessOnboardingStep.BUSINESS_OWNERS, business.getOnboardingStep());
  }

  @Test
  @SneakyThrows
  void
      accountUpdate_firstStepCreateAccountForOnboardingSecondEventFromStripeValidaAccountCreated_expectedBusinessOwnerStep() {
    OnboardBusinessRecord onboardBusinessRecord = testHelper.onboardBusiness();
    Event event =
        gson.fromJson(new FileReader(createAccount_secondEventFromStripe.getFile()), Event.class);

    event
        .getData()
        .setObject(
            JsonParser.parseString(
                    event
                        .getDataObjectDeserializer()
                        .getRawJson()
                        .replace(
                            event.getAccount(),
                            onboardBusinessRecord.business().getStripeData().getAccountRef()))
                .getAsJsonObject());
    final User user =
        userRepository
            .findById(new TypedId<>(onboardBusinessRecord.businessOwner().getId().toUuid()))
            .orElseThrow(() -> new RuntimeException("Could not find user"));
    testHelper.setCurrentUserAsWebhook(user);
    stripeConnectHandler.accountUpdated(
        event, (Account) event.getDataObjectDeserializer().deserializeUnsafe());

    Business business =
        serviceHelper
            .businessService()
            .getBusiness(onboardBusinessRecord.business().getId())
            .business();

    Assertions.assertEquals(BusinessOnboardingStep.BUSINESS_OWNERS, business.getOnboardingStep());
  }

  @Test
  @SneakyThrows
  void accountUpdate_accountAddRepresentative_expectedBusinessOwnersStep() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Event event = gson.fromJson(new FileReader(accountAddRepresentative.getFile()), Event.class);

    event
        .getData()
        .setObject(
            JsonParser.parseString(
                    event
                        .getDataObjectDeserializer()
                        .getRawJson()
                        .replace(
                            event.getAccount(),
                            createBusinessRecord.business().getStripeData().getAccountRef()))
                .getAsJsonObject());
    testHelper.setCurrentUserAsWebhook(createBusinessRecord.user());
    stripeConnectHandler.accountUpdated(
        event, (Account) event.getDataObjectDeserializer().deserializeUnsafe());

    Business business =
        serviceHelper
            .businessService()
            .getBusiness(createBusinessRecord.business().getId())
            .business();

    Assertions.assertEquals(BusinessOnboardingStep.BUSINESS_OWNERS, business.getOnboardingStep());
  }

  @Test
  @SneakyThrows
  void
      accountUpdate_invalidAccountAddressInvalidPersonAddressRequirePersonDocument_expectedBusinessOwnersStep() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Event event =
        gson.fromJson(
            new FileReader(
                invalidAccountAddressInvalidPersonAddressRequirePersonDocument.getFile()),
            Event.class);

    event
        .getData()
        .setObject(
            JsonParser.parseString(
                    event
                        .getDataObjectDeserializer()
                        .getRawJson()
                        .replace(
                            event.getAccount(),
                            createBusinessRecord.business().getStripeData().getAccountRef()))
                .getAsJsonObject());
    testHelper.setCurrentUserAsWebhook(createBusinessRecord.user());
    stripeConnectHandler.accountUpdated(
        event, (Account) event.getDataObjectDeserializer().deserializeUnsafe());

    Business business =
        serviceHelper
            .businessService()
            .getBusiness(createBusinessRecord.business().getId())
            .business();

    Assertions.assertEquals(BusinessOnboardingStep.BUSINESS, business.getOnboardingStep());
  }

  @Test
  @SneakyThrows
  void accountUpdate_addressNotCorrectForPerson_expectedBusinessOwnersStep() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Event event = gson.fromJson(new FileReader(addressNotCorrectForPerson.getFile()), Event.class);

    event
        .getData()
        .setObject(
            JsonParser.parseString(
                    event
                        .getDataObjectDeserializer()
                        .getRawJson()
                        .replace(
                            event.getAccount(),
                            createBusinessRecord.business().getStripeData().getAccountRef()))
                .getAsJsonObject());
    testHelper.setCurrentUserAsWebhook(createBusinessRecord.user());
    stripeConnectHandler.accountUpdated(
        event, (Account) event.getDataObjectDeserializer().deserializeUnsafe());

    Business business =
        serviceHelper
            .businessService()
            .getBusiness(createBusinessRecord.business().getId())
            .business();

    Assertions.assertEquals(BusinessOnboardingStep.BUSINESS_OWNERS, business.getOnboardingStep());
  }

  @Test
  @SneakyThrows
  void accountUpdate_addressNotCorrectForPersonAndDocumentRequired_expectedBusinessOwnersStep() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Event event =
        gson.fromJson(
            new FileReader(addressNotCorrectForPersonAndDocumentRequired.getFile()), Event.class);

    event
        .getData()
        .setObject(
            JsonParser.parseString(
                    event
                        .getDataObjectDeserializer()
                        .getRawJson()
                        .replace(
                            event.getAccount(),
                            createBusinessRecord.business().getStripeData().getAccountRef()))
                .getAsJsonObject());
    testHelper.setCurrentUserAsWebhook(createBusinessRecord.user());
    stripeConnectHandler.accountUpdated(
        event, (Account) event.getDataObjectDeserializer().deserializeUnsafe());

    Business business =
        serviceHelper
            .businessService()
            .getBusiness(createBusinessRecord.business().getId())
            .business();

    Assertions.assertEquals(BusinessOnboardingStep.BUSINESS_OWNERS, business.getOnboardingStep());
  }

  @Test
  @SneakyThrows
  void
      accountUpdate_withOwnersAndRepresentativeAndSetAccountOwnersProvided_expectedSoftFailRequireDocuments() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Event event =
        gson.fromJson(
            new FileReader(ownersAndRepresentativeAndDocumentProvided.getFile()), Event.class);

    event
        .getData()
        .setObject(
            JsonParser.parseString(
                    event
                        .getDataObjectDeserializer()
                        .getRawJson()
                        .replace(
                            event.getAccount(),
                            createBusinessRecord.business().getStripeData().getAccountRef()))
                .getAsJsonObject());
    testHelper.setCurrentUserAsWebhook(createBusinessRecord.user());
    stripeConnectHandler.accountUpdated(
        event, (Account) event.getDataObjectDeserializer().deserializeUnsafe());

    Business business =
        serviceHelper
            .businessService()
            .getBusiness(createBusinessRecord.business().getId())
            .business();

    Assertions.assertEquals(BusinessOnboardingStep.REVIEW, business.getOnboardingStep());
  }

  @Test
  @SneakyThrows
  void
      accountUpdate_withOwnersAndRepresentativeAndSetAccountOwnersProvidedSecondStepInStripe_expectedReviewStep() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Event event =
        gson.fromJson(
            new FileReader(ownersAndRepresentativeProvided_step2inStripe.getFile()), Event.class);

    event
        .getData()
        .setObject(
            JsonParser.parseString(
                    event
                        .getDataObjectDeserializer()
                        .getRawJson()
                        .replace(
                            event.getAccount(),
                            createBusinessRecord.business().getStripeData().getAccountRef()))
                .getAsJsonObject());
    testHelper.setCurrentUserAsWebhook(createBusinessRecord.user());
    stripeConnectHandler.accountUpdated(
        event, (Account) event.getDataObjectDeserializer().deserializeUnsafe());

    Business business =
        serviceHelper
            .businessService()
            .getBusiness(createBusinessRecord.business().getId())
            .business();

    Assertions.assertEquals(BusinessOnboardingStep.REVIEW, business.getOnboardingStep());
  }

  @Test
  @SneakyThrows
  void accountUpdate_secondEventAfterSuccessUploadRequiredDocuments_expectedStepLinkAccount() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Event event =
        gson.fromJson(
            new FileReader(secondEventAfterSuccessUploadRequiredDocuments.getFile()), Event.class);

    event
        .getData()
        .setObject(
            JsonParser.parseString(
                    event
                        .getDataObjectDeserializer()
                        .getRawJson()
                        .replace(
                            event.getAccount(),
                            createBusinessRecord.business().getStripeData().getAccountRef()))
                .getAsJsonObject());
    testHelper.setCurrentUserAsWebhook(createBusinessRecord.user());
    stripeConnectHandler.accountUpdated(
        event, (Account) event.getDataObjectDeserializer().deserializeUnsafe());

    Business business =
        serviceHelper
            .businessService()
            .getBusiness(createBusinessRecord.business().getId())
            .business();

    Assertions.assertEquals(BusinessOnboardingStep.LINK_ACCOUNT, business.getOnboardingStep());
  }

  @Test
  @SneakyThrows
  void accountUpdate_uploadRequiredDocuments_expectedStepLinkAccount() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Event event = gson.fromJson(new FileReader(uploadRequiredDocuments.getFile()), Event.class);

    event
        .getData()
        .setObject(
            JsonParser.parseString(
                    event
                        .getDataObjectDeserializer()
                        .getRawJson()
                        .replace(
                            event.getAccount(),
                            createBusinessRecord.business().getStripeData().getAccountRef()))
                .getAsJsonObject());
    testHelper.setCurrentUserAsWebhook(createBusinessRecord.user());
    stripeConnectHandler.accountUpdated(
        event, (Account) event.getDataObjectDeserializer().deserializeUnsafe());

    Business business =
        serviceHelper
            .businessService()
            .getBusiness(createBusinessRecord.business().getId())
            .business();

    Assertions.assertEquals(BusinessOnboardingStep.LINK_ACCOUNT, business.getOnboardingStep());
  }

  @Test
  @SneakyThrows
  void accountUpdate_receiveFromStripeRequiredDocAndSSNLast4_expectedStepSoftFail() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Event event =
        gson.fromJson(new FileReader(requiredDocumentsForPersonAndSSNLast4.getFile()), Event.class);

    event
        .getData()
        .setObject(
            JsonParser.parseString(
                    event
                        .getDataObjectDeserializer()
                        .getRawJson()
                        .replace(
                            event.getAccount(),
                            createBusinessRecord.business().getStripeData().getAccountRef()))
                .getAsJsonObject());
    testHelper.setCurrentUserAsWebhook(createBusinessRecord.user());
    stripeConnectHandler.accountUpdated(
        event, (Account) event.getDataObjectDeserializer().deserializeUnsafe());

    Business business =
        serviceHelper
            .businessService()
            .getBusiness(createBusinessRecord.business().getId())
            .business();

    Assertions.assertEquals(BusinessOnboardingStep.SOFT_FAIL, business.getOnboardingStep());
  }

  @Test
  @SneakyThrows
  void accountUpdate_onSuccessValidOnboarding_expectedStepLinkAccount() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Event event = gson.fromJson(new FileReader(successOnboarding.getFile()), Event.class);

    event
        .getData()
        .setObject(
            JsonParser.parseString(
                    event
                        .getDataObjectDeserializer()
                        .getRawJson()
                        .replace(
                            event.getAccount(),
                            createBusinessRecord.business().getStripeData().getAccountRef()))
                .getAsJsonObject());
    testHelper.setCurrentUserAsWebhook(createBusinessRecord.user());
    stripeConnectHandler.accountUpdated(
        event, (Account) event.getDataObjectDeserializer().deserializeUnsafe());

    Business business =
        serviceHelper
            .businessService()
            .getBusiness(createBusinessRecord.business().getId())
            .business();

    Assertions.assertEquals(BusinessOnboardingStep.LINK_ACCOUNT, business.getOnboardingStep());
  }

  @Test
  @SneakyThrows
  void accountUpdate_whenBusinessCompleteOnboarding_shouldNotImpactCurrentStatus() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    businessService.updateBusinessForOnboarding(
        createBusinessRecord.business().getId(),
        BusinessStatus.ACTIVE,
        BusinessOnboardingStep.COMPLETE,
        KnowYourBusinessStatus.PASS);
    Event event = gson.fromJson(new FileReader(controllerError.getFile()), Event.class);

    event
        .getData()
        .setObject(
            JsonParser.parseString(
                    event
                        .getDataObjectDeserializer()
                        .getRawJson()
                        .replace(
                            event.getAccount(),
                            createBusinessRecord.business().getStripeData().getAccountRef()))
                .getAsJsonObject());
    testHelper.setCurrentUserAsWebhook(createBusinessRecord.user());
    stripeConnectHandler.accountUpdated(
        event, (Account) event.getDataObjectDeserializer().deserializeUnsafe());

    Business business =
        serviceHelper
            .businessService()
            .getBusiness(createBusinessRecord.business().getId())
            .business();

    Assertions.assertEquals(BusinessOnboardingStep.COMPLETE, business.getOnboardingStep());
  }

  @Test
  @SneakyThrows
  // test for concurrent stripe account update event
  // this test will leave the database dirty with some data
  void accountUpdate_whenBusinessCompleteOnboarding_concurrentAccess() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness(new TypedId<>());

    Event event1 =
        gson.fromJson(
            new FileReader(addressNotCorrectForPersonAndDocumentRequired.getFile()), Event.class);

    String[] person_s = event1.getDataObjectDeserializer().getRawJson().split("person_");
    String personId = person_s[1].substring(0, person_s[1].indexOf("."));
    createBusinessRecord.businessOwner().setStripePersonReference("person_" + personId);
    businessOwnerRepository.save(createBusinessRecord.businessOwner());

    updateEventJsonObjectForTest(createBusinessRecord, event1, personId);

    Event event2 =
        gson.fromJson(new FileReader(createAccount_secondEventFromStripe.getFile()), Event.class);

    updateEventJsonObjectForTest(createBusinessRecord, event2, personId);

    Event event3 =
        gson.fromJson(
            new FileReader(ownersAndRepresentativeAndDocumentProvided.getFile()), Event.class);

    updateEventJsonObjectForTest(createBusinessRecord, event3, personId);

    Event event4 = gson.fromJson(new FileReader(addressNotCorrectForPerson.getFile()), Event.class);

    updateEventJsonObjectForTest(createBusinessRecord, event4, personId);

    stripeRequirementsRepository.save(
        new StripeRequirements(
            createBusinessRecord.business().getId(),
            gson.fromJson(event4.getDataObjectDeserializer().getRawJson(), Account.class)
                .getRequirements()));

    Event event5 = gson.fromJson(new FileReader(controllerError.getFile()), Event.class);
    updateEventJsonObjectForTest(createBusinessRecord, event2, personId);

    entityManager.flush();
    TestTransaction.flagForCommit();
    TestTransaction.end();
    TestTransaction.start();

    ExecutorService service = Executors.newFixedThreadPool(5);
    service.execute(
        () -> {
          try {
            stripeConnectHandler.accountUpdated(
                event1, (Account) event1.getDataObjectDeserializer().deserializeUnsafe());
            log.info("start event1");
          } catch (Exception e) {
            e.printStackTrace();
          }
        });
    service.execute(
        () -> {
          try {
            stripeConnectHandler.accountUpdated(
                event2, (Account) event2.getDataObjectDeserializer().deserializeUnsafe());
            log.info("start event2");
          } catch (Exception e) {
            e.printStackTrace();
          }
        });
    service.execute(
        () -> {
          try {
            stripeConnectHandler.accountUpdated(
                event3, (Account) event3.getDataObjectDeserializer().deserializeUnsafe());
            log.info("start event3");
          } catch (Exception e) {
            e.printStackTrace();
          }
        });
    service.execute(
        () -> {
          try {
            stripeConnectHandler.accountUpdated(
                event4, (Account) event4.getDataObjectDeserializer().deserializeUnsafe());
            log.info("start event4");
          } catch (Exception e) {
            e.printStackTrace();
          }
        });
    service.execute(
        () -> {
          try {
            stripeConnectHandler.accountUpdated(
                event5, (Account) event5.getDataObjectDeserializer().deserializeUnsafe());
            log.info("start event5");
          } catch (Exception e) {
            e.printStackTrace();
          }
        });

    service.shutdown();
    while (!service.isTerminated()) {}

    // clean database
    allocationRepository.deleteAll();
    businessOwnerRepository.deleteAll();
    businessLimitRepository.deleteAll();
    expenseCategoryRepository.deleteAll();
    userRepository.deleteAll();
    accountRepository.deleteAll();
    transactionLimitRepository.deleteAll();
    stripeRequirementsRepository.deleteAll();
    businessRepository.deleteAll();
    TestTransaction.flagForCommit();
  }

  private void updateEventJsonObjectForTest(
      CreateBusinessRecord createBusinessRecord, Event event, String personId) {
    Set<String> stringSet =
        Arrays.stream(event.getDataObjectDeserializer().getRawJson().split("person_"))
            .skip(1)
            .map(s -> s.substring(0, s.indexOf(".")))
            .collect(Collectors.toSet());

    AtomicReference<String> replace =
        new AtomicReference<>(
            event
                .getDataObjectDeserializer()
                .getRawJson()
                .replace(
                    event.getAccount(),
                    createBusinessRecord.business().getStripeData().getAccountRef()));

    stringSet.forEach(s -> replace.set(replace.get().replaceAll(s, personId)));

    event.getData().setObject(JsonParser.parseString(replace.get()).getAsJsonObject());
  }
}
