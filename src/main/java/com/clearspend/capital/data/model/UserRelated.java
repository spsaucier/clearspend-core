package com.clearspend.capital.data.model;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;

public interface UserRelated extends BusinessRelated {
  TypedId<UserId> getUserId();
}
