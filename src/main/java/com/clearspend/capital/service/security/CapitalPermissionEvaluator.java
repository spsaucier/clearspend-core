package com.clearspend.capital.service.security;

import java.io.Serializable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Slf4j
@RequiredArgsConstructor
public class CapitalPermissionEvaluator implements PermissionEvaluator {
  private final PermissionEnrichmentService permissionEnrichmentService;

  @Override
  public boolean hasPermission(
      final Authentication authentication, final Object targetObject, final Object permissions) {
    final String permissionsString = getPermissionsAsString(permissions);
    final PermissionEvaluationContext permissionEvaluationContext =
        PermissionEvaluationContext.fromTarget(targetObject);
    return permissionEnrichmentService.evaluatePermission(
        authentication, permissionEvaluationContext, permissionsString);
  }

  private String getPermissionsAsString(final Object permissions) {
    if (permissions == null) {
      return "";
    }

    if (permissions instanceof String perm) {
      return perm;
    }
    throw new IllegalArgumentException(
        "Permissions value in hasPermission() expression must be a String");
  }

  @Override
  public boolean hasPermission(
      Authentication authentication, Serializable targetId, String targetType, Object permission) {
    throw new UnsupportedOperationException("Please use hasPermissions(arg, arg) instead");
  }
}
