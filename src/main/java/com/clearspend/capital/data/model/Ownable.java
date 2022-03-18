package com.clearspend.capital.data.model;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;

public interface Ownable extends BusinessRelated {
  TypedId<UserId> getUserId();

  TypedId<AllocationId> getAllocationId();
}
