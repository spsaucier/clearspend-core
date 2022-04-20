package com.clearspend.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.client.twilio.TwilioServiceMock;
import com.clearspend.capital.controller.type.user.ChangePasswordRequest;
import com.clearspend.capital.controller.type.user.ForgotPasswordRequest;
import com.clearspend.capital.controller.type.user.LoginRequest;
import com.clearspend.capital.controller.type.user.ResetPasswordRequest;
import com.clearspend.capital.controller.type.user.UserLoginResponse;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.model.enums.card.BinType;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.model.security.DefaultRoles;
import com.clearspend.capital.service.BusinessService;
import com.clearspend.capital.service.CardService;
import com.clearspend.capital.service.UserService;
import com.clearspend.capital.service.UserService.CreateUpdateUserRecord;
import io.fusionauth.domain.api.user.ChangePasswordResponse;
import java.util.Collections;
import javax.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class AuthenticationControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final TwilioServiceMock twilioServiceMock;
  private final UserService userService;
  private final CardService cardService;
  private final BusinessService businessService;

  private CreateBusinessRecord createBusinessRecord;
  private UserService.CreateUpdateUserRecord user;
  private Cookie userCookie;

  @SneakyThrows
  @BeforeEach
  void init() {
    if (createBusinessRecord == null) {
      createBusinessRecord = testHelper.init();
      user = testHelper.createUser(createBusinessRecord.business());
      userCookie = testHelper.login(user.user());
    }
  }

  @Test
  @SneakyThrows
  void forgotPassword() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();

    ForgotPasswordRequest forgotPasswordRequest =
        new ForgotPasswordRequest(createBusinessRecord.email());
    String body = objectMapper.writeValueAsString(forgotPasswordRequest);

    mvc.perform(
            post("/authentication/forgot-password")
                .content(body)
                .contentType(APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();

    ResetPasswordRequest resetPasswordRequest =
        new ResetPasswordRequest(twilioServiceMock.getLastChangePasswordId(), "Qwerty12345!");
    body = objectMapper.writeValueAsString(resetPasswordRequest);

    mvc.perform(
            post("/authentication/reset-password")
                .content(body)
                .contentType(APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();
  }

  @Test
  @SneakyThrows
  void emailDoesNotExist() {
    String changePasswordId =
        "This value should not be recreated as a result of sending reset password email";
    twilioServiceMock.setLastChangePasswordId(changePasswordId);

    ForgotPasswordRequest forgotPasswordRequest =
        new ForgotPasswordRequest("no_user_with_this_email@nowhere.io");
    String body = objectMapper.writeValueAsString(forgotPasswordRequest);

    mvc.perform(
            post("/authentication/forgot-password")
                .content(body)
                .contentType(APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();

    assertThat(twilioServiceMock.getLastChangePasswordId()).isEqualTo(changePasswordId);
  }

  @Test
  @SneakyThrows
  void invalidChangePasswordId() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();

    ForgotPasswordRequest forgotPasswordRequest =
        new ForgotPasswordRequest(createBusinessRecord.email());
    String body = objectMapper.writeValueAsString(forgotPasswordRequest);

    mvc.perform(
            post("/authentication/forgot-password")
                .content(body)
                .contentType(APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andReturn()
        .getResponse();

    ResetPasswordRequest resetPasswordRequest =
        new ResetPasswordRequest(
            twilioServiceMock.getLastChangePasswordId() + "123", "Qwerty12345!");
    body = objectMapper.writeValueAsString(resetPasswordRequest);

    mvc.perform(
            post("/authentication/reset-password")
                .content(body)
                .contentType(APPLICATION_JSON_VALUE))
        .andExpect(status().isForbidden())
        .andReturn()
        .getResponse();
  }

  @Test
  @SneakyThrows
  void testIfIncorrectLoginIdOrPassword() {
    Cookie authCookie = userCookie;
    checkLoginDetailsAndUpdatePassword(
        authCookie, user.user().getEmail().toString(), user.password(), "lucky1234");
    checkLoginDetailsAndUpdatePassword(
        authCookie, user.user().getEmail().toString(), "lucky1234", "lucky1234");
  }

  private void checkLoginDetailsAndUpdatePassword(
      Cookie authCookie, String loginId, String currentPassword, String newPassword)
      throws Exception {
    // Check login details against fusion auth and throw errors as per the response
    ChangePasswordRequest changePasswordRequest =
        new ChangePasswordRequest(loginId, currentPassword, newPassword);
    String body = objectMapper.writeValueAsString(changePasswordRequest);
    mvc.perform(
            post("/authentication/change-password")
                .contentType("application/json")
                .content(body)
                .cookie(authCookie))
        .andReturn()
        .getResponse();
  }

  @Test
  @SneakyThrows
  void expirePasswordForNextLogin() {
    // CAP-601 support reset password email
    // The astute reader will wonder why the heck this test issues cards
    // instead of just creating a user with an expired password.  The latter is the main use
    // case we want to see working at the time of this test writing.

    testHelper.setCurrentUser(createBusinessRecord.user());

    // Create a user without registering with FusionAuth
    CreateUpdateUserRecord newUser =
        userService.createUser(
            createBusinessRecord.business().getId(),
            UserType.EMPLOYEE,
            testHelper.generateFirstName(),
            testHelper.generateLastName(),
            testHelper.generateEntityAddress(),
            testHelper.generateEmail(),
            testHelper.generatePhone());

    twilioServiceMock.setLastUserAccountCreatedPassword("NONE");

    // Issuing the card registers with FA and sends an email to reset their password
    testHelper.setCurrentUser(createBusinessRecord.user());
    cardService.issueCard(
        BinType.DEBIT,
        FundingType.POOLED,
        CardType.VIRTUAL,
        createBusinessRecord.business().getId(),
        createBusinessRecord.allocationRecord().allocation().getId(),
        newUser.user().getId(),
        Currency.USD,
        false,
        createBusinessRecord.business().getLegalName(),
        Collections.emptyMap(),
        Collections.emptySet(),
        Collections.emptySet(),
        newUser.user().getAddress());

    String newPassword = twilioServiceMock.getLastUserAccountCreatedPassword();
    // check that the email arrived
    assertThat(newPassword).isNotEqualTo("NONE");

    LoginRequest request = new LoginRequest(newUser.user().getEmail().getEncrypted(), newPassword);
    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(post("/authentication/login").contentType("application/json").content(body))
            .andExpect(status().is(203))
            .andReturn()
            .getResponse();

    UserLoginResponse changePasswordResponse =
        objectMapper.readValue(response.getContentAsString(), UserLoginResponse.class);

    assertThat(changePasswordResponse.getChangePasswordId()).isNotNull();

    // Set a new password
    String replacementPassword = testHelper.generatePassword(16);
    ChangePasswordRequest changePasswordRequest =
        new ChangePasswordRequest(
            newUser.user().getEmail().getEncrypted(), newPassword, replacementPassword);

    MockHttpServletResponse httpResponseAfterChange =
        mvc.perform(
                post(
                        "/authentication/change-password/{changePasswordId}",
                        changePasswordResponse.getChangePasswordId())
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(changePasswordRequest)))
            .andExpect(status().is(200))
            .andReturn()
            .getResponse();

    ChangePasswordResponse faChangeResponse =
        objectMapper.readValue(
            httpResponseAfterChange.getContentAsString(), ChangePasswordResponse.class);

    // Log in and get a fresh cookie.
    Cookie cookie = testHelper.login(newUser.user().getEmail().getEncrypted(), replacementPassword);
    assertThat(cookie).isNotNull();
  }

  @SneakyThrows
  @Test
  void login_suspendedBusinessCausesLoginFailure() {
    User user = createBusinessRecord.user();
    User csManager =
        testHelper
            .createUserWithGlobalRole(
                createBusinessRecord.business(), DefaultRoles.GLOBAL_CUSTOMER_SERVICE)
            .user();

    LoginRequest request =
        new LoginRequest(user.getEmail().getEncrypted(), testHelper.getPassword(user));
    String body = objectMapper.writeValueAsString(request);

    MockHttpServletResponse response =
        mvc.perform(post("/authentication/login").contentType("application/json").content(body))
            .andExpect(status().is(200))
            .andReturn()
            .getResponse();

    UserLoginResponse successful =
        objectMapper.readValue(response.getContentAsString(), UserLoginResponse.class);

    testHelper.setCurrentUser(csManager);
    businessService.updateBusinessStatus(user.getBusinessId(), BusinessStatus.SUSPENDED);

    MockHttpServletResponse suspendedResponse =
        mvc.perform(post("/authentication/login").contentType("application/json").content(body))
            .andExpect(status().is(403))
            .andReturn()
            .getResponse();

    assertThat(suspendedResponse.getContentAsString()).isEqualTo("");
  }
}
