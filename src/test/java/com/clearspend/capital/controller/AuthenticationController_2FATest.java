package com.clearspend.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.configuration.SecurityConfig;
import com.clearspend.capital.controller.AuthenticationController.FirstTwoFactorSendRequest;
import com.clearspend.capital.controller.AuthenticationController.FirstTwoFactorValidateRequest;
import com.clearspend.capital.controller.security.TestFusionAuthClient;
import com.clearspend.capital.controller.type.user.LoginRequest;
import com.clearspend.capital.controller.type.user.UserLoginResponse;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.service.FusionAuthService.TwoFactorAuthenticationMethod;
import io.fusionauth.client.FusionAuthClient;
import io.fusionauth.domain.api.twoFactor.TwoFactorLoginRequest;
import java.util.UUID;
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

    String wrong2FCodeForEnable;
    do { // it is unlikely this loop will execute more than once, but it's for good measure.
      wrong2FCodeForEnable = RandomStringUtils.randomNumeric(6);
    } while (wrong2FCodeForEnable.equals(twoFactorCodeForEnable));

    response = enable2FA(userCookie, wrong2FCodeForEnable, userPhone);
    assertEquals(421, response.getStatus());

    response = enable2FA(userCookie, twoFactorCodeForEnable, userPhone);
    assertEquals(200, response.getStatus());

    // 2FA is enabled, now try logging in
    final String username = user.getEmail().getEncrypted();
    final String password = testHelper.getPassword(user);

    UserLoginResponse twoFactorAuthenticationStart = login(username, password);
    final String twoFactorId = twoFactorAuthenticationStart.getTwoFactorId();
    final String twoFactorCode = faClient.getTwoFactorCodeForLogin(twoFactorId);
    assertThat(twoFactorId).isNotNull();
    assertThat(twoFactorCode).isNotNull();

    // and confirm the 2F
    String wrong2FCode;
    do {
      wrong2FCode = RandomStringUtils.randomNumeric(6);
    } while (wrong2FCode.equals(twoFactorCode));

    String wrong2FId;
    do {
      wrong2FId = RandomStringUtils.randomAlphanumeric(13);
    } while (wrong2FId.equals(twoFactorId));

    // Try with a bad TwoFactorID and get a 404
    MockHttpServletResponse loginCompleteResponse = complete2FALogin(wrong2FId, twoFactorCode);
    assertThat(loginCompleteResponse.getStatus()).isEqualTo(404);

    // Try with a bad code and get 421
    loginCompleteResponse = complete2FALogin(twoFactorId, wrong2FCode);
    assertThat(loginCompleteResponse.getStatus()).isEqualTo(421);

    loginCompleteResponse = complete2FALogin(twoFactorId, twoFactorCode);
    Cookie authCookie = loginCompleteResponse.getCookie(SecurityConfig.ACCESS_TOKEN_COOKIE_NAME);
    assertNotNull(authCookie);
  }

  private MockHttpServletResponse complete2FALogin(String twoFactorId, String twoFactorCode)
      throws Exception {
    MockHttpServletResponse response;
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
            .andExpect(status().is(200))
            .andReturn()
            .getResponse();

    log.info("response: {}", response);
    return objectMapper.readValue(response.getContentAsString(), UserLoginResponse.class);
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
}
