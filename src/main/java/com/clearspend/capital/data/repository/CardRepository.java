package com.clearspend.capital.data.repository;

import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.enums.card.CardType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CardRepository extends JpaRepository<Card, TypedId<CardId>>, CardRepositoryCustom {

  Optional<Card> findByBusinessIdAndId(TypedId<BusinessId> businessId, TypedId<CardId> id);

  Optional<Card> findByBusinessIdAndIdAndLastFour(
      TypedId<BusinessId> businessId, TypedId<CardId> id, String lastFour);

  @Query(
      "SELECT c FROM Card c WHERE c.businessId = :businessId AND c.userId = :userId AND c.status <> 'CANCELLED'")
  List<Card> findAllNotCancelledForUser(
      @Param("businessId") final TypedId<BusinessId> businessId,
      @Param("userId") final TypedId<UserId> userId);

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

  @Query(
      """
        SELECT COUNT(c)
        FROM Card c
        WHERE c.allocationId IN :allocationIds
        AND c.status <> 'CANCELLED'
""")
  long countNonCancelledCardsForAllocations(
      @Param("allocationIds") Collection<TypedId<AllocationId>> allocationIds);

  @Query(
      "SELECT c FROM Card c WHERE c.status <> 'CANCELLED' AND c.allocationId = :allocationId AND c.type = :type")
  List<Card> findAllNonCancelledByAllocationIdAndType(
      @Param("allocationId") final TypedId<AllocationId> allocationId,
      @Param("type") final CardType type);
}
