package com.clearspend.capital.common.advice;

import static com.clearspend.capital.controller.Common.ROLES;

import com.clearspend.capital.configuration.SecurityConfig.CapitalAuthenticationConverter;
import com.clearspend.capital.data.model.security.DefaultRoles;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * Our Webhooks, by their very nature, do not support the normal Spring authentication flow.
 * However, our security model in the application requires a SecurityContext to be present on all
 * operations. This AOP Advice class is the solution.
 *
 * <p>First, annotate the class declaration of any controller that receives Webhook calls with
 * the @SecureWebhook annotation. That will result in this class creating a custom SecurityContext
 * for a user with the GLOBAL_APPLICATION_WEBHOOK role. This role will ensure that the APPLICATION
 * global permission is available for all webhook calls. The APPLICATION permission should then be
 * assigned to any service method the Webhook needs to invoke.
 */
@Aspect
@Component
public class AssignWebhookSecurityContextAdvice {

  public @interface SecureWebhook {}

  private final CapitalAuthenticationConverter jwtConverter = new CapitalAuthenticationConverter();

  @Pointcut(
      "within(@com.clearspend.capital.common.advice.AssignWebhookSecurityContextAdvice.SecureWebhook *)")
  void secureWebhook() {}

  @Before("secureWebhook()")
  void setupSecureWebhook() {
    final Map<String, Object> claims =
        Map.of("name", "SecureWebhook", ROLES, Set.of(DefaultRoles.GLOBAL_APPLICATION_WEBHOOK));

    final Jwt secureWebhookJwt =
        new Jwt(
            "SecureWebhookToken",
            Instant.now(),
            Instant.MAX,
            Map.of("SecureWebhookHeader", "IsSecureWebhook"),
            claims);

    final JwtAuthenticationToken token = jwtConverter.convert(secureWebhookJwt);
    SecurityContextHolder.setContext(new SecurityContextImpl(token));
  }
}
