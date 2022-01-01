package com.clearspend.capital.controller;

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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

@Slf4j
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class AuthenticationControllerTest extends BaseCapitalTest {

  private final MockMvc mvc;
  private final TestHelper testHelper;
  private final TwilioServiceMock twilioServiceMock;

  @AfterEach
  void resetSendGridMock() {
    Mockito.reset(twilioServiceMock.getSendGrid());
  }

  @Test
  @SneakyThrows
  void forgotPassword() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();

    twilioServiceMock.expectResetPassword();

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
        new ResetPasswordRequest(twilioServiceMock.getChangePasswordId(), "Qwerty12345!");
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

    Mockito.verifyNoInteractions(twilioServiceMock.getSendGrid());
  }

  @Test
  @SneakyThrows
  void invalidChangePasswordId() {
    CreateBusinessRecord createBusinessRecord = testHelper.createBusiness();

    twilioServiceMock.expectResetPassword();

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
        new ResetPasswordRequest(twilioServiceMock.getChangePasswordId() + "123", "Qwerty12345!");
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
