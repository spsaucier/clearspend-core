package com.tranwall.capital.data.repository;

import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.CardId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.data.model.Card;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CardRepository extends JpaRepository<Card, TypedId<CardId>>, CardRepositoryCustom {

  Optional<Card> findByBusinessIdAndId(TypedId<BusinessId> businessId, TypedId<CardId> id);

  Optional<Card> findByBusinessIdAndUserIdAndId(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, TypedId<CardId> id);

  Optional<Card> findByCardNumberHash(byte[] cardNumberHash);

  List<Card> findByBusinessIdAndUserId(TypedId<BusinessId> businessId, TypedId<UserId> userId);
}
