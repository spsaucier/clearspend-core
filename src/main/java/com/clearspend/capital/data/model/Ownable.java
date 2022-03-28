package com.clearspend.capital.data.model;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;

public interface Ownable extends BusinessRelated, UserRelated {
  TypedId<AllocationId> getAllocationId();
}
