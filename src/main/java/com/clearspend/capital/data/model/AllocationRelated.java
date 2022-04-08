package com.clearspend.capital.data.model;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;

public interface AllocationRelated extends BusinessRelated {
  TypedId<AllocationId> getAllocationId();
}
