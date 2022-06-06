package com.clearspend.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.MockMvcHelper;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.client.twilio.TwilioServiceMock;
import com.clearspend.capital.common.error.FusionAuthException;
import com.clearspend.capital.configuration.SecurityConfig;
import com.clearspend.capital.controller.AuthenticationController.ChangeMethodRequest;
import com.clearspend.capital.controller.AuthenticationController.FirstTwoFactorSendRequest;
import com.clearspend.capital.controller.AuthenticationController.FirstTwoFactorValidateRequest;
import com.clearspend.capital.controller.AuthenticationController.TwoFactorAuthenticationMethod;
import com.clearspend.capital.controller.security.TestFusionAuthClient;
import com.clearspend.capital.controller.type.twofactor.TwoFactorStartResponse;
import com.clearspend.capital.controller.type.user.ChangePasswordRequest;
import com.clearspend.capital.controller.type.user.ForgotPasswordRequest;
import com.clearspend.capital.controller.type.user.LoginRequest;
import com.clearspend.capital.controller.type.user.ResetPasswordRequest;
import com.clearspend.capital.controller.type.user.UserLoginResponse;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.service.FusionAuthService;
import com.inversoft.error.Errors;
import com.inversoft.rest.ClientResponse;
import io.fusionauth.client.FusionAuthClient;
import io.fusionauth.domain.TwoFactorMethod;
import io.fusionauth.domain.api.TwoFactorResponse;
import io.fusionauth.domain.api.twoFactor.TwoFactorLoginRequest;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.Cookie;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class AuthenticationController_2FATest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final MockMvcHelper mockMvcHelper;
  private final TestHelper testHelper;
  private final FusionAuthClient fusionAuthClient;
  private final TwilioServiceMock twilioServiceMock;

  /**
   * This does a byzantine bumbling test of setting up two factor authentication and disabling it,
   * using wrong codes before right codes all along the way to validate both affirmative and
   * negative cases. This is done because all of the successful steps are necessary to set up any of
   * the failed steps later on.
   *
   * <p>For a straightforward example of two factor setup, see {@link #twoFactorSetup(User)}.
   *
   * <p>For a straightforward example of two factor log in, see {@link #twoFactorLogin(String,
   * String, String)}
   */
  @Test
  @SneakyThrows
  void twoFactorLogin() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    TestFusionAuthClient faClient = (TestFusionAuthClient) fusionAuthClient;
    User user = createBusinessRecord.user();

    // Enable 2FA
    Cookie userCookie = createBusinessRecord.authCookie();
    final String userPhone = user.getPhone().getEncrypted();

    // "/api/two-factor/send"
    // Try to enable without an auth cookie
    Cookie sugarCookie = new Cookie("sugarCookie", "sweet");
    MockHttpServletResponse response = begin2FASetup(sugarCookie, userPhone);
    assertEquals(403, response.getStatus());

    // Enable with the correct auth cookie
    response = begin2FASetup(userCookie, userPhone);
    assertEquals(200, response.getStatus());

    // Confirm receipt of the code to enable 2FA
    final String twoFactorCodeForEnable = faClient.getLastCode(userPhone);

    String wrong2FCodeForEnable =
        generateWrongCode(() -> RandomStringUtils.randomNumeric(6), Set.of(twoFactorCodeForEnable));

    response =
        enable2FA(userCookie, wrong2FCodeForEnable, userPhone, TwoFactorAuthenticationMethod.sms);
    assertThat(response.getStatus()).isEqualTo(421);

    List<String> recoveryCodes =
        validateResponse(
                enable2FA(
                    userCookie,
                    twoFactorCodeForEnable,
                    userPhone,
                    TwoFactorAuthenticationMethod.sms),
                TwoFactorResponse.class)
            .recoveryCodes;

    // 2FA is enabled, now try logging in
    final String username = user.getEmail().getEncrypted();
    final String password = testHelper.getPassword(user);

    UserLoginResponse twoFactorAuthenticationStart = login(username, password);

    final String twoFactorId = twoFactorAuthenticationStart.getTwoFactorId();
    String twoFactorCodeForLogin = faClient.getLastCode(userPhone);
    assertThat(twoFactorId).isNotNull();
    assertThat(twoFactorCodeForLogin).isNotNull();

    // and confirm the 2F
    final String wrong2FCode =
        generateWrongCode(() -> RandomStringUtils.randomNumeric(6), Set.of(twoFactorCodeForLogin));

    final String wrong2FId =
        generateWrongCode(() -> RandomStringUtils.randomAlphanumeric(13), Set.of(twoFactorId));

    // Try with a bad TwoFactorID and get a 404
    MockHttpServletResponse loginCompleteResponse =
        complete2FALogin(wrong2FId, twoFactorCodeForLogin);
    assertThat(loginCompleteResponse.getStatus()).isEqualTo(404);

    // Try with a bad code and get 421
    loginCompleteResponse = complete2FALogin(twoFactorId, wrong2FCode);
    assertThat(loginCompleteResponse.getStatus()).isEqualTo(421);

    // Successful login
    loginCompleteResponse = complete2FALogin(twoFactorId, twoFactorCodeForLogin);
    assertThat(loginCompleteResponse.getStatus()).isEqualTo(200);
    Cookie authCookie = loginCompleteResponse.getCookie(SecurityConfig.ACCESS_TOKEN_COOKIE_NAME);

    // Validate 403 on re-start 2FA
    String hackerPhone = testHelper.generatePhone();
    response = begin2FASetup(userCookie, hackerPhone);
    assertThat(response.getStatus()).isEqualTo(403);

    // Check that the authCookie works
    assertNotNull(authCookie);
    validateCookie(authCookie);

    // Gearing up for password change - generate a new password
    String newPassword = RandomStringUtils.randomAlphanumeric(10);

    // step up 2FA for password change
    MockHttpServletResponse changePasswordResponse =
        changePassword(user, authCookie, null, null, null, password, newPassword);
    assertThat(changePasswordResponse.getStatus()).isEqualTo(242);
    TwoFactorStartResponse twoFactorStartLoggedInResponse =
        validateResponse(changePasswordResponse, TwoFactorStartResponse.class);

    // change the password
    String twoFactorCode = faClient.getLastCode(userPhone);
    MockHttpServletResponse realChangeResponse =
        changePassword(
            user,
            authCookie,
            twoFactorStartLoggedInResponse.trustChallenge(),
            twoFactorStartLoggedInResponse.twoFactorId(),
            twoFactorCode,
            null,
            null);
    assertThat(realChangeResponse.getStatus()).isEqualTo(204);
    assertThat(realChangeResponse.getContentLength()).isEqualTo(0);

    // NB the old auth cookie still works
    validateCookie(authCookie);

    // get a new auth cookie with the new password and verify it works
    authCookie = twoFactorLogin(username, newPassword, userPhone);
    validateCookie(authCookie);

    // Start to disable 2FA
    @NotNull TwoFactorStartResponse startResponse = start2FALoggedIn(authCookie);
    final String twoFactorCodeForDisable = faClient.getLastCode(userPhone);

    // Try to disable with the wrong code
    assertThrows(
        FusionAuthException.class,
        () ->
            disable2FA(
                userCookie,
                generateWrongCode(
                    () -> RandomStringUtils.randomNumeric(6),
                    makeSet(twoFactorCodeForDisable, recoveryCodes)),
                startResponse.methodId()));

    // Actually disable it
    disable2FA(userCookie, twoFactorCodeForDisable, startResponse.methodId());

    // Try to disable again
    assertThrows(
        FusionAuthException.class,
        () -> disable2FA(userCookie, twoFactorCodeForDisable, startResponse.methodId()));

    // log in again without 2FA
    authCookie = testHelper.login(username, newPassword);
    assertThat(authCookie).isNotNull();
    validateCookie(authCookie);
  }

  @SneakyThrows
  private Cookie twoFactorSetup(User user) {
    String userPhone = user.getPhone().getEncrypted();

    TestFusionAuthClient faClient = (TestFusionAuthClient) fusionAuthClient;

    // Enable 2FA
    Cookie userCookie = testHelper.login(user);

    // "/api/two-factor/send"
    MockHttpServletResponse response = begin2FASetup(userCookie, userPhone);
    assertEquals(200, response.getStatus());

    // Confirm receipt of the code to enable 2FA
    final String twoFactorCodeForEnable = faClient.getLastCode(userPhone);

    List<String> recoveryCodes =
        validateResponse(
                enable2FA(
                    userCookie,
                    twoFactorCodeForEnable,
                    userPhone,
                    TwoFactorAuthenticationMethod.sms),
                TwoFactorResponse.class)
            .recoveryCodes;
    assertThat(recoveryCodes.size()).isEqualTo(10);

    // 2FA is enabled, now try logging in
    final String username = user.getEmail().getEncrypted();
    final String password = testHelper.getPassword(user);

    UserLoginResponse twoFactorAuthenticationStart = login(username, password);

    final String twoFactorId = twoFactorAuthenticationStart.getTwoFactorId();
    String twoFactorCodeForLogin = faClient.getLastCode(userPhone);
    assertThat(twoFactorId).isNotNull();
    assertThat(twoFactorCodeForLogin).isNotNull();

    // Successful login
    MockHttpServletResponse loginCompleteResponse =
        complete2FALogin(twoFactorId, twoFactorCodeForLogin);
    assertThat(loginCompleteResponse.getStatus()).isEqualTo(200);
    return loginCompleteResponse.getCookie(SecurityConfig.ACCESS_TOKEN_COOKIE_NAME);
  }

  @Test
  @SneakyThrows
  void twoFactorForgotAndResetPassword() {
    final String newPassword = UUID.randomUUID().toString();
    final CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    twoFactorSetup(createBusinessRecord.user());
    TestFusionAuthClient faClient = (TestFusionAuthClient) fusionAuthClient;

    final ForgotPasswordRequest forgotRequest =
        new ForgotPasswordRequest(createBusinessRecord.user().getEmail().getEncrypted());

    mockMvcHelper
        .query("/authentication/forgot-password", HttpMethod.POST, null, forgotRequest)
        .andExpect(status().isOk());

    assertNotNull(twilioServiceMock.getLastChangePasswordId());

    final ResetPasswordRequest resetRequest =
        new ResetPasswordRequest(twilioServiceMock.getLastChangePasswordId(), newPassword);

    final String resetPasswordResponseString =
        mockMvcHelper
            .query("/authentication/reset-password", HttpMethod.POST, null, resetRequest)
            .andExpect(status().is(242))
            .andReturn()
            .getResponse()
            .getContentAsString();
    final TwoFactorStartResponse resetPasswordResponse =
        objectMapper.readValue(resetPasswordResponseString, TwoFactorStartResponse.class);

    final ResetPasswordRequest resetRequestPost2Factor =
        new ResetPasswordRequest(
            resetRequest.getChangePasswordId(),
            resetRequest.getNewPassword(),
            faClient.getLastCode(createBusinessRecord.user().getPhone().getEncrypted()),
            resetPasswordResponse.twoFactorId(),
            resetPasswordResponse.trustChallenge());
    mockMvcHelper
        .query("/authentication/reset-password", HttpMethod.POST, null, resetRequestPost2Factor)
        .andExpect(status().isNoContent());

    // Verify everything works by ensuring we can login with the new password
    final LoginRequest loginRequest =
        new LoginRequest(createBusinessRecord.user().getEmail().getEncrypted(), newPassword);
    mockMvcHelper
        .query("/authentication/login", HttpMethod.POST, null, loginRequest)
        .andExpect(status().isOk());
  }

  @Test
  @SneakyThrows
  void twoFactorChangePhone() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    User user = createBusinessRecord.user();

    // Enable 2FA
    Cookie userCookie = twoFactorSetup(user);

    final String newUserPhone = testHelper.generatePhone();
    final UUID userId = UUID.fromString(user.getSubjectRef());
    addMethod(
        user,
        userCookie,
        newUserPhone,
        TwoFactorAuthenticationMethod.sms,
        user.getPhone().getEncrypted());
    TwoFactorMethod method =
        validateResponse(fusionAuthClient.retrieveUser(userId)).user.twoFactor.methods.stream()
            .filter(m -> newUserPhone.equals(m.mobilePhone))
            .findAny()
            .orElseThrow();
    assertThat(method).isNotNull();

    String doomedPhone = user.getPhone().getEncrypted();
    deleteMethod(user, userCookie, doomedPhone, TwoFactorAuthenticationMethod.sms, false);
    assertThat(
            validateResponse(fusionAuthClient.retrieveUser(userId)).user.twoFactor.methods.stream()
                .noneMatch(m -> doomedPhone.equals(m.mobilePhone)))
        .isTrue();
  }

  @Test
  @SneakyThrows
  void twoFactorChangeEmail() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    User user = createBusinessRecord.user();

    // Enable 2FA
    Cookie userCookie = twoFactorSetup(user);

    // First, register the user's verified address
    addMethod(
        user,
        userCookie,
        user.getEmail().getEncrypted(),
        TwoFactorAuthenticationMethod.email,
        user.getPhone().getEncrypted());

    final String newEmail = testHelper.generateEmail();
    final UUID userId = UUID.fromString(user.getSubjectRef());
    addMethod(user, userCookie, newEmail, TwoFactorAuthenticationMethod.email, null);
    TwoFactorMethod method =
        validateResponse(fusionAuthClient.retrieveUser(userId)).user.twoFactor.methods.stream()
            .filter(m -> newEmail.equals(m.email))
            .findAny()
            .orElseThrow();
    assertThat(method).isNotNull();

    String doomedEmail = user.getEmail().getEncrypted();
    deleteMethod(user, userCookie, doomedEmail, TwoFactorAuthenticationMethod.email, false);
    assertThat(
            validateResponse(fusionAuthClient.retrieveUser(userId)).user.twoFactor.methods.stream()
                .noneMatch(m -> doomedEmail.equals(m.email)))
        .isTrue();
  }

  @Test
  @SneakyThrows
  void twoFactorAddNumberMissingNumber400() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    User user = createBusinessRecord.user();

    // Enable 2FA
    Cookie userCookie = twoFactorSetup(user);

    final String newUserPhone = null;

    ChangeMethodRequest changeRequest =
        new ChangeMethodRequest(
            user.getId(),
            user.getBusinessId(),
            newUserPhone,
            TwoFactorAuthenticationMethod.sms,
            null,
            null,
            null);
    MockHttpServletResponse response =
        mvc.perform(
                patch("/authentication/two-factor/method")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(changeRequest))
                    .cookie(userCookie))
            .andReturn()
            .getResponse();

    assertThat(response.getStatus()).isEqualTo(400);
  }

  private <T> T validateResponse(ClientResponse<T, Errors> response) {
    return FusionAuthService.validateResponse(response);
  }

  @SneakyThrows
  private void addMethod(
      User user,
      Cookie userCookie,
      String newDestination,
      TwoFactorAuthenticationMethod method,
      String stepUpCodeDestination) {
    TestFusionAuthClient faClient = (TestFusionAuthClient) fusionAuthClient;
    ChangeMethodRequest changeRequest =
        new ChangeMethodRequest(
            Optional.ofNullable(user).map(User::getId).orElse(null),
            Optional.ofNullable(user).map(User::getBusinessId).orElse(null),
            newDestination,
            method,
            null,
            null,
            null);
    MockHttpServletResponse response =
        mvc.perform(
                patch("/authentication/two-factor/method")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(changeRequest))
                    .cookie(userCookie))
            .andReturn()
            .getResponse();

    if (response.getStatus() == 242) {
      stepUp(userCookie, response, stepUpCodeDestination, HttpMethod.PATCH);
    } else {
      assertThat(response.getStatus()).isEqualTo(204);
    }

    final String twoFactorCodeForEnable = faClient.getLastCode(newDestination);
    validateResponse(
        enable2FA(userCookie, twoFactorCodeForEnable, newDestination, method),
        TwoFactorResponse.class);
  }

  @SneakyThrows
  private void stepUp(
      Cookie userCookie,
      MockHttpServletResponse firstResponse,
      String stepUpCodeDestination,
      HttpMethod method) {
    TestFusionAuthClient faClient = (TestFusionAuthClient) fusionAuthClient;

    assertThat(firstResponse.getStatus()).isEqualTo(242);

    TwoFactorStartResponse twoFactorStartLoggedInResponse =
        objectMapper.readValue(firstResponse.getContentAsString(), TwoFactorStartResponse.class);

    ChangeMethodRequest changeRequest =
        new ChangeMethodRequest(
            null,
            null,
            null,
            null,
            twoFactorStartLoggedInResponse.trustChallenge(),
            twoFactorStartLoggedInResponse.twoFactorId(),
            faClient.getLastCode(stepUpCodeDestination));

    MockHttpServletResponse response =
        mvc.perform(
                request(method, "/authentication/two-factor/method")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(changeRequest))
                    .cookie(userCookie))
            .andReturn()
            .getResponse();

    assertThat(response.getStatus()).isEqualTo(204);
    assertThat(response.getContentLength()).isEqualTo(0);
  }

  /**
   * @param user null for current user
   * @param userCookie for the user whose number is going away
   * @param doomedDestination the plain text phone number, E.164 format
   * @param method
   * @param stepUp true if this call should require a step-up before the actual change
   */
  @SneakyThrows
  private void deleteMethod(
      User user,
      Cookie userCookie,
      String doomedDestination,
      TwoFactorAuthenticationMethod method,
      boolean stepUp) {
    TestFusionAuthClient faClient = (TestFusionAuthClient) fusionAuthClient;
    ChangeMethodRequest changeRequest =
        new ChangeMethodRequest(
            Optional.ofNullable(user).map(User::getId).orElse(null),
            Optional.ofNullable(user).map(User::getBusinessId).orElse(null),
            doomedDestination,
            method,
            null,
            null,
            null);
    MockHttpServletResponse response = null;
    if (stepUp) {
      response =
          mvc.perform(
                  delete("/authentication/two-factor/method")
                      .contentType("application/json")
                      .content(objectMapper.writeValueAsString(changeRequest))
                      .cookie(userCookie))
              .andReturn()
              .getResponse();

      assertThat(response.getStatus()).isEqualTo(242);

      TwoFactorStartResponse twoFactorStartLoggedInResponse =
          objectMapper.readValue(response.getContentAsString(), TwoFactorStartResponse.class);

      changeRequest =
          new ChangeMethodRequest(
              Optional.ofNullable(user).map(User::getId).orElse(null),
              Optional.ofNullable(user).map(User::getBusinessId).orElse(null),
              null,
              null,
              faClient.getLastCode(doomedDestination),
              twoFactorStartLoggedInResponse.twoFactorId(),
              twoFactorStartLoggedInResponse.trustChallenge());
    }
    response =
        mvc.perform(
                delete("/authentication/two-factor/method")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(changeRequest))
                    .cookie(userCookie))
            .andReturn()
            .getResponse();

    assertThat(response.getStatus()).isEqualTo(204);
    assertThat(response.getContentLength()).isEqualTo(0);
  }

  @NotNull
  private Cookie twoFactorLogin(String username, String password, String phone) throws Exception {
    TestFusionAuthClient faClient = (TestFusionAuthClient) fusionAuthClient;
    String twoFactorId = login(username, password).getTwoFactorId();
    Cookie authCookie =
        complete2FALogin(twoFactorId, faClient.getLastCode(phone))
            .getCookie(SecurityConfig.ACCESS_TOKEN_COOKIE_NAME);
    assertNotNull(authCookie);
    return authCookie;
  }

  private void validateCookie(Cookie authCookie) throws Exception {
    MockHttpServletResponse response;
    response = getCurrentUser(authCookie);
    assertThat(response.getStatus()).isEqualTo(200);
  }

  private Set<String> makeSet(String one, List<String> others) {
    return Stream.concat(Stream.of(one), others.stream()).collect(Collectors.toSet());
  }

  @NotNull
  private String generateWrongCode(Supplier<String> generator, Set<String> exclusionsSet) {
    String wrong = generator.get();
    while (exclusionsSet.contains(wrong)) {
      wrong = generator.get();
    }
    return wrong;
  }

  private MockHttpServletResponse complete2FALogin(String twoFactorId, String twoFactorCode)
      throws Exception {
    TwoFactorLoginRequest twoFactorLoginRequest = new TwoFactorLoginRequest();
    twoFactorLoginRequest.code = twoFactorCode;
    twoFactorLoginRequest.twoFactorId = twoFactorId;

    return mvc.perform(
            post("/authentication/two-factor/login")
                .contentType("application/json")
                .content(objectMapper.writeValueAsString(twoFactorLoginRequest)))
        .andReturn()
        .getResponse();
  }

  private UserLoginResponse login(String username, String password) throws Exception {
    MockHttpServletResponse response;
    LoginRequest request = new LoginRequest(username, password);
    String body = objectMapper.writeValueAsString(request);

    response =
        mvc.perform(post("/authentication/login").contentType("application/json").content(body))
            .andReturn()
            .getResponse();

    log.info("response: {}", response);
    return validateResponse(response, UserLoginResponse.class);
  }

  @NotNull
  private MockHttpServletResponse begin2FASetup(Cookie userCookie, String userPhone)
      throws Exception {
    FirstTwoFactorSendRequest firstTwoFactorSendRequest =
        new FirstTwoFactorSendRequest(userPhone, TwoFactorAuthenticationMethod.sms);
    MockHttpServletResponse response =
        mvc.perform(
                post("/authentication/two-factor/first/send")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(firstTwoFactorSendRequest))
                    .cookie(userCookie))
            .andReturn()
            .getResponse();

    log.info("response: {}", response);
    return response;
  }

  @NotNull
  private TwoFactorStartResponse start2FALoggedIn(Cookie userCookie) throws Exception {
    MockHttpServletResponse response =
        mvc.perform(
                post("/authentication/two-factor/start")
                    .contentType("application/json")
                    .cookie(userCookie))
            .andReturn()
            .getResponse();

    log.info("response: {}", response);
    return validateResponse(response, TwoFactorStartResponse.class);
  }

  private void disable2FA(Cookie userCookie, String code, String methodId) throws Exception {

    MockHttpServletResponse response =
        mvc.perform(
                delete("/authentication/two-factor")
                    .contentType("application/json")
                    .param("code", code)
                    .param("methodId", methodId)
                    .cookie(userCookie))
            .andReturn()
            .getResponse();

    log.info("response: {}", response);
    validateResponse(response, Void.class);
  }

  @NotNull
  private MockHttpServletResponse enable2FA(
      Cookie userCookie,
      String twoFactorCodeForEnable,
      String userPhone,
      TwoFactorAuthenticationMethod method)
      throws Exception {
    MockHttpServletResponse response;
    FirstTwoFactorValidateRequest firstTwoFactorValidateRequest =
        new FirstTwoFactorValidateRequest(twoFactorCodeForEnable, method, userPhone);

    response =
        mvc.perform(
                post("/authentication/two-factor/first/validate")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(firstTwoFactorValidateRequest))
                    .cookie(userCookie))
            .andReturn()
            .getResponse();

    log.info("response: {}", response);
    return response;
  }

  private MockHttpServletResponse changePassword(
      User user,
      Cookie userCookie,
      String trustChallenge,
      String twoFactorCode,
      String twoFactorId,
      String currentPassword,
      String newPassword)
      throws Exception {
    MockHttpServletResponse response;
    ChangePasswordRequest changePasswordRequest =
        new ChangePasswordRequest(
            Optional.ofNullable(user).map(User::getId).orElse(null),
            currentPassword,
            newPassword,
            trustChallenge,
            twoFactorCode,
            twoFactorId);

    response =
        mvc.perform(
                post("/authentication/change-password")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(changePasswordRequest))
                    .cookie(userCookie))
            .andReturn()
            .getResponse();

    log.info("response: {}", response);
    return response;
  }

  @NotNull
  private MockHttpServletResponse getCurrentUser(Cookie userCookie) throws Exception {
    MockHttpServletResponse response;

    response =
        mvc.perform(get("/users").contentType("application/json").cookie(userCookie))
            .andReturn()
            .getResponse();

    log.info("response: {}", response);
    return response;
  }

  @SneakyThrows
  private <T> T validateResponse(MockHttpServletResponse response, Class<T> clazz) {
    if (response.getStatus() > 299) {
      Errors errors = null;
      try {
        errors = objectMapper.readValue(response.getContentAsString(), Errors.class);
      } catch (Exception e) {
        // pass
      }
      throw new FusionAuthException(response.getStatus(), errors);
    }
    if (clazz.equals(Void.class) || clazz.equals(Void.TYPE)) {
      return null;
    }
    return objectMapper.readValue(response.getContentAsString(), clazz);
  }
}
