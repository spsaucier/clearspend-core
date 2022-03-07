package com.clearspend.capital.service;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.TestHelper.OnboardBusinessRecord;
import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.controller.type.review.ApplicationReviewRequirements;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.BusinessOnboardingStep;
import com.clearspend.capital.data.model.enums.BusinessType;
import com.clearspend.capital.data.repository.business.BusinessProspectRepository;
import com.clearspend.capital.data.repository.business.BusinessRepository;
import com.clearspend.capital.service.kyc.BusinessKycStepHandler;
import javax.transaction.Transactional;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
@Transactional
class ApplicationReviewServiceTest extends BaseCapitalTest {

  @Autowired private MockMvc mvc;
  @Autowired private TestHelper testHelper;
  @Autowired private BusinessProspectRepository businessProspectRepository;
  @Autowired private BusinessRepository businessRepository;
  @Autowired private BusinessService businessService;
  @Autowired private ApplicationReviewService applicationReviewService;
  @Autowired private StripeClient stripeClient;
  @Autowired private BusinessKycStepHandler stepHandler;

  @Test
  @SneakyThrows
  void firstStepCreateAccountForOnboarding() {
    OnboardBusinessRecord onboardBusinessRecord = testHelper.onboardBusiness();
    Business business1 = onboardBusinessRecord.business();
    business1.setLegalName("createAccount");
    businessRepository.save(business1);
    businessRepository.flush();

    ApplicationReviewRequirements stripeApplicationRequirements =
        applicationReviewService.getStripeApplicationRequirements(
            onboardBusinessRecord.business().getId());

    Business business =
        businessService.retrieveBusiness(onboardBusinessRecord.business().getId(), true);

    Assertions.assertEquals(BusinessOnboardingStep.BUSINESS_OWNERS, business.getOnboardingStep());
    Assertions.assertTrue(stripeApplicationRequirements.getKybRequiredFields().isEmpty());
    Assertions.assertTrue(stripeApplicationRequirements.getKycRequiredFields().isEmpty());
    Assertions.assertTrue(stripeApplicationRequirements.getKycRequiredDocuments().isEmpty());
    Assertions.assertTrue(stripeApplicationRequirements.getKybRequiredDocuments().isEmpty());
    Assertions.assertTrue(stripeApplicationRequirements.getRequireOwner());
    Assertions.assertTrue(stripeApplicationRequirements.getRequireRepresentative());
  }

  @Test
  @SneakyThrows
  void firstStepCreateAccountForOnboardingSecondEventFromStripe() {
    OnboardBusinessRecord onboardBusinessRecord = testHelper.onboardBusiness();
    Business business1 = onboardBusinessRecord.business();
    business1.setLegalName("createAccount_secondEventFromStripe");
    businessRepository.save(business1);
    businessRepository.flush();

    ApplicationReviewRequirements stripeApplicationRequirements =
        applicationReviewService.getStripeApplicationRequirements(
            onboardBusinessRecord.business().getId());

    Business business =
        businessService.retrieveBusiness(onboardBusinessRecord.business().getId(), true);

    Assertions.assertEquals(BusinessOnboardingStep.BUSINESS_OWNERS, business.getOnboardingStep());
    Assertions.assertTrue(stripeApplicationRequirements.getKybRequiredFields().isEmpty());
    Assertions.assertTrue(stripeApplicationRequirements.getKycRequiredFields().isEmpty());
    Assertions.assertTrue(stripeApplicationRequirements.getKycRequiredDocuments().isEmpty());
    Assertions.assertTrue(stripeApplicationRequirements.getKybRequiredDocuments().isEmpty());
    Assertions.assertTrue(stripeApplicationRequirements.getRequireOwner());
    Assertions.assertTrue(stripeApplicationRequirements.getRequireRepresentative());
  }

  @Test
  @SneakyThrows
  void accountAddRepresentative() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business1 = createBusinessRecord.business();
    business1.setLegalName("accountAddRepresentative");
    businessRepository.save(business1);
    businessRepository.flush();

    ApplicationReviewRequirements stripeApplicationRequirements =
        applicationReviewService.getStripeApplicationRequirements(
            createBusinessRecord.business().getId());

    Business business =
        businessService.retrieveBusiness(createBusinessRecord.business().getId(), true);

    Assertions.assertEquals(BusinessOnboardingStep.BUSINESS_OWNERS, business.getOnboardingStep());
    Assertions.assertTrue(stripeApplicationRequirements.getKybRequiredDocuments().isEmpty());
    Assertions.assertEquals(1, stripeApplicationRequirements.getKycRequiredDocuments().size());
    Assertions.assertTrue(stripeApplicationRequirements.getKybRequiredFields().isEmpty());
    Assertions.assertTrue(stripeApplicationRequirements.getKycRequiredFields().isEmpty());
    Assertions.assertTrue(stripeApplicationRequirements.getRequireOwner());
    Assertions.assertFalse(stripeApplicationRequirements.getRequireRepresentative());
  }

  @Test
  @SneakyThrows
  void invalidAccountAddressInvalidPersonAddressRequirePersonDocument() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business1 = createBusinessRecord.business();
    business1.setLegalName("invalidAccountAddressInvalidPersonAddressRequirePersonDocument");
    businessRepository.save(business1);
    businessRepository.flush();
    stepHandler.execute(
        business1, stripeClient.retrieveCompleteAccount(business1.getStripeData().getAccountRef()));
    ApplicationReviewRequirements stripeApplicationRequirements =
        applicationReviewService.getStripeApplicationRequirements(
            createBusinessRecord.business().getId());

    Business business =
        businessService.retrieveBusiness(createBusinessRecord.business().getId(), true);

    Assertions.assertEquals(BusinessOnboardingStep.BUSINESS, business.getOnboardingStep());
    Assertions.assertTrue(stripeApplicationRequirements.getKybRequiredDocuments().isEmpty());
    Assertions.assertEquals(1, stripeApplicationRequirements.getKycRequiredDocuments().size());
    Assertions.assertEquals(1, stripeApplicationRequirements.getKybRequiredFields().size());
    Assertions.assertEquals(1, stripeApplicationRequirements.getKycRequiredFields().size());
  }

  @Test
  @SneakyThrows
  void addressNotCorrectForPerson() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business1 = createBusinessRecord.business();
    business1.setLegalName("addressNotCorrectForPerson");
    businessRepository.save(business1);
    businessRepository.flush();

    ApplicationReviewRequirements stripeApplicationRequirements =
        applicationReviewService.getStripeApplicationRequirements(
            createBusinessRecord.business().getId());

    Business business =
        businessService.retrieveBusiness(createBusinessRecord.business().getId(), true);

    Assertions.assertEquals(BusinessOnboardingStep.BUSINESS_OWNERS, business.getOnboardingStep());
    Assertions.assertTrue(stripeApplicationRequirements.getKybRequiredDocuments().isEmpty());
    Assertions.assertTrue(stripeApplicationRequirements.getKybRequiredFields().isEmpty());
    Assertions.assertTrue(stripeApplicationRequirements.getKycRequiredDocuments().isEmpty());
    Assertions.assertTrue(stripeApplicationRequirements.getKycRequiredFields().size() > 0);
  }

  @Test
  @SneakyThrows
  void addressNotCorrectForPersonAndDocumentRequired() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business1 = createBusinessRecord.business();
    business1.setLegalName("addressNotCorrectForPersonAndDocumentRequired");
    businessRepository.save(business1);
    businessRepository.flush();

    ApplicationReviewRequirements stripeApplicationRequirements =
        applicationReviewService.getStripeApplicationRequirements(
            createBusinessRecord.business().getId());

    Business business =
        businessService.retrieveBusiness(createBusinessRecord.business().getId(), true);

    Assertions.assertEquals(BusinessOnboardingStep.BUSINESS_OWNERS, business.getOnboardingStep());
    Assertions.assertTrue(stripeApplicationRequirements.getKybRequiredDocuments().isEmpty());
    Assertions.assertTrue(stripeApplicationRequirements.getKybRequiredFields().isEmpty());
    Assertions.assertTrue(stripeApplicationRequirements.getKycRequiredDocuments().size() > 0);
    Assertions.assertTrue(stripeApplicationRequirements.getKycRequiredFields().size() > 0);
  }

  @Test
  @SneakyThrows
  void ownersAndRepresentativeProvided() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business1 = createBusinessRecord.business();
    business1.setLegalName("ownersAndRepresentativeProvided");
    businessRepository.save(business1);
    businessRepository.flush();
    stepHandler.execute(
        business1, stripeClient.retrieveCompleteAccount(business1.getStripeData().getAccountRef()));
    ApplicationReviewRequirements stripeApplicationRequirements =
        applicationReviewService.getStripeApplicationRequirements(
            createBusinessRecord.business().getId());

    Business business =
        businessService.retrieveBusiness(createBusinessRecord.business().getId(), true);

    Assertions.assertEquals(BusinessOnboardingStep.REVIEW, business.getOnboardingStep());
    Assertions.assertTrue(stripeApplicationRequirements.getKybRequiredDocuments().isEmpty());
    Assertions.assertTrue(stripeApplicationRequirements.getKybRequiredFields().isEmpty());
    Assertions.assertTrue(stripeApplicationRequirements.getKycRequiredDocuments().isEmpty());
    Assertions.assertTrue(stripeApplicationRequirements.getKycRequiredFields().isEmpty());
    Assertions.assertTrue(stripeApplicationRequirements.getPendingVerification().size() > 0);
  }

  @Test
  @SneakyThrows
  void ownersAndRepresentativeProvidedSecondStepInStripe() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business1 = createBusinessRecord.business();
    business1.setLegalName("ownersAndRepresentativeProvided_event2fromStripe");
    businessRepository.save(business1);
    businessRepository.flush();
    stepHandler.execute(
        business1, stripeClient.retrieveCompleteAccount(business1.getStripeData().getAccountRef()));
    ApplicationReviewRequirements stripeApplicationRequirements =
        applicationReviewService.getStripeApplicationRequirements(
            createBusinessRecord.business().getId());

    Business business =
        businessService.retrieveBusiness(createBusinessRecord.business().getId(), true);

    Assertions.assertEquals(BusinessOnboardingStep.REVIEW, business.getOnboardingStep());
    Assertions.assertTrue(stripeApplicationRequirements.getKybRequiredDocuments().isEmpty());
    Assertions.assertTrue(stripeApplicationRequirements.getKybRequiredFields().isEmpty());
    Assertions.assertTrue(stripeApplicationRequirements.getKycRequiredDocuments().isEmpty());
    Assertions.assertTrue(stripeApplicationRequirements.getKycRequiredFields().isEmpty());
  }

  @Test
  @SneakyThrows
  void onSuccessValidOnboarding() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business1 = createBusinessRecord.business();
    business1.setLegalName("successOnboarding");
    businessRepository.save(business1);
    businessRepository.flush();
    stepHandler.execute(
        business1, stripeClient.retrieveCompleteAccount(business1.getStripeData().getAccountRef()));
    ApplicationReviewRequirements stripeApplicationRequirements =
        applicationReviewService.getStripeApplicationRequirements(business1.getId());

    Business business = businessService.retrieveBusiness(business1.getId(), true);

    Assertions.assertEquals(BusinessOnboardingStep.LINK_ACCOUNT, business.getOnboardingStep());
    Assertions.assertTrue(stripeApplicationRequirements.getKybRequiredDocuments().isEmpty());
    Assertions.assertTrue(stripeApplicationRequirements.getKycRequiredDocuments().isEmpty());
    Assertions.assertTrue(stripeApplicationRequirements.getKybRequiredFields().isEmpty());
    Assertions.assertTrue(stripeApplicationRequirements.getKycRequiredFields().isEmpty());
  }

  @Test
  @SneakyThrows
  void ownerRepresentativeAditionaCompanyAndSettingDetailsRequired() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business1 = createBusinessRecord.business();
    business1.setLegalName("ownerRepresentativeAditionaCompanyAndSettingDetailsRequired");
    businessRepository.save(business1);
    businessRepository.flush();
    stepHandler.execute(
        business1, stripeClient.retrieveCompleteAccount(business1.getStripeData().getAccountRef()));
    ApplicationReviewRequirements stripeApplicationRequirements =
        applicationReviewService.getStripeApplicationRequirements(business1.getId());

    Business business = businessService.retrieveBusiness(business1.getId(), true);

    Assertions.assertEquals(BusinessOnboardingStep.BUSINESS, business.getOnboardingStep());
    Assertions.assertTrue(stripeApplicationRequirements.getKybRequiredDocuments().isEmpty());
    Assertions.assertTrue(stripeApplicationRequirements.getKycRequiredDocuments().isEmpty());
    Assertions.assertFalse(stripeApplicationRequirements.getKybRequiredFields().isEmpty());
    Assertions.assertFalse(stripeApplicationRequirements.getKycRequiredFields().isEmpty());
    Assertions.assertFalse(stripeApplicationRequirements.getRequireRepresentative());
    Assertions.assertTrue(stripeApplicationRequirements.getRequireOwner());
  }

  @Test
  @SneakyThrows
  void individualDetailsRequired() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    Business business1 = createBusinessRecord.business();
    business1.setType(BusinessType.INDIVIDUAL);
    business1.setLegalName("individualDetailsRequired");
    businessRepository.save(business1);
    businessRepository.flush();

    ApplicationReviewRequirements stripeApplicationRequirements =
        applicationReviewService.getStripeApplicationRequirements(business1.getId());

    Business business = businessService.retrieveBusiness(business1.getId(), true);

    Assertions.assertEquals(BusinessOnboardingStep.BUSINESS_OWNERS, business.getOnboardingStep());
    Assertions.assertTrue(stripeApplicationRequirements.getKybRequiredDocuments().isEmpty());
    Assertions.assertTrue(stripeApplicationRequirements.getKycRequiredDocuments().isEmpty());
    Assertions.assertTrue(stripeApplicationRequirements.getKybRequiredFields().isEmpty());
    Assertions.assertTrue(stripeApplicationRequirements.getKycRequiredFields().isEmpty());
    Assertions.assertFalse(stripeApplicationRequirements.getRequireRepresentative());
    Assertions.assertTrue(stripeApplicationRequirements.getRequireOwner());
  }
}
