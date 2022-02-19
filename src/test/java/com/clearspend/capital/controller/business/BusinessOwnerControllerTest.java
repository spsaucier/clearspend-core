package com.clearspend.capital.controller.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.OnboardBusinessRecord;
import com.clearspend.capital.client.stripe.webhook.controller.StripeConnectHandler;
import com.clearspend.capital.common.data.model.Address;
import com.clearspend.capital.controller.type.business.owner.CreateBusinessOwnerResponse;
import com.clearspend.capital.controller.type.business.owner.CreateOrUpdateBusinessOwnerRequest;
import com.clearspend.capital.crypto.data.model.embedded.EncryptedString;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.business.BusinessProspect;
import com.clearspend.capital.data.model.enums.BusinessOnboardingStep;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.clearspend.capital.data.model.enums.Country;
import com.clearspend.capital.data.model.enums.KnowYourCustomerStatus;
import com.clearspend.capital.service.BusinessOwnerService;
import com.clearspend.capital.service.BusinessService;
import com.stripe.model.Account;
import com.stripe.model.Account.Requirements;
import com.stripe.model.Account.Requirements.Errors;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import javax.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
@Slf4j
@Transactional
class BusinessOwnerControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;

  private final BusinessService businessService;
  private final BusinessOwnerService businessOwnerService;
  private final StripeConnectHandler stripeConnectHandler;

  OnboardBusinessRecord onboardBusinessRecord;

  Address address =
      new Address(
          new EncryptedString("13810 Shavano Wind"),
          new EncryptedString("San Antonio, Texas(TX), 78230"),
          "San Antonio",
          "Texas",
          new EncryptedString("78230"),
          Country.USA);

  @BeforeEach
  void init() throws Exception {
    if (onboardBusinessRecord == null) {
      onboardBusinessRecord = testHelper.onboardBusiness();
    }
  }

  @SneakyThrows
  @Test
  void createBusinessOwner_success() {
    BusinessOwner businessOwner = onboardBusinessRecord.businessOwner();

    List<CreateOrUpdateBusinessOwnerRequest> request =
        List.of(
            new CreateOrUpdateBusinessOwnerRequest(
                null,
                testHelper.generateFirstName(),
                testHelper.generateLastName(),
                businessOwner.getRelationshipOwner(),
                businessOwner.getRelationshipRepresentative(),
                businessOwner.getRelationshipExecutive(),
                businessOwner.getRelationshipDirector(),
                BigDecimal.valueOf(40),
                "CEO",
                LocalDate.of(1900, 1, 1),
                testHelper.generateTaxIdentificationNumber(),
                testHelper.generateEmail(),
                testHelper.generatePhone(),
                new com.clearspend.capital.controller.type.Address(address),
                true));

    String body = objectMapper.writeValueAsString(request);
    MockHttpServletResponse response =
        mvc.perform(
                post("/business-owners")
                    .contentType("application/json")
                    .content(body)
                    .cookie(onboardBusinessRecord.cookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    List<CreateBusinessOwnerResponse> createBusinessOwnerResponse =
        objectMapper.readValue(
            response.getContentAsString(),
            objectMapper
                .getTypeFactory()
                .constructParametricType(List.class, CreateBusinessOwnerResponse.class));

    Assertions.assertNull(createBusinessOwnerResponse.get(0).getErrorMessage());
  }

  @SneakyThrows
  @Test
  void updateBusinessOwner_success() {
    BusinessProspect businessProspect = onboardBusinessRecord.businessProspect();
    CreateOrUpdateBusinessOwnerRequest request =
        new CreateOrUpdateBusinessOwnerRequest(
            onboardBusinessRecord.businessOwner().getId(),
            businessProspect.getFirstName().getEncrypted(),
            businessProspect.getLastName().getEncrypted(),
            null,
            null,
            null,
            null,
            null,
            null,
            testHelper.generateDateOfBirth(),
            testHelper.generateTaxIdentificationNumber(),
            businessProspect.getEmail().getEncrypted(),
            null,
            testHelper.generateApiAddress(),
            true);
    request.setOnboarding(true);
    String body = objectMapper.writeValueAsString(request);

    mvc.perform(
            patch("/business-owners")
                .contentType("application/json")
                .content(body)
                .cookie(onboardBusinessRecord.cookie()))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();

    BusinessOwner businessOwner =
        businessOwnerService.retrieveBusinessOwner(onboardBusinessRecord.businessOwner().getId());
    Assertions.assertEquals(KnowYourCustomerStatus.PASS, businessOwner.getKnowYourCustomerStatus());
    Assertions.assertNotNull(businessOwner.getStripePersonReference());
  }

  @SneakyThrows
  @Test
  void updateBusinessOwner_ToHardFail() {
    Business business = onboardBusinessRecord.business();
    BusinessOwner businessOwner = onboardBusinessRecord.businessOwner();

    List<CreateOrUpdateBusinessOwnerRequest> request =
        List.of(
            new CreateOrUpdateBusinessOwnerRequest(
                businessOwner.getId(),
                businessOwner.getFirstName().getEncrypted(),
                businessOwner.getLastName().getEncrypted(),
                businessOwner.getRelationshipOwner(),
                businessOwner.getRelationshipRepresentative(),
                businessOwner.getRelationshipExecutive(),
                businessOwner.getRelationshipDirector(),
                BigDecimal.valueOf(50),
                "Fraud",
                LocalDate.of(1900, 1, 1),
                testHelper.generateTaxIdentificationNumber(),
                businessOwner.getEmail().getEncrypted(),
                businessOwner.getPhone().getEncrypted(),
                new com.clearspend.capital.controller.type.Address(address),
                true));

    String body = objectMapper.writeValueAsString(request);
    mvc.perform(
            post("/business-owners")
                .contentType("application/json")
                .content(body)
                .cookie(onboardBusinessRecord.cookie()))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();

    Account account1 = new Account();
    account1.setId(business.getStripeData().getAccountRef());
    Requirements requirements = new Requirements();
    requirements.setDisabledReason("rejected.fraud");
    account1.setRequirements(requirements);
    stripeConnectHandler.accountUpdated(account1);

    businessOwnerService.retrieveBusinessOwner(businessOwner.getId());
    Business businessResponse = businessService.retrieveBusiness(business.getId(), true);
    assertThat(businessResponse.getOnboardingStep())
        .isEqualTo(BusinessOnboardingStep.BUSINESS_OWNERS);
    assertThat(businessResponse.getStatus()).isEqualTo(BusinessStatus.CLOSED);
  }

  @SneakyThrows
  @Test
  void updateBusinessOwner_ToSoftFail() {
    Business business = onboardBusinessRecord.business();
    BusinessOwner businessOwner = onboardBusinessRecord.businessOwner();

    List<CreateOrUpdateBusinessOwnerRequest> request =
        List.of(
            new CreateOrUpdateBusinessOwnerRequest(
                businessOwner.getId(),
                businessOwner.getFirstName().getEncrypted(),
                businessOwner.getLastName().getEncrypted(),
                businessOwner.getRelationshipOwner(),
                businessOwner.getRelationshipRepresentative(),
                businessOwner.getRelationshipExecutive(),
                businessOwner.getRelationshipDirector(),
                BigDecimal.valueOf(40),
                "Review",
                LocalDate.of(1900, 1, 1),
                testHelper.generateTaxIdentificationNumber(),
                businessOwner.getEmail().getEncrypted(),
                businessOwner.getPhone().getEncrypted(),
                new com.clearspend.capital.controller.type.Address(address),
                true));

    String body = objectMapper.writeValueAsString(request);
    mvc.perform(
            post("/business-owners")
                .contentType("application/json")
                .content(body)
                .cookie(onboardBusinessRecord.cookie()))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();

    Account account1 = new Account();
    account1.setId(business.getStripeData().getAccountRef());
    Requirements requirements = new Requirements();
    Errors failed_address_match = new Errors();
    failed_address_match.setCode("verification_failed_address_match");
    requirements.setErrors(List.of(failed_address_match));
    requirements.setPastDue(List.of("verification.document"));
    account1.setRequirements(requirements);
    stripeConnectHandler.accountUpdated(account1);

    BusinessOwner owner = businessOwnerService.retrieveBusinessOwner(businessOwner.getId());
    Business businessResponse = businessService.retrieveBusiness(business.getId(), true);
    assertThat(businessResponse.getOnboardingStep()).isEqualTo(BusinessOnboardingStep.SOFT_FAIL);
    assertThat(businessResponse.getStatus()).isEqualTo(BusinessStatus.ONBOARDING);
    assertThat(owner.getKnowYourCustomerStatus()).isEqualTo(KnowYourCustomerStatus.REVIEW);
  }
}
