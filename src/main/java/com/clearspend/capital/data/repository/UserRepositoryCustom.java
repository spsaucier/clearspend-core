package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.repository.impl.UserRepositoryImpl.CardAndAllocationName;
import com.clearspend.capital.service.UserFilterCriteria;
import java.util.List;
import org.springframework.data.domain.Page;

public interface UserRepositoryCustom {

  record FilteredUserWithCardListRecord(User user, List<CardAndAllocationName> card) {}

  Page<FilteredUserWithCardListRecord> find(
      TypedId<BusinessId> businessId, UserFilterCriteria filterCriteria);
}
