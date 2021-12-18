package com.clearspend.capital.data.repository.impl;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.JoinType;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.BusinessId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.crypto.HashUtil;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.Hold;
import com.clearspend.capital.data.model.TransactionLimit;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.enums.HoldStatus;
import com.clearspend.capital.data.repository.CardRepositoryCustom;
import com.clearspend.capital.service.BeanUtils;
import com.clearspend.capital.service.CardFilterCriteria;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

@Repository
@RequiredArgsConstructor
public class CardRepositoryImpl implements CardRepositoryCustom {

  private final EntityManager entityManager;
  private final CriteriaBuilderFactory criteriaBuilderFactory;

  public record CardDetailsWithHoldRecord(
      Card card,
      Allocation allocation,
      Account account,
      User user,
      TransactionLimit transactionLimit,
      BigDecimal holdsSum) {}

  @Override
  public Page<FilteredCardRecord> filter(CardFilterCriteria criteria) {
    CriteriaBuilder<Tuple> builder = createDefaultBuilder(criteria.getBusinessId());

    BeanUtils.setNotNull(criteria.getBusinessId(), id -> builder.where("card.businessId").eq(id));
    BeanUtils.setNotNull(criteria.getUserId(), id -> builder.where("card.userId").eq(id));
    BeanUtils.setNotNull(criteria.getAllocationId(), id -> builder.where("allocation.id").eq(id));

    if (StringUtils.isNotEmpty(criteria.getSearchText())) {
      byte[] encryptedValue = HashUtil.calculateHash(criteria.getSearchText());
      builder
          .whereOr()
          .where("card.cardNumber.hash")
          .eq(encryptedValue)
          .where("user.firstName.hash")
          .eq(encryptedValue)
          .where("user.lastName.hash")
          .eq(encryptedValue)
          .where("allocation.name")
          .like(false)
          .value("%" + criteria.getSearchText() + "%")
          .noEscape()
          .endOr();
    }

    Page<CardDetailsWithHoldRecord> results =
        BlazePersistenceUtils.queryPagedTuples(
            CardDetailsWithHoldRecord.class, builder, criteria.getPageToken(), false);

    return new PageImpl<>(
        results.stream()
            .map(
                r ->
                    new FilteredCardRecord(
                        r.card,
                        r.allocation,
                        calcualteAvailableBalance(r.account, r.holdsSum),
                        r.user))
            .collect(Collectors.toList()),
        results.getPageable(),
        results.getTotalElements());
  }

  private List<CardDetailsRecord> findDetails(
      @NonNull TypedId<BusinessId> businessId, TypedId<CardId> cardId, TypedId<UserId> userId) {

    CriteriaBuilder<Tuple> builder = createDefaultBuilder(businessId);

    builder.where("card.businessId").eq(businessId);

    BeanUtils.setNotNull(cardId, id -> builder.where("card.id").eq(id));
    BeanUtils.setNotNull(userId, id -> builder.where("card.userId").eq(id));

    return BlazePersistenceUtils.queryTuples(CardDetailsWithHoldRecord.class, builder, false)
        .stream()
        .map(
            r ->
                new CardDetailsRecord(
                    r.card,
                    r.allocation,
                    calcualteAvailableBalance(r.account, r.holdsSum),
                    r.transactionLimit))
        .collect(Collectors.toList());
  }

  @Override
  public Optional<CardDetailsRecord> findDetailsByBusinessIdAndId(
      TypedId<BusinessId> businessId, TypedId<CardId> cardId) {

    return toOptional(findDetails(businessId, cardId, null));
  }

  @Override
  public List<CardDetailsRecord> findDetailsByBusinessIdAndUserId(
      TypedId<BusinessId> businessId, TypedId<UserId> userId) {

    return findDetails(businessId, null, userId);
  }

  @Override
  public Optional<CardDetailsRecord> findDetailsByBusinessIdAndUserIdAndId(
      TypedId<BusinessId> businessId, @NonNull TypedId<CardId> cardId, TypedId<UserId> userId) {

    return toOptional(findDetails(businessId, cardId, userId));
  }

  private CriteriaBuilder<Tuple> createDefaultBuilder(TypedId<BusinessId> businessId) {
    CriteriaBuilder<Tuple> builder =
        criteriaBuilderFactory.create(entityManager, Tuple.class).from(Card.class, "card");

    BlazePersistenceUtils.joinOnForeignKey(builder, Card.class, Allocation.class, JoinType.INNER);
    BlazePersistenceUtils.joinOnForeignKey(builder, Card.class, Account.class, JoinType.INNER);
    BlazePersistenceUtils.joinOnForeignKey(builder, Card.class, User.class, JoinType.INNER);

    BlazePersistenceUtils.joinOnPrimaryKey(
        builder, Card.class, TransactionLimit.class, "ownerId", JoinType.INNER);
    BlazePersistenceUtils.joinOnPrimaryKey(
        builder, Account.class, Hold.class, "accountId", JoinType.LEFT);

    builder
        // hold status
        .whereOr()
        .where("hold.status")
        .eqLiteral(HoldStatus.PLACED)
        .where("hold.status")
        .isNull()
        .endOr()
        // hold expirationDate
        .whereOr()
        .where("expirationDate")
        .gtExpression("CURRENT_TIMESTAMP")
        .where("expirationDate")
        .isNull()
        .endOr();

    builder
        .select("card")
        .select("allocation")
        .select("account")
        .select("user")
        .select("transactionLimit")
        .select("SUM(hold.amount.amount)");

    return builder;
  }

  private Optional<CardDetailsRecord> toOptional(List<CardDetailsRecord> cardDetailsRecords) {
    if (CollectionUtils.isEmpty(cardDetailsRecords)) {
      return Optional.empty();
    } else {
      return Optional.of(cardDetailsRecords.get(0));
    }
  }

  private Account calcualteAvailableBalance(Account account, BigDecimal holdsSum) {
    Amount ledgerBalance = account.getLedgerBalance();
    account.setAvailableBalance(
        ledgerBalance.add(
            new Amount(
                ledgerBalance.getCurrency(), holdsSum != null ? holdsSum : BigDecimal.ZERO)));

    return account;
  }
}
