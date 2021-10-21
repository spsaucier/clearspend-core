package com.tranwall.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockserver.model.HttpRequest.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tranwall.capital.BaseCapitalTest;
import com.tranwall.capital.TestHelper;
import com.tranwall.capital.common.typedid.data.BusinessProspectId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.business.prospect.SetBusinessProspectPasswordRequest;
import com.tranwall.capital.controller.type.business.prospect.ValidateBusinessProspectIdentifierRequest.IdentifierType;
import com.tranwall.capital.crypto.PasswordUtil;
import com.tranwall.capital.data.model.BusinessProspect;
import com.tranwall.capital.data.repository.BusinessProspectRepository;
import com.tranwall.capital.service.FusionAuthService;
import io.fusionauth.domain.api.UserResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

  @Test
  void createBusinessProspect_success() throws Exception {
    // given
    mockServerHelper.expectOtpViaEmail();

    // when
    BusinessProspect businessProspect = testHelper.createBusinessProspect().businessProspect();

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
    BusinessProspect businessProspect = testHelper.createBusinessProspect().businessProspect();
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.EMAIL, businessProspect.getId(), "123456");

    // then
    BusinessProspect dbRecord =
        businessProspectRepository.findById(businessProspect.getId()).orElseThrow();
    assertThat(dbRecord.isEmailVerified()).isTrue();
    mockServerHelper.verifyEmailVerificationCalled(1);
  }

  @Test
  void setBusinessProspectPhone_success() throws Exception {
    // given
    mockServerHelper.expectOtpViaEmail();
    mockServerHelper.expectOtpViaSms();
    mockServerHelper.expectEmailVerification("234567890");
    mockServerHelper.expectPhoneVerification("766255906");

    // when
    BusinessProspect businessProspect = testHelper.createBusinessProspect().businessProspect();
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.EMAIL, businessProspect.getId(), "234567890");

    // then
    BusinessProspect dbRecord =
        businessProspectRepository.findById(businessProspect.getId()).orElseThrow();
    assertThat(dbRecord.isEmailVerified()).isTrue();
    assertThat(dbRecord.isPhoneVerified()).isFalse();
    mockServerHelper.verifyEmailVerificationCalled(1);
  }

  @Test
  void validateBusinessProspectPhone_success() throws Exception {
    // given
    mockServerHelper.expectOtpViaEmail();
    mockServerHelper.expectOtpViaSms();
    mockServerHelper.expectEmailVerification("777888999");
    mockServerHelper.expectPhoneVerification("766255906");

    // when
    BusinessProspect businessProspect = testHelper.createBusinessProspect().businessProspect();
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
  }

  void setBusinessProspectPassword(TypedId<BusinessProspectId> businessProspectId)
      throws Exception {
    SetBusinessProspectPasswordRequest request =
        new SetBusinessProspectPasswordRequest(PasswordUtil.generatePassword());
    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(
                post(String.format("/business-prospects/%s/password", businessProspectId))
                    .contentType("application/json")
                    .content(body))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();
  }

  @Test
  void setBusinessProspectPassword_success() throws Exception {
    // given
    mockServerHelper.expectOtpViaEmail();
    mockServerHelper.expectOtpViaSms();
    mockServerHelper.expectEmailVerification("777888999");
    mockServerHelper.expectPhoneVerification("766255906");

    // when
    BusinessProspect businessProspect = testHelper.createBusinessProspect().businessProspect();
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
    mockServerHelper.expectFusionAuthCreateUser(businessProspect.getBusinessOwnerId());
    setBusinessProspectPassword(businessProspect.getId());

    // then
    UserResponse user = fusionAuthService.findUser(businessProspect.getEmail().getEncrypted());
    log.info("user: {}", user);
  }

  /*
    @Test
    void convertBusinessProspect_success() throws Exception {
      CreateBusinessProspectRecord createBusinessProspectRecord = testHelper.createBusinessProspect();
      testHelper.validateBusinessProspectIdentifier(
          IdentifierType.EMAIL,
          createBusinessProspectRecord.businessProspect().getId(),
          createBusinessProspectRecord.otp());
      String otp =
          testHelper.setBusinessProspectPhone(
              createBusinessProspectRecord.businessProspect().getId());
      testHelper.validateBusinessProspectIdentifier(
          IdentifierType.PHONE, createBusinessProspectRecord.businessProspect().getId(), otp);
      setBusinessProspectPassword(createBusinessProspectRecord.businessProspect().getId());
      testHelper.convertBusinessProspect(createBusinessProspectRecord.businessProspect().getId());
    }
  */
}