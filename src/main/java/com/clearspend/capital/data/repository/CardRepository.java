package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.Card;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, TypedId<CardId>>, CardRepositoryCustom {

  Optional<Card> findByBusinessIdAndId(TypedId<BusinessId> businessId, TypedId<CardId> id);

  Optional<Card> findByBusinessIdAndUserIdAndId(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, TypedId<CardId> id);

  Optional<Card> findByBusinessIdAndUserIdAndIdAndLastFour(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, TypedId<CardId> id, String lastFour);

  Optional<Card> findByExternalRef(String externalRef);
}
