package com.clearspend.capital.data.model;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;

public interface Ownable {
  TypedId<UserId> getUserId();

  TypedId<AllocationId> getAllocationId();

  TypedId<BusinessId> getBusinessId();
}
