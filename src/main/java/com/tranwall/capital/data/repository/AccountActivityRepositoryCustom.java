package com.tranwall.capital.data.repository;

import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.AccountActivity;
import com.tranwall.capital.data.model.Card;
import com.tranwall.capital.service.AccountActivityFilterCriteria;
import org.springframework.data.domain.Page;

public interface AccountActivityRepositoryCustom {

  record FilteredAccountActivityRecord(AccountActivity accountActivity, Card card) {}

  Page<FilteredAccountActivityRecord> find(
      TypedId<BusinessId> businessId, AccountActivityFilterCriteria filterCriteria);
}
