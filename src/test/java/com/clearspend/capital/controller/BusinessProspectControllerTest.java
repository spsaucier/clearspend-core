package com.clearspend.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.common.typedid.data.BusinessProspectId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.business.prospect.BusinessProspectStatus;
import com.clearspend.capital.controller.type.business.prospect.ConvertBusinessProspectResponse;
import com.clearspend.capital.controller.type.business.prospect.SetBusinessProspectPasswordRequest;
import com.clearspend.capital.controller.type.business.prospect.ValidateBusinessProspectIdentifierRequest.IdentifierType;
import com.clearspend.capital.crypto.PasswordUtil;
import com.clearspend.capital.data.model.Business;
import com.clearspend.capital.data.model.BusinessProspect;
import com.clearspend.capital.data.model.enums.BusinessOnboardingStep;
import com.clearspend.capital.data.repository.BusinessProspectRepository;
import com.clearspend.capital.data.repository.BusinessRepository;
import com.clearspend.capital.service.FusionAuthService;
import io.fusionauth.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
class BusinessProspectControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;

  private final FusionAuthService fusionAuthService;
  private final BusinessProspectRepository businessProspectRepository;
  private final BusinessRepository businessRepository;

  @Test
  void createBusinessProspect_success() throws Exception {
    // given
    mockServerHelper.expectOtpViaEmail();

    // when
    BusinessProspect businessProspect = testHelper.createBusinessProspect();

    // then
    BusinessProspect dbRecord =
        businessProspectRepository.findById(businessProspect.getId()).orElseThrow();
    log.info("{}", dbRecord);
    assertThat(dbRecord).isEqualTo(businessProspect);
    assertThat(dbRecord.isEmailVerified()).isFalse();
    mockServerHelper.verifyEmailVerificationCalled(1);
  }

  @Test
  void validateBusinessProspectEmail_success() throws Exception {
    // given
    mockServerHelper.expectOtpViaEmail();
    mockServerHelper.expectEmailVerification("123456");

    // when
    BusinessProspect businessProspect = testHelper.createBusinessProspect();
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.EMAIL, businessProspect.getId(), "123456");

    // then
    BusinessProspect dbRecord =
        businessProspectRepository.findById(businessProspect.getId()).orElseThrow();
    assertThat(dbRecord.isEmailVerified()).isTrue();
    mockServerHelper.verifyEmailVerificationCalled(1);

    testHelper.testBusinessProspectState(
        dbRecord.getEmail().getEncrypted(), BusinessProspectStatus.EMAIL_VERIFIED);
  }

  @Test
  void setBusinessProspectPhone_success() throws Exception {
    // given
    mockServerHelper.expectOtpViaEmail();
    mockServerHelper.expectOtpViaSms();
    mockServerHelper.expectEmailVerification("234567890");
    mockServerHelper.expectPhoneVerification("766255906");

    // when
    BusinessProspect businessProspect = testHelper.createBusinessProspect();
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.EMAIL, businessProspect.getId(), "234567890");

    // then
    BusinessProspect dbRecord =
        businessProspectRepository.findById(businessProspect.getId()).orElseThrow();
    assertThat(dbRecord.isEmailVerified()).isTrue();
    assertThat(dbRecord.isPhoneVerified()).isFalse();
    mockServerHelper.verifyEmailVerificationCalled(1);

    testHelper.testBusinessProspectState(
        dbRecord.getEmail().getEncrypted(), BusinessProspectStatus.EMAIL_VERIFIED);
  }

  @Test
  void validateBusinessProspectPhone_success() throws Exception {
    // given
    mockServerHelper.expectOtpViaEmail();
    mockServerHelper.expectOtpViaSms();
    mockServerHelper.expectEmailVerification("777888999");
    mockServerHelper.expectPhoneVerification("766255906");

    // when
    BusinessProspect businessProspect = testHelper.createBusinessProspect();
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.EMAIL, businessProspect.getId(), "777888999");

    // then
    BusinessProspect dbRecord =
        businessProspectRepository.findById(businessProspect.getId()).orElseThrow();
    assertThat(dbRecord.isEmailVerified()).isTrue();
    assertThat(dbRecord.isPhoneVerified()).isFalse();

    // when
    testHelper.setBusinessProspectPhone(businessProspect.getId());
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.PHONE, businessProspect.getId(), "766255906");

    // then
    dbRecord = businessProspectRepository.findById(businessProspect.getId()).orElseThrow();
    assertThat(dbRecord.isEmailVerified()).isTrue();
    assertThat(dbRecord.isPhoneVerified()).isTrue();
    mockServerHelper.verifyEmailVerificationCalled(1);
    mockServerHelper.verifyPhoneVerificationCalled(1);

    testHelper.testBusinessProspectState(
        dbRecord.getEmail().getEncrypted(), BusinessProspectStatus.MOBILE_VERIFIED);
  }

  String setBusinessProspectPassword(TypedId<BusinessProspectId> businessProspectId)
      throws Exception {
    String password = PasswordUtil.generatePassword();
    SetBusinessProspectPasswordRequest request = new SetBusinessProspectPasswordRequest(password);
    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                post(String.format("/business-prospects/%s/password", businessProspectId))
                    .contentType("application/json")
                    .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
    return password;
  }

  @Test
  void setBusinessProspectPassword_success() throws Exception {
    // given
    mockServerHelper.expectOtpViaEmail();
    mockServerHelper.expectOtpViaSms();
    mockServerHelper.expectEmailVerification("777888999");
    mockServerHelper.expectPhoneVerification("766255906");

    // when
    BusinessProspect businessProspect = testHelper.createBusinessProspect();
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.EMAIL, businessProspect.getId(), "777888999");

    // then
    BusinessProspect dbRecord =
        businessProspectRepository.findById(businessProspect.getId()).orElseThrow();
    assertThat(dbRecord.isEmailVerified()).isTrue();
    assertThat(dbRecord.isPhoneVerified()).isFalse();

    // when
    testHelper.setBusinessProspectPhone(businessProspect.getId());
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.PHONE, businessProspect.getId(), "766255906");

    // then
    dbRecord = businessProspectRepository.findById(businessProspect.getId()).orElseThrow();
    assertThat(dbRecord.isEmailVerified()).isTrue();
    assertThat(dbRecord.isPhoneVerified()).isTrue();
    mockServerHelper.verifyEmailVerificationCalled(1);
    mockServerHelper.verifyPhoneVerificationCalled(1);

    // when
    setBusinessProspectPassword(businessProspect.getId());

    // then
    User user =
        fusionAuthService.retrieveUserByEmail(businessProspect.getEmail().getEncrypted()).user;
    log.info("user: {}", user);
    assertThat(user.email).isEqualTo(businessProspect.getEmail().getEncrypted());
    dbRecord = businessProspectRepository.findById(businessProspect.getId()).orElseThrow();
    log.info("dbRecord: {}", dbRecord);
    assertThat(dbRecord.getSubjectRef()).isEqualTo(user.id.toString());

    testHelper.testBusinessProspectState(
        dbRecord.getEmail().getEncrypted(), BusinessProspectStatus.COMPLETED);
  }

  @Test
  void convertBusinessProspect_success() throws Exception {
    // given
    mockServerHelper.expectOtpViaEmail();
    mockServerHelper.expectOtpViaSms();
    mockServerHelper.expectEmailVerification("777888999");
    mockServerHelper.expectPhoneVerification("766255906");

    // when
    BusinessProspect businessProspect = testHelper.createBusinessProspect();
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.EMAIL, businessProspect.getId(), "777888999");

    // then
    BusinessProspect dbRecord =
        businessProspectRepository.findById(businessProspect.getId()).orElseThrow();
    assertThat(dbRecord.isEmailVerified()).isTrue();
    assertThat(dbRecord.isPhoneVerified()).isFalse();

    // when
    testHelper.setBusinessProspectPhone(businessProspect.getId());
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.PHONE, businessProspect.getId(), "766255906");

    // then
    dbRecord = businessProspectRepository.findById(businessProspect.getId()).orElseThrow();
    assertThat(dbRecord.isEmailVerified()).isTrue();
    assertThat(dbRecord.isPhoneVerified()).isTrue();
    mockServerHelper.verifyEmailVerificationCalled(1);
    mockServerHelper.verifyPhoneVerificationCalled(1);

    // when
    String password = setBusinessProspectPassword(businessProspect.getId());

    // then
    User user =
        fusionAuthService.retrieveUserByEmail(businessProspect.getEmail().getEncrypted()).user;
    log.info("user: {}", user);
    assertThat(user.email).isEqualTo(businessProspect.getEmail().getEncrypted());
    dbRecord = businessProspectRepository.findById(businessProspect.getId()).orElseThrow();
    log.info("dbRecord: {}", dbRecord);
    assertThat(dbRecord.getSubjectRef()).isEqualTo(user.id.toString());

    // login
    testHelper.login(businessProspect.getEmail().getEncrypted(), password);

    // when
    ConvertBusinessProspectResponse convertBusinessProspectResponse =
        testHelper.convertBusinessProspect(businessProspect.getId());
    log.info("{}", convertBusinessProspectResponse);

    // then
    Assertions.assertThat(businessProspectRepository.findById(businessProspect.getId())).isEmpty();
    Business business = businessRepository.findById(businessProspect.getBusinessId()).orElseThrow();
    log.info("{}", business);
    Assertions.assertThat(business.getOnboardingStep())
        .isEqualTo(BusinessOnboardingStep.BUSINESS_OWNERS);
  }
}
