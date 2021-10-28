package com.tranwall.capital.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.tranwall.capital.configuration.SecurityConfig;
import com.tranwall.capital.controller.type.user.LoginRequest;
import java.time.Duration;
import java.util.UUID;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@RestController
@RequestMapping("/authentication")
public class AuthenticationController {

  private final WebClient webClient;

  public AuthenticationController(@Qualifier("fusionAuthWebClient") WebClient webClient) {
    this.webClient = webClient;
  }

  @PostMapping("/login")
  public ResponseEntity<?> login(@Validated @RequestBody LoginRequest request) {
    AccessTokenResponse tokenResponse;
    try {
      tokenResponse =
          webClient
              .post()
              .uri("/oauth2/token")
              .body(
                  BodyInserters.fromFormData("grant_type", "password")
                      .with("username", request.getUsername())
                      .with("password", request.getPassword())
                      .with("scope", "offline_access"))
              .retrieve()
              .bodyToMono(AccessTokenResponse.class)
              .block();
      if (tokenResponse == null) {
        throw new RuntimeException("Received empty response from fusion auth");
      }
    } catch (Exception e) {
      // making it debug level to prevent incorrect login/password trace spamming
      log.debug("Failed to get fusion auth token response", e);

      return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    return ResponseEntity.ok()
        .header(
            HttpHeaders.SET_COOKIE,
            createCookie(
                SecurityConfig.ACCESS_TOKEN_COOKIE_NAME,
                tokenResponse.accessToken,
                tokenResponse.expiresIn),
            createCookie(
                SecurityConfig.REFRESH_TOKEN_COOKIE_NAME,
                tokenResponse.refreshToken,
                tokenResponse.expiresIn))
        .build();
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

  private String createCookie(String name, String value, long ttl) {
    HttpCookie authTokenCookie =
        ResponseCookie.from(name, value)
            .path("/")
            .secure(true)
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
