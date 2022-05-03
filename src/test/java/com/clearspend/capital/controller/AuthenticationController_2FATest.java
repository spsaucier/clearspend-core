package com.clearspend.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.common.error.FusionAuthException;
import com.clearspend.capital.configuration.SecurityConfig;
import com.clearspend.capital.controller.AuthenticationController.FirstTwoFactorSendRequest;
import com.clearspend.capital.controller.AuthenticationController.FirstTwoFactorValidateRequest;
import com.clearspend.capital.controller.AuthenticationController.TwoFactorStartLoggedInResponse;
import com.clearspend.capital.controller.security.TestFusionAuthClient;
import com.clearspend.capital.controller.type.user.ChangePasswordRequest;
import com.clearspend.capital.controller.type.user.LoginRequest;
import com.clearspend.capital.controller.type.user.UserLoginResponse;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.service.FusionAuthService.TwoFactorAuthenticationMethod;
import com.clearspend.capital.service.UserService;
import com.inversoft.error.Errors;
import io.fusionauth.client.FusionAuthClient;
import io.fusionauth.domain.api.TwoFactorResponse;
import io.fusionauth.domain.api.twoFactor.TwoFactorLoginRequest;
import java.util.List;
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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class AuthenticationController_2FATest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final FusionAuthClient fusionAuthClient;
  private final UserService userService;

  @Test
  @SneakyThrows
  void twoFactorLogin() {
    // This test reads like a bumbling user, trying things that don't work first, then
    // things that should work.  It's easier than writing a slew of tests that
    // set up the various scenarios where things break.
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    TestFusionAuthClient faClient = (TestFusionAuthClient) fusionAuthClient;
    User user = createBusinessRecord.user();

    final UUID userId = UUID.fromString(user.getSubjectRef());

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
    final String twoFactorCodeForEnable = faClient.getTwoFactorCodeForEnable(userId);

    String wrong2FCodeForEnable =
        generateWrongCode(() -> RandomStringUtils.randomNumeric(6), Set.of(twoFactorCodeForEnable));

    response = enable2FA(userCookie, wrong2FCodeForEnable, userPhone);
    assertEquals(421, response.getStatus());

    List<String> recoveryCodes =
        validateResponse(
                enable2FA(userCookie, twoFactorCodeForEnable, userPhone), TwoFactorResponse.class)
            .recoveryCodes;

    // 2FA is enabled, now try logging in
    final String username = user.getEmail().getEncrypted();
    final String password = testHelper.getPassword(user);

    UserLoginResponse twoFactorAuthenticationStart = login(username, password);

    final String twoFactorId = twoFactorAuthenticationStart.getTwoFactorId();
    String twoFactorCodeForLogin = faClient.getTwoFactorCodeForLogin(twoFactorId);
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

    // Check that the authCookie works
    assertNotNull(authCookie);
    validateCookie(authCookie);

    // Gearing up for password change - generate a new password
    String newPassword = RandomStringUtils.randomAlphanumeric(10);

    // step up 2FA for password change
    MockHttpServletResponse changePasswordResponse =
        changePassword(authCookie, null, null, null, password, newPassword);
    assertThat(changePasswordResponse.getStatus()).isEqualTo(242);
    TwoFactorStartLoggedInResponse twoFactorStartLoggedInResponse =
        validateResponse(changePasswordResponse, TwoFactorStartLoggedInResponse.class);

    // change the password
    String twoFactorCode =
        faClient.getTwoFactorCodeForLogin(twoFactorStartLoggedInResponse.twoFactorId());
    MockHttpServletResponse realChangeResponse =
        changePassword(
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
    authCookie = twoFactorLogin(username, newPassword);
    validateCookie(authCookie);

    // Start to disable 2FA
    @NotNull TwoFactorStartLoggedInResponse startResponse = start2FALoggedIn(authCookie);
    final String twoFactorCodeForDisable =
        faClient.getTwoFactorCodeForLogin(startResponse.twoFactorId());

    // Try to disable with the wrong code
    disable2FA(
        userCookie,
        generateWrongCode(
            () -> RandomStringUtils.randomNumeric(6),
            makeSet(twoFactorCodeForDisable, recoveryCodes)),
        startResponse.methodId());

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

  @NotNull
  private Cookie twoFactorLogin(String username, String password) throws Exception {
    TestFusionAuthClient faClient = (TestFusionAuthClient) fusionAuthClient;
    String twoFactorId = login(username, password).getTwoFactorId();
    Cookie authCookie =
        complete2FALogin(twoFactorId, faClient.getTwoFactorCodeForLogin(twoFactorId))
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
  private TwoFactorStartLoggedInResponse start2FALoggedIn(Cookie userCookie) throws Exception {
    MockHttpServletResponse response =
        mvc.perform(
                post("/authentication/two-factor/start")
                    .contentType("application/json")
                    .cookie(userCookie))
            .andReturn()
            .getResponse();

    log.info("response: {}", response);
    return validateResponse(response, TwoFactorStartLoggedInResponse.class);
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
      Cookie userCookie, String twoFactorCodeForEnable, String userPhone) throws Exception {
    MockHttpServletResponse response;
    FirstTwoFactorValidateRequest firstTwoFactorValidateRequest =
        new FirstTwoFactorValidateRequest(
            twoFactorCodeForEnable, TwoFactorAuthenticationMethod.sms, userPhone);

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
            currentPassword, newPassword, trustChallenge, twoFactorCode, twoFactorId);

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
