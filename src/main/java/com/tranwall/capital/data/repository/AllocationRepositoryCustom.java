package com.tranwall.capital.data.repository;

import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.TypedId;
import java.util.List;

public interface AllocationRepositoryCustom {

  List<TypedId<AllocationId>> retrieveAncestorAllocationIds(TypedId<AllocationId> allocationId);
}
