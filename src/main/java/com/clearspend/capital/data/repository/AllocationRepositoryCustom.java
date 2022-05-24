package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.Allocation;
import java.util.List;
import java.util.Set;

public interface AllocationRepositoryCustom {

  List<TypedId<AllocationId>> retrieveAncestorAllocationIds(TypedId<AllocationId> allocationId);

  /**
   * @param allocationId the point from which to search
   * @return a list of AllocationIds having the given AllocationId as an ancestor
   */
  List<TypedId<AllocationId>> retrieveAllocationDescendants(TypedId<AllocationId> allocationId);

  List<Allocation> findByBusinessIdWithSqlPermissions(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, Set<String> globalRoles);
}
