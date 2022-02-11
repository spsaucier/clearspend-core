package com.clearspend.capital.controller;

import com.clearspend.capital.common.error.LoginException;
import com.clearspend.capital.configuration.SecurityConfig;
import com.clearspend.capital.controller.type.user.ForgotPasswordRequest;
import com.clearspend.capital.controller.type.user.LoginRequest;
import com.clearspend.capital.controller.type.user.ResetPasswordRequest;
import com.clearspend.capital.controller.type.user.User;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.service.BusinessOwnerService;
import com.clearspend.capital.service.BusinessProspectService;
import com.clearspend.capital.service.FusionAuthService;
import com.clearspend.capital.service.UserService;
import com.clearspend.capital.service.type.CurrentUser;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.inversoft.error.Errors;
import com.inversoft.rest.ClientResponse;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import io.fusionauth.domain.api.LoginResponse;
import java.text.ParseException;
import java.time.Duration;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import javax.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
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

  @PostMapping("/login")
  public ResponseEntity<User> login(
      @Validated @RequestBody LoginRequest request, HttpServletRequest httpServletRequest)
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
    LoginResponse response = loginResponse.successResponse;

    // JWT is enriched according to populate-token.js
    final JWTClaimsSet jwtClaimsSet = JWTParser.parse(response.token).getJWTClaimsSet();

    // Status 202 = The user was authenticated successfully. The user is not registered for
    // the application specified by the applicationId on the request. The response will contain
    // the User object that was authenticated.
    if (loginResponse.status == 202) {
      // populate-token.js populates the JWT either way
      CurrentUser user = CurrentUser.get(jwtClaimsSet.getClaims());
      fusionAuthService.updateUser(
          user.businessId(),
          user.userId(),
          request.getUsername(),
          request.getPassword(),
          user.userType(),
          response.user.id.toString());
      loginResponse.status = 200;
    }

    String userId = response.user.id.toString();
    Long expiry;
    try {
      expiry =
          (((Date) jwtClaimsSet.getClaim("exp")).getTime() - System.currentTimeMillis()) / 1000;
    } catch (Exception e) {
      // not found or not parsed as expected
      expiry = 20L * 60; // 20 minutes
    }

    Optional<User> user = userService.retrieveUserBySubjectRef(userId).map(User::new);
    if (user.isEmpty()) {
      // If it is not found from "users" table, it may still exist in business_prospect
      // during the first steps of on-boarding phase
      user = businessProspectService.retrieveBusinessProspectBySubjectRef(userId).map(User::new);
    } else {
      // For special group of users =business owners= we want to load them
      // from business_owner table, as it contains important fields used during on-boarding
      if (user.get().getType() == UserType.BUSINESS_OWNER) {
        user = businessOwnerService.retrieveBusinessOwnerBySubjectRef(userId).map(User::new);
      }
    }

    return ResponseEntity.status(loginResponse.status)
        .header(
            HttpHeaders.SET_COOKIE,
            createCookie(SecurityConfig.ACCESS_TOKEN_COOKIE_NAME, response.token, expiry),
            createCookie(
                SecurityConfig.REFRESH_TOKEN_COOKIE_NAME,
                response.refreshToken,
                refreshTokenTimeToLiveInMinutes * 60L))
        .body(user.orElse(null));
  }

  @PostMapping("/logout")
  public ResponseEntity<?> logout() {
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
  public void forgotPassword(@Validated @RequestBody ForgotPasswordRequest request) {
    fusionAuthService.forgotPassword(request);
  }

  @PostMapping("/reset-password")
  public void resetPassword(@Validated @RequestBody ResetPasswordRequest request) {
    fusionAuthService.resetPassword(request);
  }

  private String createCookie(String name, String value, long ttl) {
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
