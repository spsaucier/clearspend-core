package com.clearspend.capital.data.model;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;

public interface BusinessRelated {
  TypedId<BusinessId> getBusinessId();
}
