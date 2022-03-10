package com.clearspend.capital.configuration;

import com.clearspend.capital.service.security.CapitalMethodSecurityExpressionHandler;
import com.clearspend.capital.service.security.CapitalPermissionEvaluator;
import com.clearspend.capital.service.security.PermissionEnrichmentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.method.configuration.GlobalMethodSecurityConfiguration;

@Configuration
@EnableGlobalMethodSecurity(prePostEnabled = true)
public class MethodSecurityConfig extends GlobalMethodSecurityConfiguration {

  @Autowired private PermissionEnrichmentService permissionEnrichmentService;

  @Override
  protected MethodSecurityExpressionHandler createExpressionHandler() {
    CapitalMethodSecurityExpressionHandler expressionHandler =
        new CapitalMethodSecurityExpressionHandler(permissionEnrichmentService);
    expressionHandler.setPermissionEvaluator(
        new CapitalPermissionEvaluator(permissionEnrichmentService));
    return expressionHandler;
  }
}
