package com.clearspend.capital.common.advice;

import static com.clearspend.capital.controller.Common.ROLES;

import com.clearspend.capital.configuration.SecurityConfig.CapitalAuthenticationConverter;
import com.clearspend.capital.data.model.security.DefaultRoles;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.core.annotation.Order;
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
@Slf4j
@Aspect
// make sure that this advice runs before security advices
@Order(-1)
@Component
public class AssignApplicationSecurityContextAdvice {

  private static final Map<String, Object> SECURE_WEBHOOK_CLAIMS =
      Map.of("name", "SecureWebhook", ROLES, Set.of(DefaultRoles.GLOBAL_APPLICATION_WEBHOOK));
  private static final Map<String, Object> SECURE_JOB_CLAIMS =
      Map.of("name", "SecureJob", ROLES, Set.of(DefaultRoles.GLOBAL_APPLICATION_JOB));

  public @interface SecureWebhook {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  public @interface SecureJob {}

  private final CapitalAuthenticationConverter jwtConverter = new CapitalAuthenticationConverter();

  @Pointcut(
      "within(@com.clearspend.capital.common.advice.AssignApplicationSecurityContextAdvice.SecureWebhook *)")
  void secureWebhook() {}

  @Pointcut(
      "@annotation(com.clearspend.capital.common.advice.AssignApplicationSecurityContextAdvice.SecureJob)")
  void secureJob() {}

  @Before("secureWebhook()")
  void setupSecureWebhook() {
    final Jwt secureWebhookJwt =
        new Jwt(
            "SecureWebhookToken",
            Instant.now(),
            Instant.MAX,
            Map.of("SecureWebhookHeader", "IsSecureWebhook"),
            SECURE_WEBHOOK_CLAIMS);

    final JwtAuthenticationToken token = jwtConverter.convert(secureWebhookJwt);
    SecurityContextHolder.setContext(new SecurityContextImpl(token));
  }

  @Before("secureJob()")
  void setupSecureJob() {

    final Jwt secureJobJwt =
        new Jwt(
            "SecureJobToken",
            Instant.now(),
            Instant.MAX,
            Map.of("SecureJobHeader", "IsSecureJob"),
            SECURE_JOB_CLAIMS);

    final JwtAuthenticationToken token = jwtConverter.convert(secureJobJwt);
    SecurityContextHolder.setContext(new SecurityContextImpl(token));
  }
}
