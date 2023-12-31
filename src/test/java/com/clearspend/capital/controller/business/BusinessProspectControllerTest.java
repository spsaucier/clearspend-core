package com.clearspend.capital.controller.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.client.twilio.TwilioServiceMock;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessProspectId;
import com.clearspend.capital.controller.type.business.prospect.BusinessProspectData;
import com.clearspend.capital.controller.type.business.prospect.BusinessProspectStatus;
import com.clearspend.capital.controller.type.business.prospect.ConvertBusinessProspectResponse;
import com.clearspend.capital.controller.type.business.prospect.SetBusinessProspectPasswordRequest;
import com.clearspend.capital.controller.type.business.prospect.ValidateBusinessProspectIdentifierRequest.IdentifierType;
import com.clearspend.capital.crypto.PasswordUtil;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessProspect;
import com.clearspend.capital.data.model.enums.BusinessOnboardingStep;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.clearspend.capital.data.model.enums.KnowYourBusinessStatus;
import com.clearspend.capital.data.repository.business.BusinessProspectRepository;
import com.clearspend.capital.data.repository.business.BusinessRepository;
import com.clearspend.capital.service.FusionAuthService.FusionAuthUserAccessor;
import com.clearspend.capital.service.kyc.BusinessKycStepHandler;
import io.fusionauth.domain.User;
import java.util.UUID;
import javax.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
class BusinessProspectControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;

  private final BusinessProspectRepository businessProspectRepository;
  private final BusinessRepository businessRepository;
  private final TwilioServiceMock twilioServiceMock;
  private final StripeClient stripeClient;
  private final BusinessKycStepHandler stepHandler;

  @BeforeEach
  void beforeEach() {
    twilioServiceMock.setLastChangePasswordId("NONE");
    twilioServiceMock.setLastVerificationEmail("NONE");
    twilioServiceMock.setLastChangePasswordId("NONE");
    twilioServiceMock.setLastOtp("NONE");
  }

  @Test
  void createBusinessProspect_success() throws Exception {
    // when
    BusinessProspect businessProspect = testHelper.createBusinessProspect();

    // then
    BusinessProspect dbRecord =
        businessProspectRepository.findById(businessProspect.getId()).orElseThrow();
    log.info("{}", dbRecord);
    assertThat(dbRecord).isEqualTo(businessProspect);
    assertThat(dbRecord.isEmailVerified()).isFalse();
    assertThat(dbRecord.getEmail().getEncrypted())
        .isEqualTo(twilioServiceMock.getLastVerificationEmail());
  }

  @Test
  void validateBusinessProspectEmail_success() throws Exception {
    // when
    BusinessProspect businessProspect = testHelper.createBusinessProspect();
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.EMAIL, businessProspect.getId(), "123456");

    // then
    BusinessProspect dbRecord =
        businessProspectRepository.findById(businessProspect.getId()).orElseThrow();
    assertThat(dbRecord.isEmailVerified()).isTrue();
    assertThat(twilioServiceMock.getLastOtp()).isEqualTo("123456");

    testHelper.testBusinessProspectState(
        dbRecord.getEmail().getEncrypted(), BusinessProspectStatus.EMAIL_VERIFIED);
  }

  @Test
  void resendOtpCodeForEmailValidation_success() throws Exception {
    BusinessProspect businessProspect = testHelper.createBusinessProspect();

    mvc.perform(
            get(String.format(
                    "/business-prospects/%s/%s/resend-otp",
                    businessProspect.getId(), IdentifierType.EMAIL))
                .contentType("application/json"))
        .andExpect(status().isOk())
        .andReturn();

    assertThat(twilioServiceMock.getLastVerificationEmail())
        .isEqualTo(businessProspect.getEmail().getEncrypted());
  }

  @Test
  void resendOtpCodeForEmailValidation_Fail_EmailValidated() throws Exception {
    BusinessProspect businessProspect = testHelper.createBusinessProspect();
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.EMAIL, businessProspect.getId(), "123456");

    MockHttpServletResponse response =
        mvc.perform(
                get(String.format(
                        "/business-prospects/%s/%s/resend-otp",
                        businessProspect.getId(), IdentifierType.EMAIL))
                    .contentType("application/json"))
            .andExpect(status().is4xxClientError())
            .andReturn()
            .getResponse();

    assertThat(response.getContentAsString()).contains("email already validated");
  }

  @Test
  void resendOtpCodeForPhoneValidation_Fail_PhoneValidated() throws Exception {
    BusinessProspect businessProspect = testHelper.createBusinessProspect();
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.EMAIL, businessProspect.getId(), "123456");
    testHelper.setBusinessProspectPhone(businessProspect.getId());
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.PHONE, businessProspect.getId(), "766255906");

    MockHttpServletResponse response =
        mvc.perform(
                get(String.format(
                        "/business-prospects/%s/%s/resend-otp",
                        businessProspect.getId(), IdentifierType.PHONE))
                    .contentType("application/json"))
            .andExpect(status().is4xxClientError())
            .andReturn()
            .getResponse();

    assertThat(response.getContentAsString()).contains("phone already validated");
  }

  @Test
  void resendOtpCodeForPhoneValidation_Fail_PhoneNotSet() throws Exception {
    BusinessProspect businessProspect = testHelper.createBusinessProspect();
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.EMAIL, businessProspect.getId(), "123456");

    MockHttpServletResponse response =
        mvc.perform(
                get(String.format(
                        "/business-prospects/%s/%s/resend-otp",
                        businessProspect.getId(), IdentifierType.PHONE))
                    .contentType("application/json"))
            .andExpect(status().is4xxClientError())
            .andReturn()
            .getResponse();

    assertThat(response.getContentAsString()).contains("Phone is not set");
  }

  @Test
  void resendOtpCodeForEmailValidation_Fail_RecordNotFount() throws Exception {
    mvc.perform(
            get(String.format(
                    "/business-prospects/%s/%s/resend-otp", new TypedId<>(), IdentifierType.EMAIL))
                .contentType("application/json"))
        .andExpect(status().is(404))
        .andReturn()
        .getResponse();
  }

  @Test
  void resendOtpCodeForEmailValidation_Fail_InvalidOtpType() throws Exception {
    MockHttpServletResponse response =
        mvc.perform(
                get(String.format("/business-prospects/%s/%s/resend-otp", new TypedId<>(), "test"))
                    .contentType("application/json"))
            .andExpect(status().is5xxServerError())
            .andReturn()
            .getResponse();

    assertThat(response.getContentAsString()).contains("Failed to convert value");
  }

  @Test
  void resendOtpCodeForPhoneValidation_Fail_RecordNotFount() throws Exception {
    mvc.perform(
            get(String.format(
                    "/business-prospects/%s/%s/resend-otp", new TypedId<>(), IdentifierType.PHONE))
                .contentType("application/json"))
        .andExpect(status().is(404))
        .andReturn()
        .getResponse();
  }

  @Test
  void resendOtpCodeForPhoneValidation_success() throws Exception {
    BusinessProspect businessProspect = testHelper.createBusinessProspect();
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.EMAIL, businessProspect.getId(), "123456");
    testHelper.setBusinessProspectPhone(businessProspect.getId());

    mvc.perform(
            get(String.format(
                    "/business-prospects/%s/%s/resend-otp",
                    businessProspect.getId(), IdentifierType.PHONE))
                .contentType("application/json"))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();

    BusinessProspect dbRecord =
        businessProspectRepository.findById(businessProspect.getId()).orElseThrow();
    assertThat(dbRecord.isEmailVerified()).isTrue();
    assertThat(twilioServiceMock.getLastVerificationPhone())
        .isEqualTo(dbRecord.getPhone().getEncrypted());

    testHelper.testBusinessProspectState(
        dbRecord.getEmail().getEncrypted(), BusinessProspectStatus.EMAIL_VERIFIED);
  }

  @Test
  void setBusinessProspectPhone_success() throws Exception {
    // when
    BusinessProspect businessProspect = testHelper.createBusinessProspect();
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.EMAIL, businessProspect.getId(), "234567890");
    testHelper.setBusinessProspectPhone(businessProspect.getId());

    // then
    BusinessProspect dbRecord =
        businessProspectRepository.findById(businessProspect.getId()).orElseThrow();
    assertThat(dbRecord.isEmailVerified()).isTrue();
    assertThat(dbRecord.isPhoneVerified()).isFalse();
    assertThat(twilioServiceMock.getLastOtp()).isEqualTo("234567890");

    testHelper.testBusinessProspectState(
        dbRecord.getEmail().getEncrypted(), BusinessProspectStatus.EMAIL_VERIFIED);
  }

  @Test
  void validateBusinessProspectPhone_success() throws Exception {
    // when
    BusinessProspect businessProspect = testHelper.createBusinessProspect();
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.EMAIL, businessProspect.getId(), "777888999");

    // then
    BusinessProspect dbRecord =
        businessProspectRepository.findById(businessProspect.getId()).orElseThrow();
    assertThat(dbRecord.isEmailVerified()).isTrue();
    assertThat(dbRecord.isPhoneVerified()).isFalse();
    assertThat(twilioServiceMock.getLastOtp()).isEqualTo("777888999");

    // when
    testHelper.setBusinessProspectPhone(businessProspect.getId());
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.PHONE, businessProspect.getId(), "766255906");

    // then
    dbRecord = businessProspectRepository.findById(businessProspect.getId()).orElseThrow();
    assertThat(dbRecord.isEmailVerified()).isTrue();
    assertThat(dbRecord.isPhoneVerified()).isTrue();
    assertThat(twilioServiceMock.getLastOtp()).isEqualTo("766255906");

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

  @FusionAuthUserAccessor(reviewer = "jscarbor", explanation = "testing")
  @Test
  void setBusinessProspectPassword_success() throws Exception {
    // when
    BusinessProspect businessProspect = testHelper.createBusinessProspect();
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.EMAIL, businessProspect.getId(), "777888999");

    // then
    BusinessProspect dbRecord =
        businessProspectRepository.findById(businessProspect.getId()).orElseThrow();
    assertThat(dbRecord.isEmailVerified()).isTrue();
    assertThat(dbRecord.isPhoneVerified()).isFalse();
    assertThat(twilioServiceMock.getLastOtp()).isEqualTo("777888999");

    // when
    testHelper.setBusinessProspectPhone(businessProspect.getId());
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.PHONE, businessProspect.getId(), "766255906");

    // then
    dbRecord = businessProspectRepository.findById(businessProspect.getId()).orElseThrow();
    assertThat(dbRecord.isEmailVerified()).isTrue();
    assertThat(dbRecord.isPhoneVerified()).isTrue();
    assertThat(twilioServiceMock.getLastOtp()).isEqualTo("766255906");

    // when
    setBusinessProspectPassword(businessProspect.getId());

    // then
    User user = testHelper.getUser(UUID.fromString(businessProspect.getSubjectRef()));
    log.info("user: {}", user);
    assertThat(user.email).isEqualTo(businessProspect.getEmail().getEncrypted());
    dbRecord = businessProspectRepository.findById(businessProspect.getId()).orElseThrow();
    log.info("dbRecord: {}", dbRecord);
    assertThat(dbRecord.getSubjectRef()).isEqualTo(user.id.toString());

    testHelper.testBusinessProspectState(
        dbRecord.getEmail().getEncrypted(), BusinessProspectStatus.COMPLETED);
  }

  @FusionAuthUserAccessor(reviewer = "jscarbor", explanation = "testing")
  @Test
  void convertBusinessProspect_success() throws Exception {
    // when
    BusinessProspect businessProspect = testHelper.createBusinessProspect();
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.EMAIL, businessProspect.getId(), "777888999");

    // then
    BusinessProspect dbRecord =
        businessProspectRepository.findById(businessProspect.getId()).orElseThrow();
    assertThat(dbRecord.isEmailVerified()).isTrue();
    assertThat(dbRecord.isPhoneVerified()).isFalse();
    assertThat(twilioServiceMock.getLastOtp()).isEqualTo("777888999");

    // when
    testHelper.setBusinessProspectPhone(businessProspect.getId());
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.PHONE, businessProspect.getId(), "766255906");

    // then
    dbRecord = businessProspectRepository.findById(businessProspect.getId()).orElseThrow();
    assertThat(dbRecord.isEmailVerified()).isTrue();
    assertThat(dbRecord.isPhoneVerified()).isTrue();
    assertThat(twilioServiceMock.getLastOtp()).isEqualTo("766255906");

    // when
    String password = setBusinessProspectPassword(businessProspect.getId());

    // then
    User user = testHelper.getUser(UUID.fromString(businessProspect.getSubjectRef()));
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

  @Test
  void convertBusinessProspect_HardFail() throws Exception {
    BusinessProspect businessProspect = testHelper.createBusinessProspect();
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.EMAIL, businessProspect.getId(), "777888999");
    assertThat(twilioServiceMock.getLastOtp()).isEqualTo("777888999");

    testHelper.setBusinessProspectPhone(businessProspect.getId());
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.PHONE, businessProspect.getId(), "766255906");
    assertThat(twilioServiceMock.getLastOtp()).isEqualTo("766255906");

    String password = setBusinessProspectPassword(businessProspect.getId());
    testHelper.login(businessProspect.getEmail().getEncrypted(), password);

    ConvertBusinessProspectResponse convertBusinessProspectResponse =
        testHelper.convertBusinessProspect("Denied", businessProspect.getId());
    log.info("{}", convertBusinessProspectResponse);
    Business business = businessRepository.findById(businessProspect.getBusinessId()).orElseThrow();
    stepHandler.execute(
        business, stripeClient.retrieveCompleteAccount(business.getStripeData().getAccountRef()));
    assertThat(businessProspectRepository.findById(businessProspect.getId())).isEmpty();
    business = businessRepository.findById(businessProspect.getBusinessId()).orElseThrow();
    log.info("{}", business);
    assertThat(business.getOnboardingStep()).isEqualTo(BusinessOnboardingStep.BUSINESS_OWNERS);
    assertThat(business.getKnowYourBusinessStatus()).isEqualTo(KnowYourBusinessStatus.FAIL);
    assertThat(business.getStatus()).isEqualTo(BusinessStatus.CLOSED);
  }

  @Test
  void convertBusinessProspect_SoftFail() throws Exception {
    BusinessProspect businessProspect = testHelper.createBusinessProspect();
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.EMAIL, businessProspect.getId(), "777888999");
    assertThat(twilioServiceMock.getLastOtp()).isEqualTo("777888999");

    testHelper.setBusinessProspectPhone(businessProspect.getId());
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.PHONE, businessProspect.getId(), "766255906");
    assertThat(twilioServiceMock.getLastOtp()).isEqualTo("766255906");

    String password = setBusinessProspectPassword(businessProspect.getId());
    testHelper.login(businessProspect.getEmail().getEncrypted(), password);

    ConvertBusinessProspectResponse convertBusinessProspectResponse =
        testHelper.convertBusinessProspect("Review", businessProspect.getId());
    log.info("{}", convertBusinessProspectResponse);
    Business business = businessRepository.findById(businessProspect.getBusinessId()).orElseThrow();
    stepHandler.execute(
        business, stripeClient.retrieveCompleteAccount(business.getStripeData().getAccountRef()));
    assertThat(businessProspectRepository.findById(businessProspect.getId())).isEmpty();
    business = businessRepository.findById(businessProspect.getBusinessId()).orElseThrow();
    log.info("{}", business);
    assertThat(business.getOnboardingStep()).isEqualTo(BusinessOnboardingStep.SOFT_FAIL);
    assertThat(business.getKnowYourBusinessStatus()).isEqualTo(KnowYourBusinessStatus.REVIEW);
    assertThat(business.getStatus()).isEqualTo(BusinessStatus.ONBOARDING);
  }

  @Test
  void getBusinessProspect_success() throws Exception {
    BusinessProspect businessProspect = testHelper.createBusinessProspect();
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.EMAIL, businessProspect.getId(), "777888999");
    assertThat(twilioServiceMock.getLastOtp()).isEqualTo("777888999");

    testHelper.setBusinessProspectPhone(businessProspect.getId());
    testHelper.validateBusinessProspectIdentifier(
        IdentifierType.PHONE, businessProspect.getId(), "766255906");
    assertThat(twilioServiceMock.getLastOtp()).isEqualTo("766255906");

    String password = setBusinessProspectPassword(businessProspect.getId());

    Cookie cookie = testHelper.login(businessProspect.getEmail().getEncrypted(), password);

    MockHttpServletResponse response =
        mvc.perform(
                get("/business-prospects/" + businessProspect.getId())
                    .contentType("application/json")
                    .cookie(cookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    BusinessProspectData businessProspectData =
        objectMapper.readValue(response.getContentAsString(), BusinessProspectData.class);
    org.junit.jupiter.api.Assertions.assertEquals(
        businessProspect.getFirstName().getEncrypted(), businessProspectData.getFirstName());
    org.junit.jupiter.api.Assertions.assertEquals(
        businessProspect.getLastName().getEncrypted(), businessProspectData.getLastName());
    org.junit.jupiter.api.Assertions.assertEquals(
        businessProspect.getEmail().getEncrypted(), businessProspectData.getEmail());
    org.junit.jupiter.api.Assertions.assertEquals(
        businessProspect.getBusinessType(), businessProspectData.getBusinessType());
  }
}
