package com.clearspend.capital.common.advice;

import static com.clearspend.capital.controller.Common.ROLES;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.clearspend.capital.data.model.security.DefaultRoles;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class AssignApplicationSecurityContextAdviceTest {
  private final AssignApplicationSecurityContextAdvice advice =
      new AssignApplicationSecurityContextAdvice();

  @BeforeEach
  void setup() {
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void cleanup() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void setupSecureWebhook() {
    advice.setupSecureWebhook();
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertNotNull(authentication);
    assertThat(authentication).isInstanceOf(JwtAuthenticationToken.class);
    final Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
    assertThat(jwt.getClaims())
        .containsEntry("name", "SecureWebhook")
        .containsEntry(ROLES, Set.of(DefaultRoles.GLOBAL_APPLICATION_WEBHOOK));
  }

  @Test
  void setupSecureJob() {
    advice.setupSecureJob();
    final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    assertNotNull(authentication);
    assertThat(authentication).isInstanceOf(JwtAuthenticationToken.class);
    final Jwt jwt = ((JwtAuthenticationToken) authentication).getToken();
    assertThat(jwt.getClaims())
        .containsEntry("name", "SecureJob")
        .containsEntry(ROLES, Set.of(DefaultRoles.GLOBAL_APPLICATION_JOB));
  }
}
