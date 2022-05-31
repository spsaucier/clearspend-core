package com.clearspend.capital.common.data.model;

import com.clearspend.capital.common.typedid.data.TypedId;

public interface TypedObject<T> {

  TypedId<T> getId();
}
