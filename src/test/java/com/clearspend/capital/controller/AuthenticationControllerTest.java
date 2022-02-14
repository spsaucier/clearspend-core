package com.clearspend.capital.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.clearspend.capital.BaseCapitalTest;
import com.clearspend.capital.TestHelper;
import com.clearspend.capital.TestHelper.CreateBusinessRecord;
import com.clearspend.capital.client.twilio.TwilioServiceMock;
import com.clearspend.capital.controller.type.user.ForgotPasswordRequest;
import com.clearspend.capital.controller.type.user.ResetPasswordRequest;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class AuthenticationControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final TwilioServiceMock twilioServiceMock;

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
}
