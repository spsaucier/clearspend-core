package com.clearspend.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.OnboardBusinessRecord;
import com.clearspend.capital.controller.type.business.owner.CreateBusinessOwnerResponse;
import com.clearspend.capital.controller.type.business.owner.CreateOrUpdateBusinessOwnerRequest;
import com.clearspend.capital.data.model.Business;
import com.clearspend.capital.data.model.BusinessOwner;
import com.clearspend.capital.data.model.BusinessProspect;
import com.clearspend.capital.data.model.enums.BusinessOnboardingStep;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.clearspend.capital.data.model.enums.KnowYourCustomerStatus;
import com.clearspend.capital.service.BusinessOwnerService;
import com.clearspend.capital.service.BusinessService;
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

  OnboardBusinessRecord onboardBusinessRecord;

  @BeforeEach
  void init() throws Exception {
    if (onboardBusinessRecord == null) {
      mockServerHelper.expectOtpViaEmail();
      mockServerHelper.expectOtpViaSms();
      mockServerHelper.expectEmailVerification("123456");
      mockServerHelper.expectPhoneVerification("123456");

      onboardBusinessRecord = testHelper.onboardBusiness();
    }
  }

  @SneakyThrows
  @Test
  void createBusinessOwner_success() {
    CreateOrUpdateBusinessOwnerRequest request =
        new CreateOrUpdateBusinessOwnerRequest(
            testHelper.generateFirstName(),
            testHelper.generateLastName(),
            testHelper.generateDateOfBirth(),
            testHelper.generateTaxIdentificationNumber(),
            testHelper.generateEmail(),
            testHelper.generateApiAddress());

    request.setOnboarding(true);

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

    CreateBusinessOwnerResponse createBusinessOwnerResponse =
        objectMapper.readValue(response.getContentAsString(), CreateBusinessOwnerResponse.class);

    Assertions.assertNull(createBusinessOwnerResponse.getErrorMessage());
  }

  @SneakyThrows
  @Test
  void updateBusinessOwner_success() {
    BusinessProspect businessProspect = onboardBusinessRecord.businessProspect();
    CreateOrUpdateBusinessOwnerRequest request =
        new CreateOrUpdateBusinessOwnerRequest(
            businessProspect.getFirstName().getEncrypted(),
            businessProspect.getLastName().getEncrypted(),
            testHelper.generateDateOfBirth(),
            testHelper.generateTaxIdentificationNumber(),
            businessProspect.getEmail().getEncrypted(),
            testHelper.generateApiAddress());
    request.setOnboarding(true);
    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                patch(
                        String.format(
                            "/business-owners/%s", onboardBusinessRecord.businessOwner().getId()))
                    .contentType("application/json")
                    .content(body)
                    .cookie(onboardBusinessRecord.cookie()))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
  }

  @SneakyThrows
  @Test
  void updateBusinessOwner_ToHardFail() {
    BusinessProspect businessProspect = onboardBusinessRecord.businessProspect();
    CreateOrUpdateBusinessOwnerRequest request =
        new CreateOrUpdateBusinessOwnerRequest(
            businessProspect.getFirstName().getEncrypted(),
            "Denied",
            testHelper.generateDateOfBirth(),
            testHelper.generateTaxIdentificationNumber(),
            businessProspect.getEmail().getEncrypted(),
            testHelper.generateApiAddress());
    request.setOnboarding(true);

    String body = objectMapper.writeValueAsString(request);
    mvc.perform(
            patch(
                    String.format(
                        "/business-owners/%s", onboardBusinessRecord.businessOwner().getId()))
                .contentType("application/json")
                .content(body)
                .cookie(onboardBusinessRecord.cookie()))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();

    BusinessOwner owner =
        businessOwnerService.retrieveBusinessOwner(onboardBusinessRecord.businessOwner().getId());
    Business business = businessService.retrieveBusiness(onboardBusinessRecord.business().getId());
    assertThat(business.getOnboardingStep()).isEqualTo(BusinessOnboardingStep.BUSINESS_OWNERS);
    assertThat(business.getStatus()).isEqualTo(BusinessStatus.CLOSED);
    assertThat(owner.getKnowYourCustomerStatus()).isEqualTo(KnowYourCustomerStatus.FAIL);
  }

  @SneakyThrows
  @Test
  void updateBusinessOwner_ToSoftFail() {
    BusinessProspect businessProspect = onboardBusinessRecord.businessProspect();
    CreateOrUpdateBusinessOwnerRequest request =
        new CreateOrUpdateBusinessOwnerRequest(
            businessProspect.getFirstName().getEncrypted(),
            "Review",
            testHelper.generateDateOfBirth(),
            testHelper.generateTaxIdentificationNumber(),
            businessProspect.getEmail().getEncrypted(),
            testHelper.generateApiAddress());
    request.setOnboarding(true);
    String body = objectMapper.writeValueAsString(request);
    mvc.perform(
            patch(
                    String.format(
                        "/business-owners/%s", onboardBusinessRecord.businessOwner().getId()))
                .contentType("application/json")
                .content(body)
                .cookie(onboardBusinessRecord.cookie()))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();

    BusinessOwner owner =
        businessOwnerService.retrieveBusinessOwner(onboardBusinessRecord.businessOwner().getId());
    Business business = businessService.retrieveBusiness(onboardBusinessRecord.business().getId());
    assertThat(business.getOnboardingStep()).isEqualTo(BusinessOnboardingStep.SOFT_FAIL);
    assertThat(business.getStatus()).isEqualTo(BusinessStatus.ONBOARDING);
    assertThat(owner.getKnowYourCustomerStatus()).isEqualTo(KnowYourCustomerStatus.REVIEW);
  }

  @SneakyThrows
  @Test
  void updateBusinessOwner_ToHardFail_fromAdditionalOwnerReview() {
    CreateOrUpdateBusinessOwnerRequest request =
        new CreateOrUpdateBusinessOwnerRequest(
            testHelper.generateFirstName(),
            "Denied",
            testHelper.generateDateOfBirth(),
            testHelper.generateTaxIdentificationNumber(),
            testHelper.generateEmail(),
            testHelper.generateApiAddress());
    request.setOnboarding(true);

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
    CreateBusinessOwnerResponse createBusinessOwnerResponse =
        objectMapper.readValue(response.getContentAsString(), CreateBusinessOwnerResponse.class);

    BusinessProspect businessProspect = onboardBusinessRecord.businessProspect();
    CreateOrUpdateBusinessOwnerRequest request2 =
        new CreateOrUpdateBusinessOwnerRequest(
            businessProspect.getFirstName().getEncrypted(),
            businessProspect.getLastName().getEncrypted(),
            testHelper.generateDateOfBirth(),
            testHelper.generateTaxIdentificationNumber(),
            businessProspect.getEmail().getEncrypted(),
            testHelper.generateApiAddress());
    request2.setOnboarding(true);

    String body2 = objectMapper.writeValueAsString(request2);
    mvc.perform(
            patch(
                    String.format(
                        "/business-owners/%s", onboardBusinessRecord.businessOwner().getId()))
                .contentType("application/json")
                .content(body2)
                .cookie(onboardBusinessRecord.cookie()))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();

    BusinessOwner owner =
        businessOwnerService.retrieveBusinessOwner(onboardBusinessRecord.businessOwner().getId());
    Business business = businessService.retrieveBusiness(onboardBusinessRecord.business().getId());
    BusinessOwner additionalOwner =
        businessOwnerService.retrieveBusinessOwner(
            createBusinessOwnerResponse.getBusinessOwnerId());
    assertThat(additionalOwner.getKnowYourCustomerStatus()).isEqualTo(KnowYourCustomerStatus.FAIL);
    assertThat(business.getOnboardingStep()).isEqualTo(BusinessOnboardingStep.BUSINESS_OWNERS);
    assertThat(business.getStatus()).isEqualTo(BusinessStatus.CLOSED);
    assertThat(owner.getKnowYourCustomerStatus()).isEqualTo(KnowYourCustomerStatus.PASS);
  }

  @SneakyThrows
  @Test
  void updateBusinessOwner_ToSoftFail_fromAdditionalOwner() {
    CreateOrUpdateBusinessOwnerRequest request =
        new CreateOrUpdateBusinessOwnerRequest(
            testHelper.generateFirstName(),
            "Review",
            testHelper.generateDateOfBirth(),
            testHelper.generateTaxIdentificationNumber(),
            testHelper.generateEmail(),
            testHelper.generateApiAddress());
    request.setOnboarding(true);

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
    CreateBusinessOwnerResponse createBusinessOwnerResponse =
        objectMapper.readValue(response.getContentAsString(), CreateBusinessOwnerResponse.class);

    BusinessProspect businessProspect = onboardBusinessRecord.businessProspect();
    CreateOrUpdateBusinessOwnerRequest request2 =
        new CreateOrUpdateBusinessOwnerRequest(
            businessProspect.getFirstName().getEncrypted(),
            businessProspect.getLastName().getEncrypted(),
            testHelper.generateDateOfBirth(),
            testHelper.generateTaxIdentificationNumber(),
            businessProspect.getEmail().getEncrypted(),
            testHelper.generateApiAddress());
    request2.setOnboarding(true);
    String body2 = objectMapper.writeValueAsString(request2);
    mvc.perform(
            patch(
                    String.format(
                        "/business-owners/%s", onboardBusinessRecord.businessOwner().getId()))
                .contentType("application/json")
                .content(body2)
                .cookie(onboardBusinessRecord.cookie()))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();

    BusinessOwner owner =
        businessOwnerService.retrieveBusinessOwner(onboardBusinessRecord.businessOwner().getId());
    Business business = businessService.retrieveBusiness(onboardBusinessRecord.business().getId());
    BusinessOwner additionalOwner =
        businessOwnerService.retrieveBusinessOwner(
            createBusinessOwnerResponse.getBusinessOwnerId());
    assertThat(additionalOwner.getKnowYourCustomerStatus())
        .isEqualTo(KnowYourCustomerStatus.REVIEW);
    assertThat(business.getOnboardingStep()).isEqualTo(BusinessOnboardingStep.SOFT_FAIL);
    assertThat(business.getStatus()).isEqualTo(BusinessStatus.ONBOARDING);
    assertThat(owner.getKnowYourCustomerStatus()).isEqualTo(KnowYourCustomerStatus.PASS);
  }
}
