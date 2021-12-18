package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import java.util.List;

public interface AllocationRepositoryCustom {

  List<TypedId<AllocationId>> retrieveAncestorAllocationIds(TypedId<AllocationId> allocationId);
}
