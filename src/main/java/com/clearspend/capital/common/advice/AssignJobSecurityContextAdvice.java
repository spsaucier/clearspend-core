package com.clearspend.capital.common.advice;

import com.clearspend.capital.common.typedid.data.JobConfigId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.configuration.SecurityConfig.CapitalAuthenticationConverter;
import com.clearspend.capital.crypto.utils.CurrentUserSwitcher;
import com.clearspend.capital.crypto.utils.CurrentUserSwitcher.SwitchesCurrentUser;
import com.clearspend.capital.data.model.JobConfig;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.repository.JobConfigRepository;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.service.type.CurrentJob;
import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * SecureJob will create a secure context to validate if the owner of the configuration from
 * database do have access to the business where job execution will perform actions
 */
@Aspect
@Component
@Slf4j
public class AssignJobSecurityContextAdvice {

  public @interface SecureJob {}

  @Autowired JobConfigRepository jobConfigRepository;
  @Autowired UserRepository userRepository;

  private final CapitalAuthenticationConverter jwtConverter = new CapitalAuthenticationConverter();

  @Pointcut(
      "within(@com.clearspend.capital.common.advice.AssignJobSecurityContextAdvice.SecureJob *)")
  void secureJob() {}

  @Around("secureJob()")
  @SwitchesCurrentUser(
      reviewer = "gbuduianu",
      explanation = "For job scheduler we want to set current user the owner of the job")
  void setupSecureJob(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
    final Map<String, Object> claims = Map.of("name", "SecureJob");

    final Jwt secureJobJwt =
        new Jwt(
            "SecureJobToken",
            Instant.now(),
            Instant.MAX,
            Map.of("SecureJobHeader", "IsSecureJob"),
            claims);
    TypedId<JobConfigId> jobConfigId = new TypedId<>(proceedingJoinPoint.getArgs()[0].toString());
    try {
      JobConfig jobConfig = jobConfigRepository.findById(jobConfigId).orElseThrow();
      TypedId<UserId> configOwnerId = jobConfig.getConfigOwnerId();
      User user = userRepository.findById(configOwnerId).get();

      final JwtAuthenticationToken token = jwtConverter.convert(secureJobJwt);
      SecurityContextHolder.setContext(new SecurityContextImpl(token));
      CurrentUserSwitcher.setCurrentUser(user);
      SecurityContextHolder.getContext().getAuthentication().setAuthenticated(true);
      // this is a scheduled job - will need this flag to know what email template to send
      CurrentJob.setScheduledJob(true);
      proceedingJoinPoint.proceed();
    } catch (NoSuchElementException e) {
      log.debug("No configuration for job execution.", e);
    } finally {
      CurrentJob.setScheduledJob(false);
    }
  }
}
