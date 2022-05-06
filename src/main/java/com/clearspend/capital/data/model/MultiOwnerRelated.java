package com.clearspend.capital.data.model;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import java.util.Set;

public interface MultiOwnerRelated extends BusinessRelated {
  Set<TypedId<UserId>> getOwnerIds();
}
