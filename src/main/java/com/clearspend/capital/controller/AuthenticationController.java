package com.clearspend.capital.controller;

import com.clearspend.capital.common.error.FusionAuthException;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.configuration.SecurityConfig;
import com.clearspend.capital.controller.type.user.ChangePasswordRequest;
import com.clearspend.capital.controller.type.user.ForgotPasswordRequest;
import com.clearspend.capital.controller.type.user.LoginRequest;
import com.clearspend.capital.controller.type.user.ResetPasswordRequest;
import com.clearspend.capital.controller.type.user.UserLoginResponse;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.service.BusinessOwnerService;
import com.clearspend.capital.service.BusinessOwnerService.LoginBusinessOwner;
import com.clearspend.capital.service.BusinessProspectService;
import com.clearspend.capital.service.BusinessProspectService.AuthenticationBusinessProspectMethod;
import com.clearspend.capital.service.BusinessService;
import com.clearspend.capital.service.BusinessService.PreLoginOperation;
import com.clearspend.capital.service.FusionAuthService;
import com.clearspend.capital.service.FusionAuthService.FusionAuthUserAccessor;
import com.clearspend.capital.service.FusionAuthService.FusionAuthUserModifier;
import com.clearspend.capital.service.FusionAuthService.TwoFactorAuthenticationMethod;
import com.clearspend.capital.service.UserService;
import com.clearspend.capital.service.UserService.LoginUserOp;
import com.clearspend.capital.service.type.CurrentUser;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inversoft.error.Errors;
import com.inversoft.rest.ClientResponse;
import com.nimbusds.jwt.JWTParser;
import io.fusionauth.domain.TwoFactorMethod;
import io.fusionauth.domain.api.LoginResponse;
import io.fusionauth.domain.api.TwoFactorResponse;
import io.fusionauth.domain.api.twoFactor.TwoFactorLoginRequest;
import io.fusionauth.domain.api.user.ChangePasswordResponse;
import io.swagger.v3.oas.annotations.Parameter;
import java.text.ParseException;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/authentication")
public class AuthenticationController {

  private final BusinessProspectService businessProspectService;
  private final BusinessOwnerService businessOwnerService;
  private final BusinessService businessService;
  private final FusionAuthService fusionAuthService;
  private final int refreshTokenTimeToLiveInMinutes;
  private final UserService userService;

  public AuthenticationController(
      BusinessProspectService businessProspectService,
      BusinessOwnerService businessOwnerService,
      BusinessService businessService,
      FusionAuthService fusionAuthService,
      UserService userService) {
    this.businessProspectService = businessProspectService;
    this.businessOwnerService = businessOwnerService;
    this.businessService = businessService;
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
      throws ParseException, FusionAuthException {
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
      throw new FusionAuthException(loginResponse.status, loginResponse.errorResponse);
    }

    if (loginResponse.status == 203) {
      return UserLoginResponse.resetPasswordResponse(
          loginResponse.successResponse.changePasswordId,
          loginResponse.successResponse.changePasswordReason);
    }

    if (loginResponse.status == 242) {
      fusionAuthService.sendTwoFactorCodeUsingMethod(
          loginResponse.successResponse.twoFactorId,
          loginResponse.successResponse.methods.stream()
              .filter(m -> m.method.equals(TwoFactorMethod.SMS))
              .findFirst()
              .orElseThrow()
              .id);
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
  @LoginUserOp(
      reviewer = "Craig Miller",
      explanation =
          "User information must be looked up here, but there is no SecurityContext available to secure the method yet.")
  @LoginBusinessOwner(
      reviewer = "Craig Miller",
      explanation =
          "Business owner info is looked up here, but no SecurityContext is available to secure the method yet.")
  @AuthenticationBusinessProspectMethod(
      reviewer = "Craig Miller",
      explanation =
          "Need to lookup Business Prospect info, but SecurityContext is not available yet")
  @PreLoginOperation(
      reviewer = "Patrick Morton",
      explanation = "Retrieve the Business entity prior to login for Status check")
  @SuppressWarnings("JavaUtilDate")
  public ResponseEntity<UserLoginResponse> finalizeLogin(
      ClientResponse<LoginResponse, Errors> loginResponse) throws ParseException {
    LoginResponse response = loginResponse.successResponse;

    CurrentUser user = CurrentUser.get(getClaims(response));

    try {
      Business business = businessService.retrieveBusinessPriorToLogin(user.businessId(), false);
      if (business != null && business.getStatus() == BusinessStatus.SUSPENDED) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
      }
    } catch (RecordNotFoundException rnfe) {
      log.debug("Unable to locate business during login", rnfe);
    }

    // Status 202 = The user was authenticated successfully. The user is not registered for
    // the application specified by the applicationId on the request. The response will contain
    // the User object that was authenticated.
    if (loginResponse.status == 202) {
      // populate-token.js populates the JWT
      fusionAuthService.updateUser(
          user.businessId(),
          user.userId(),
          null,
          null,
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

  @FusionAuthUserAccessor(
      explanation = "Accessing the user through 2FA as designed",
      reviewer = "jscarbor")
  @PostMapping("/two-factor/login")
  ResponseEntity<UserLoginResponse> twoFactorLogin(
      @Validated @RequestBody TwoFactorLoginRequest request) throws ParseException {
    ClientResponse<LoginResponse, Errors> loginResponse = fusionAuthService.twoFactorLogin(request);
    return finalizeLogin(loginResponse);
  }

  record FirstTwoFactorSendRequest(String destination, TwoFactorAuthenticationMethod method) {}

  @FusionAuthUserModifier(
      explanation = "Modifying the user to enable 2FA from AuthenticationController by design",
      reviewer = "jscarbor")
  @PostMapping("/two-factor/first/send")
  void firstTwoFactorSend(
      @Validated @RequestBody FirstTwoFactorSendRequest firstTwoFactorSendRequest) {
    fusionAuthService.sendInitialTwoFactorCode(
        CurrentUser.getFusionAuthUserId(),
        firstTwoFactorSendRequest.method,
        firstTwoFactorSendRequest.destination);
  }

  record FirstTwoFactorValidateRequest(
      String code, TwoFactorAuthenticationMethod method, String destination) {}

  @FusionAuthUserModifier(
      explanation = "Modifying the user to enable 2FA from AuthenticationController by design",
      reviewer = "jscarbor")
  @PostMapping("/two-factor/first/validate")
  TwoFactorResponse firstTwoFactorValidate(
      @Validated @RequestBody FirstTwoFactorValidateRequest firstTwoFactorValidateRequest) {
    return fusionAuthService.validateFirstTwoFactorCode(
        CurrentUser.getFusionAuthUserId(),
        firstTwoFactorValidateRequest.code,
        firstTwoFactorValidateRequest.method,
        firstTwoFactorValidateRequest.destination);
  }

  /**
   * - * Some 2FA actions are done while the user is logged in. These have a twoFactorId and
   * possibly a - * methodId which could be needed for follow-up with the code. -
   */
  public record TwoFactorStartLoggedInResponse(
      String twoFactorId, String methodId, String trustChallenge) {

    static TwoFactorStartLoggedInResponse of(FusionAuthService.TwoFactorStartLoggedInResponse r) {
      return new TwoFactorStartLoggedInResponse(r.twoFactorId(), r.methodId(), r.trustChallenge());
    }
  }

  /**
   * This is for things requiring a very fresh code, such as disabling 2FA and changing password.
   * This is not how a 2FA login begins - that begins the same as every other login.
   *
   * @return codes required to proceed
   */
  @FusionAuthUserModifier(reviewer = "jscarbor", explanation = "Starting to change the user")
  @PostMapping("/two-factor/start")
  TwoFactorStartLoggedInResponse sendCodeToBegin2FA() {
    return TwoFactorStartLoggedInResponse.of(
        fusionAuthService.sendCodeToBegin2FA(
            userService.retrieveUser(CurrentUser.getUserId()), Collections.emptyMap()));
  }

  /**
   * Disable a user's 2FA config
   *
   * @param methodId the method to be disabled. If a recovery code is provided, this can be any
   *     valid method Id, and all methods will be disabled.
   * @param code the 2FA code
   */
  @FusionAuthUserModifier(
      reviewer = "jscarbor",
      explanation = "Authentication Controller has responsibility for changing users")
  @DeleteMapping("/two-factor")
  void disable2FA(@RequestParam String methodId, @RequestParam String code) {
    fusionAuthService.disableTwoFactor(CurrentUser.getFusionAuthUserId(), methodId, code);
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

  @FusionAuthUserModifier(
      reviewer = "jscarbor",
      explanation = "Changing password, only for CurrentUser")
  @PostMapping(value = "/change-password", consumes = "application/json")
  ResponseEntity<TwoFactorStartLoggedInResponse> changePassword(
      @Validated @RequestBody ChangePasswordRequest request) {
    User user = userService.retrieveUser(CurrentUser.getUserId());
    FusionAuthService.TwoFactorStartLoggedInResponse response;
    try {
      response = fusionAuthService.changePassword(user, request.toFusionAuthRequest());
    } catch (InvalidRequestException e) {
      if ("Unauthorized".equals(e.getMessage())) {
        return ResponseEntity.status(401).build();
      }
      throw e;
    }
    return Optional.ofNullable(response)
        .map(r -> ResponseEntity.status(242).body(TwoFactorStartLoggedInResponse.of(r)))
        .orElse(ResponseEntity.status(204).build());
  }

  /** Change password from a change request */
  @FusionAuthUserModifier(
      reviewer = "jscarbor",
      explanation = "Changing password, ID ensures it's the same user doing it")
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
        CurrentUser.getEmail(),
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
