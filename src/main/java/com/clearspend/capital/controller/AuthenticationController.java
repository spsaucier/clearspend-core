package com.clearspend.capital.controller;

import com.clearspend.capital.common.error.LoginException;
import com.clearspend.capital.configuration.SecurityConfig;
import com.clearspend.capital.controller.type.user.ChangePasswordRequest;
import com.clearspend.capital.controller.type.user.ForgotPasswordRequest;
import com.clearspend.capital.controller.type.user.LoginRequest;
import com.clearspend.capital.controller.type.user.ResetPasswordRequest;
import com.clearspend.capital.controller.type.user.UserLoginResponse;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.service.BusinessOwnerService;
import com.clearspend.capital.service.BusinessProspectService;
import com.clearspend.capital.service.FusionAuthService;
import com.clearspend.capital.service.FusionAuthService.FusionAuthUserModifier;
import com.clearspend.capital.service.FusionAuthService.TwoFactorAuthenticationMethod;
import com.clearspend.capital.service.UserService;
import com.clearspend.capital.service.type.CurrentUser;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inversoft.error.Errors;
import com.inversoft.rest.ClientResponse;
import com.nimbusds.jwt.JWTParser;
import io.fusionauth.domain.api.LoginResponse;
import io.fusionauth.domain.api.TwoFactorResponse;
import io.fusionauth.domain.api.twoFactor.TwoFactorLoginRequest;
import io.fusionauth.domain.api.user.ChangePasswordResponse;
import io.swagger.v3.oas.annotations.Parameter;
import java.text.ParseException;
import java.time.Duration;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/authentication")
public class AuthenticationController {

  private final BusinessProspectService businessProspectService;
  private final BusinessOwnerService businessOwnerService;
  private final FusionAuthService fusionAuthService;
  private final int refreshTokenTimeToLiveInMinutes;
  private final UserService userService;

  public AuthenticationController(
      BusinessProspectService businessProspectService,
      BusinessOwnerService businessOwnerService,
      FusionAuthService fusionAuthService,
      UserService userService) {
    this.businessProspectService = businessProspectService;
    this.businessOwnerService = businessOwnerService;
    this.fusionAuthService = fusionAuthService;
    this.userService = userService;
    refreshTokenTimeToLiveInMinutes =
        fusionAuthService.getApplication().jwtConfiguration.refreshTokenTimeToLiveInMinutes;
  }

  @FusionAuthUserModifier(
      reviewer = "jscarbor",
      explanation = "Migrates user to having a registration in FusionAuth upon FA's login response")
  @PostMapping("/login")
  ResponseEntity<UserLoginResponse> login(@Validated @RequestBody LoginRequest request)
      throws ParseException, LoginException {
    ClientResponse<LoginResponse, Errors> loginResponse;
    try {
      loginResponse = fusionAuthService.login(request.getUsername(), request.getPassword());
      if (loginResponse == null) {
        throw new RuntimeException("Received empty response from fusion auth");
      }
    } catch (Exception e) {
      // making it debug level to prevent incorrect login/password trace spamming
      log.debug("Failed to get fusion auth token response", e);
      return null;
    }
    if (!loginResponse.wasSuccessful()) {
      throw new LoginException(loginResponse.status, loginResponse.errorResponse);
    }

    if (loginResponse.status == 203) {
      return UserLoginResponse.resetPasswordResponse(
          loginResponse.successResponse.changePasswordId,
          loginResponse.successResponse.changePasswordReason);
    }

    if (loginResponse.status == 242) {
      return UserLoginResponse.twoFactorChallenge(loginResponse.successResponse.twoFactorId);
    }

    return finalizeLogin(loginResponse);
  }

  @FusionAuthUserModifier(
      reviewer = "jscarbor",
      explanation =
          """
              For migration purposes.  The user should have been registered with the app earlier
              but was not because the user service didn't do the registration earlier.   A case could
              be made to move this functionality to the user service or remove it entirely.
              Removal after March 2022 is probably appropriate.
              """)
  public ResponseEntity<UserLoginResponse> finalizeLogin(
      ClientResponse<LoginResponse, Errors> loginResponse) throws ParseException {
    LoginResponse response = loginResponse.successResponse;

    // Status 202 = The user was authenticated successfully. The user is not registered for
    // the application specified by the applicationId on the request. The response will contain
    // the User object that was authenticated.
    CurrentUser user = CurrentUser.get(getClaims(response));
    if (loginResponse.status == 202) {
      // populate-token.js populates the JWT
      fusionAuthService.updateUser(
          user.businessId(),
          user.userId(),
          Optional.empty(),
          Optional.empty(),
          user.userType(),
          response.user.id.toString());
      loginResponse.status = 200;
    }

    String userId = response.user.id.toString();
    long expiry;
    try {
      expiry =
          (((Date) getClaims(response).get("exp")).getTime() - System.currentTimeMillis()) / 1000;
    } catch (Exception e) {
      // not found or not parsed as expected
      expiry = 20L * 60; // 20 minutes
    }

    UserLoginResponse userLoginResponse =
        userService
            .retrieveUserBySubjectRef(userId)
            .map(UserLoginResponse::new)
            // If it is not found from "users" table, it may still exist in business_prospect
            // during the first steps of on-boarding phase
            .orElse(
                businessProspectService
                    .retrieveBusinessProspectBySubjectRef(userId)
                    .map(UserLoginResponse::new)
                    .orElseGet(
                        () -> {
                          // For special group of users =business owners= we want to load them
                          // from business_owner table, as it contains important fields used during
                          // on-boarding

                          if (user.userType() == UserType.BUSINESS_OWNER) {
                            return businessOwnerService
                                .retrieveBusinessOwnerBySubjectRef(userId)
                                .map(UserLoginResponse::new)
                                .orElse(null);
                          }
                          return null;
                        }));

    return ResponseEntity.status(loginResponse.status)
        .header(
            HttpHeaders.SET_COOKIE,
            createCookie(SecurityConfig.ACCESS_TOKEN_COOKIE_NAME, response.token, expiry),
            createCookie(
                SecurityConfig.REFRESH_TOKEN_COOKIE_NAME,
                response.refreshToken,
                refreshTokenTimeToLiveInMinutes * 60L))
        .body(userLoginResponse);
  }

  private Map<String, Object> getClaims(LoginResponse response) throws ParseException {
    if (response.token == null) {
      throw new NullPointerException("response.token is null");
    }
    return JWTParser.parse(response.token).getJWTClaimsSet().getClaims();
  }

  @PostMapping("/two-factor/login")
  ResponseEntity<UserLoginResponse> twoFactorLogin(
      @Validated @RequestBody TwoFactorLoginRequest request) throws ParseException {
    ClientResponse<LoginResponse, Errors> loginResponse = fusionAuthService.twoFactorLogin(request);
    return finalizeLogin(loginResponse);
  }

  record FirstTwoFactorSendRequest(String destination, TwoFactorAuthenticationMethod method) {}

  @PostMapping("/two-factor/first/send")
  void firstTwoFactorSend(
      @Validated @RequestBody FirstTwoFactorSendRequest firstTwoFactorSendRequest) {
    fusionAuthService.sendInitialTwoFactorCode(
        UUID.fromString(userService.retrieveUser(CurrentUser.getUserId()).getSubjectRef()),
        firstTwoFactorSendRequest.method,
        firstTwoFactorSendRequest.destination);
  }

  record FirstTwoFactorValidateRequest(
      String code, TwoFactorAuthenticationMethod method, String destination) {}

  @PostMapping("/two-factor/first/validate")
  TwoFactorResponse firstTwoFactorValidate(
      @Validated @RequestBody FirstTwoFactorValidateRequest firstTwoFactorValidateRequest) {
    return fusionAuthService.validateFirstTwoFactorCode(
        UUID.fromString(userService.retrieveUser(CurrentUser.getUserId()).getSubjectRef()),
        firstTwoFactorValidateRequest.code,
        firstTwoFactorValidateRequest.method,
        firstTwoFactorValidateRequest.destination);
  }

  @PostMapping("/logout")
  ResponseEntity<?> logout() {
    // Looks like we cannot just revoke token from Fusion Auth so might need some revoked
    // tokens storage some time later to keep tokens before they actually expire
    return ResponseEntity.ok()
        .header(
            HttpHeaders.SET_COOKIE,
            createCookie(SecurityConfig.ACCESS_TOKEN_COOKIE_NAME, StringUtils.EMPTY, 0),
            createCookie(SecurityConfig.REFRESH_TOKEN_COOKIE_NAME, StringUtils.EMPTY, 0))
        .build();
  }

  @PostMapping("/forgot-password")
  void forgotPassword(@Validated @RequestBody ForgotPasswordRequest request) {
    fusionAuthService.forgotPassword(request);
  }

  @PostMapping("/reset-password")
  void resetPassword(@Validated @RequestBody ResetPasswordRequest request) {
    fusionAuthService.resetPassword(request);
  }

  @PostMapping("/change-password")
  void changePassword(@Validated @RequestBody ChangePasswordRequest request) {
    fusionAuthService.changePassword(
        request.getUsername(), request.getCurrentPassword(), request.getNewPassword());
  }

  @PostMapping("/change-password/{changePasswordId}")
  ChangePasswordResponse changePassword(
      @PathVariable(value = "changePasswordId")
          @Parameter(
              required = true,
              name = "changePasswordId",
              description =
                  "a token presented to the user upon attempted login when password must be changed immediately",
              example = "!51m3e#P44tsTh!")
          String changePasswordId,
      @Validated @RequestBody ChangePasswordRequest request) {
    return fusionAuthService.changePassword(
        changePasswordId,
        request.getUsername(),
        request.getCurrentPassword(),
        request.getNewPassword());
  }

  String createCookie(String name, String value, long ttl) {
    HttpCookie authTokenCookie =
        ResponseCookie.from(name, value)
            .path("/")
            .secure(true)
            .httpOnly(true)
            .maxAge(Duration.ofSeconds(ttl))
            .build();
    return authTokenCookie.toString();
  }

  @Data
  public static final class AccessTokenResponse {

    @JsonProperty("access_token")
    private String accessToken;

    @JsonProperty("refresh_token")
    private String refreshToken;

    @JsonProperty("expires_in")
    private Long expiresIn;

    @JsonProperty("token_type")
    private String tokenType;

    @JsonProperty("userId")
    private UUID userId;
  }
}
