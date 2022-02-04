package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.enums.card.CardType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardRepository extends JpaRepository<Card, TypedId<CardId>>, CardRepositoryCustom {

  Optional<Card> findByBusinessIdAndId(TypedId<BusinessId> businessId, TypedId<CardId> id);

  Optional<Card> findByBusinessIdAndIdAndLastFour(
      TypedId<BusinessId> businessId, TypedId<CardId> id, String lastFour);

  Optional<Card> findByBusinessIdAndUserIdAndId(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, TypedId<CardId> id);

  Optional<Card> findByBusinessIdAndUserIdAndIdAndLastFour(
      TypedId<BusinessId> businessId, TypedId<UserId> userId, TypedId<CardId> id, String lastFour);

  Optional<Card> findByExternalRef(String externalRef);

  @Query(
      "select c from Card c where c.businessId = :businessId and c.lastFour = :lastFour and c.activated = false")
  List<Card> findNonActivatedByBusinessIdAndLastFour(
      @Param("businessId") TypedId<BusinessId> businessId, @Param("lastFour") String lastFour);

  @Query(
      "select c from Card c where c.businessId = :businessId and c.userId = :userId and c.lastFour = :lastFour and c.activated = false")
  List<Card> findNonActivatedByBusinessIdAndUserIdAndLastFour(
      @Param("businessId") TypedId<BusinessId> businessId,
      @Param("userId") TypedId<UserId> userId,
      @Param("lastFour") String lastFour);

  int countByBusinessIdAndType(TypedId<BusinessId> businessId, CardType cardType);
}
