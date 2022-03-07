package com.clearspend.capital.client.stripe.webhook.controller;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.TestHelper.OnboardBusinessRecord;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.BusinessOnboardingStep;
import com.clearspend.capital.service.BusinessService;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.stripe.model.Event;
import java.io.FileReader;
import javax.transaction.Transactional;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;

@Slf4j
@Transactional
class StripeConnectHandler_UpdateAccountTest extends BaseCapitalTest {

  @Autowired private TestHelper testHelper;
  @Autowired private BusinessService businessService;
  @Autowired private StripeConnectHandler stripeConnectHandler;

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
          Resource ownersAndRepresentativeProvided_step2inStripe) {

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
    stripeConnectHandler.accountUpdated(event);

    Business business =
        businessService.retrieveBusiness(onboardBusinessRecord.business().getId(), true);

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
    stripeConnectHandler.accountUpdated(event);

    Business business =
        businessService.retrieveBusiness(onboardBusinessRecord.business().getId(), true);

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
    stripeConnectHandler.accountUpdated(event);

    Business business =
        businessService.retrieveBusiness(createBusinessRecord.business().getId(), true);

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
    stripeConnectHandler.accountUpdated(event);

    Business business =
        businessService.retrieveBusiness(createBusinessRecord.business().getId(), true);

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
    stripeConnectHandler.accountUpdated(event);

    Business business =
        businessService.retrieveBusiness(createBusinessRecord.business().getId(), true);

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
    stripeConnectHandler.accountUpdated(event);

    Business business =
        businessService.retrieveBusiness(createBusinessRecord.business().getId(), true);

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
    stripeConnectHandler.accountUpdated(event);

    Business business =
        businessService.retrieveBusiness(createBusinessRecord.business().getId(), true);

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
    stripeConnectHandler.accountUpdated(event);

    Business business =
        businessService.retrieveBusiness(createBusinessRecord.business().getId(), true);

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
    stripeConnectHandler.accountUpdated(event);

    Business business =
        businessService.retrieveBusiness(createBusinessRecord.business().getId(), true);

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
    stripeConnectHandler.accountUpdated(event);

    Business business =
        businessService.retrieveBusiness(createBusinessRecord.business().getId(), true);

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
    stripeConnectHandler.accountUpdated(event);

    Business business =
        businessService.retrieveBusiness(createBusinessRecord.business().getId(), true);

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
    stripeConnectHandler.accountUpdated(event);

    Business business =
        businessService.retrieveBusiness(createBusinessRecord.business().getId(), true);

    Assertions.assertEquals(BusinessOnboardingStep.LINK_ACCOUNT, business.getOnboardingStep());
  }
}
