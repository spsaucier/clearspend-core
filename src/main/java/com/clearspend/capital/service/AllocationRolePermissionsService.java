package com.clearspend.capital.service;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.security.AllocationRolePermissions;
import com.clearspend.capital.data.repository.security.AllocationRolePermissionsRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AllocationRolePermissionsService {

  private final AllocationRolePermissionsRepository allocationRolePermissionsRepository;

  public List<AllocationRolePermissions> getAllocationRolePermissions(
      TypedId<BusinessId> businessId) {
    return allocationRolePermissionsRepository.findAllocationRolePermissionsByBusiness(businessId);
  }
}
