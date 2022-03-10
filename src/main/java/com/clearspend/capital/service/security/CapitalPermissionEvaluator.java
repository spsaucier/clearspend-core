package com.clearspend.capital.service.security;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.service.type.CurrentUser;
import java.io.Serializable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Slf4j
public class CapitalPermissionEvaluator implements PermissionEvaluator {

  private final PermissionEnrichmentService permissionEnrichmentService;

  public CapitalPermissionEvaluator(PermissionEnrichmentService permissionEnrichmentService) {
    this.permissionEnrichmentService = permissionEnrichmentService;
  }

  @Override
  public boolean hasPermission(
      Authentication authentication, Object targetDomainObject, Object permission) {
    TypedId<AllocationId> allocationId = null;
    TypedId<BusinessId> businessId = null;
    if (targetDomainObject instanceof Business) {
      businessId = ((Business) targetDomainObject).getId();
    } else if (targetDomainObject instanceof Allocation) {
      allocationId = ((Allocation) targetDomainObject).getId();
    } else if (targetDomainObject instanceof TypedId) {
      // only allocationID will work here, else use the syntax with an explicit targetType
      //noinspection unchecked
      allocationId = (TypedId<AllocationId>) targetDomainObject;
    }

    if (businessId == null) {
      businessId = CurrentUser.getBusinessId();
    }

    return hasPermission(
        authentication,
        allocationId == null ? businessId : allocationId,
        allocationId == null ? "BusinessId" : "AllocationId",
        permission);
  }

  @Override
  public boolean hasPermission(
      Authentication authentication, Serializable targetId, String targetType, Object permission) {
    @SuppressWarnings("unchecked")
    TypedId<AllocationId> allocationId =
        targetType.equals("AllocationId") ? (TypedId<AllocationId>) targetId : null;
    @SuppressWarnings("unchecked")
    TypedId<BusinessId> businessId =
        targetType.equals("BusinessId") ? (TypedId<BusinessId>) targetId : null;

    return permissionEnrichmentService.evaluatePermission(
        authentication, businessId, allocationId, String.valueOf(permission));
  }
}
