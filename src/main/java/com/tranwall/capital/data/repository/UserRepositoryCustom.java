package com.tranwall.capital.data.repository;

import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.Card;
import com.tranwall.capital.data.model.User;
import com.tranwall.capital.service.UserFilterCriteria;
import java.util.List;
import org.springframework.data.domain.Page;

public interface UserRepositoryCustom {

  record FilteredUserWithCardListRecord(User user, List<Card> card) {}

  Page<FilteredUserWithCardListRecord> find(
      TypedId<BusinessId> businessId, UserFilterCriteria filterCriteria);
}
