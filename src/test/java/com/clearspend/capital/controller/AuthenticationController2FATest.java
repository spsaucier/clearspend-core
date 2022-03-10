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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class AuthenticationController2FATest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final FusionAuthClient fusionAuthClient;

  @Test
  @SneakyThrows
  void twoFactorLogin() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();
    TestFusionAuthClient faClient = (TestFusionAuthClient) fusionAuthClient;
    User user = createBusinessRecord.user();

    final UUID userId = UUID.fromString(user.getSubjectRef());
    FirstTwoFactorSendRequest firstTwoFactorSendRequest =
        new FirstTwoFactorSendRequest(
            user.getPhone().getEncrypted(), TwoFactorAuthenticationMethod.sms);

    // Enable 2FA
    Cookie userCookie = createBusinessRecord.authCookie();

    MockHttpServletResponse response =
        mvc.perform(
                post("/authentication/two-factor/first/send")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(firstTwoFactorSendRequest))
                    .cookie(userCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    log.info("response: {}", response);
    assertEquals(200, response.getStatus());

    // Confirm receipt of the code to enable 2FA
    FirstTwoFactorValidateRequest firstTwoFactorValidateRequest =
        new FirstTwoFactorValidateRequest(
            faClient.getTwoFactorCodeForEnable(userId),
            TwoFactorAuthenticationMethod.sms,
            user.getPhone().getEncrypted());

    response =
        mvc.perform(
                post("/authentication/two-factor/first/validate")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(firstTwoFactorValidateRequest))
                    .cookie(userCookie))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse();

    log.info("response: {}", response);
    assertEquals(200, response.getStatus());

    // 2FA is enabled, now try logging in

    LoginRequest request =
        new LoginRequest(user.getEmail().getEncrypted(), testHelper.getPassword(user));
    String body = objectMapper.writeValueAsString(request);

    response =
        mvc.perform(post("/authentication/login").contentType("application/json").content(body))
            .andExpect(status().is(200))
            .andReturn()
            .getResponse();

    log.info("response: {}", response);
    UserLoginResponse twoFactorAuthenticationStart =
        objectMapper.readValue(response.getContentAsString(), UserLoginResponse.class);
    final String twoFactorCode =
        faClient.getTwoFactorCodeForLogin(
            UUID.fromString(twoFactorAuthenticationStart.getTwoFactorId()));
    assertThat(twoFactorCode).isNotNull();

    // and confirm the 2F

    TwoFactorLoginRequest twoFactorLoginRequest = new TwoFactorLoginRequest();

    twoFactorLoginRequest.code = twoFactorCode;
    twoFactorLoginRequest.twoFactorId = twoFactorAuthenticationStart.getTwoFactorId();

    response =
        mvc.perform(
                post("/authentication/two-factor/login")
                    .contentType("application/json")
                    .content(objectMapper.writeValueAsString(twoFactorLoginRequest)))
            .andExpect(status().is(200))
            .andReturn()
            .getResponse();

    Cookie authCookie = response.getCookie(SecurityConfig.ACCESS_TOKEN_COOKIE_NAME);
    assertNotNull(authCookie);
  }
}
