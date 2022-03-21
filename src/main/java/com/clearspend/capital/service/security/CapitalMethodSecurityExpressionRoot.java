package com.clearspend.capital.service.security;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.BusinessRelated;
import com.clearspend.capital.data.model.Ownable;
import com.clearspend.capital.data.model.enums.AllocationPermission;
import com.clearspend.capital.service.type.CurrentUser;
import lombok.NonNull;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public class CapitalMethodSecurityExpressionRoot extends SecurityExpressionRoot
    implements MethodSecurityExpressionOperations {
  private final PermissionEnrichmentService permissionEnrichmentService;

  private Object filterObject;
  private Object returnObject;

  public CapitalMethodSecurityExpressionRoot(
      Authentication authentication, PermissionEnrichmentService permissionEnrichmentService) {
    super(authentication);
    this.permissionEnrichmentService = permissionEnrichmentService;
  }

  public boolean isSelfOwned(Ownable entity) {
    if (entity == null) {
      return false;
    }
    return ((JwtAuthenticationToken) getAuthentication())
            .getToken()
            .getClaim("capitalUserId")
            .equals(String.valueOf(entity.getUserId()))
        && permissionEnrichmentService.evaluatePermission(
            getAuthentication(),
            entity.getBusinessId(),
            entity.getAllocationId(),
            AllocationPermission.VIEW_OWN.toString());
  }

  public boolean isSelfOwned() {
    if (getReturnObject() instanceof Ownable) {
      return isSelfOwned((Ownable) getReturnObject());
    }
    return false;
  }

  public boolean hasAllocationPermission(TypedId<AllocationId> allocationId, String permissions) {
    return permissionEnrichmentService.evaluatePermission(
        getAuthentication(), CurrentUser.getBusinessId(), allocationId, permissions);
  }

  public boolean hasGlobalPermission(String permissions) {
    return permissionEnrichmentService.evaluatePermission(
        getAuthentication(), CurrentUser.getBusinessId(), null, permissions);
  }

  public boolean hasRootPermission(
      @NonNull final Object businessObject, @NonNull final String permissions) {
    TypedId<BusinessId> businessId;
    if (businessObject instanceof BusinessRelated br) {
      businessId = br.getBusinessId();
    } else if (businessObject instanceof TypedId<?> ti) {
      businessId = (TypedId<BusinessId>) ti;
    } else {
      throw new IllegalArgumentException(
          "Invalid type of businessObject for root permission: "
              + businessObject.getClass().getName());
    }

    return permissionEnrichmentService.evaluatePermission(
        getAuthentication(), businessId, null, permissions);
  }

  @Override
  public Object getFilterObject() {
    return this.filterObject;
  }

  @Override
  public Object getReturnObject() {
    return this.returnObject;
  }

  @Override
  public Object getThis() {
    return this;
  }

  @Override
  public void setFilterObject(Object obj) {
    this.filterObject = obj;
  }

  @Override
  public void setReturnObject(Object obj) {
    this.returnObject = obj;
  }
}
