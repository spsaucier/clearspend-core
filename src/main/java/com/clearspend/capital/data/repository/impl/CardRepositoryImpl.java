package com.clearspend.capital.data.repository.impl;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.JoinType;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.crypto.HashUtil;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.Hold;
import com.clearspend.capital.data.model.TransactionLimit;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.HoldStatus;
import com.clearspend.capital.data.repository.CardRepositoryCustom;
import com.clearspend.capital.service.BeanUtils;
import com.clearspend.capital.service.CardFilterCriteria;
import com.clearspend.capital.service.type.CurrentUser;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.validation.constraints.NotNull;
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

  public record CardDetailsWithUserRecord(
      Card card,
      Allocation allocation,
      Account account,
      User user,
      TransactionLimit transactionLimit) {}

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
          .where("card.lastFour")
          .eq(criteria.getSearchText())
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

    Page<CardDetailsWithUserRecord> results =
        BlazePersistenceUtils.queryPagedTuples(
            CardDetailsWithUserRecord.class, builder, criteria.getPageToken(), false);

    PageImpl<FilteredCardRecord> result =
        new PageImpl<>(
            results.stream()
                .map(r -> new FilteredCardRecord(r.card, r.allocation, r.account, r.user))
                .collect(Collectors.toList()),
            results.getPageable(),
            results.getTotalElements());

    calculateAvailableBalance(
        result.getContent().stream().map(FilteredCardRecord::account).distinct().toList());

    return result;
  }

  private List<CardDetailsRecord> findDetails(
      @NonNull TypedId<BusinessId> businessId, TypedId<CardId> cardId, TypedId<UserId> userId) {

    CriteriaBuilder<Tuple> builder = createDefaultBuilder(businessId);

    builder.where("card.businessId").eq(businessId);

    BeanUtils.setNotNull(cardId, id -> builder.where("card.id").eq(id));
    BeanUtils.setNotNull(userId, id -> builder.where("card.userId").eq(id));

    List<CardDetailsRecord> result =
        BlazePersistenceUtils.queryTuples(CardDetailsWithUserRecord.class, builder, false).stream()
            .map(r -> new CardDetailsRecord(r.card, r.allocation, r.account, r.transactionLimit))
            .collect(Collectors.toList());

    calculateAvailableBalance(result.stream().map(CardDetailsRecord::account).distinct().toList());

    return result;
  }

  @Override
  public Optional<CardDetailsRecord> findDetailsByBusinessIdAndId(
      TypedId<BusinessId> businessId, TypedId<CardId> cardId) {

    return toOptional(findDetails(businessId, cardId, null));
  }

  @Override
  public Optional<CardDetailsRecord> findDetailsById(@NotNull TypedId<CardId> cardId) {
    return findDetailsByBusinessIdAndId(CurrentUser.getBusinessId(), cardId);
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

    builder
        .select("card")
        .select("allocation")
        .select("account")
        .select("user")
        .select("transactionLimit");

    return builder;
  }

  private Optional<CardDetailsRecord> toOptional(List<CardDetailsRecord> cardDetailsRecords) {
    if (CollectionUtils.isEmpty(cardDetailsRecords)) {
      return Optional.empty();
    } else {
      return Optional.of(cardDetailsRecords.get(0));
    }
  }

  private void calculateAvailableBalance(List<Account> accounts) {
    if (!CollectionUtils.isEmpty(accounts)) {
      Map<TypedId<AccountId>, Account> accountMap =
          accounts.stream().collect(Collectors.toMap(Account::getId, Function.identity()));

      CriteriaBuilder<Tuple> builder =
          criteriaBuilderFactory.create(entityManager, Tuple.class).from(Hold.class, "hold");

      builder
          .where("hold.accountId")
          .in(accounts.stream().map(Account::getId).toList())
          .where("hold.expirationDate")
          .gt(OffsetDateTime.now(Clock.systemUTC()))
          .where("hold.status")
          .eqLiteral(HoldStatus.PLACED.name());

      builder.select("hold.accountId").select("SUM(hold.amount.amount)").groupBy("hold.accountId");

      builder
          .getResultList()
          .forEach(
              tuple -> {
                TypedId<AccountId> accountId = tuple.get(0, TypedId.class);
                Account account = accountMap.get(accountId);
                account.setAvailableBalance(
                    account
                        .getLedgerBalance()
                        .add(Amount.of(Currency.USD, tuple.get(1, BigDecimal.class))));
              });

      // To make available balance equal to the ledger one for all accounts without holds
      accounts.stream()
          .filter(account -> account.getAvailableBalance() == null)
          .forEach(account -> account.setAvailableBalance(account.getLedgerBalance()));
    }
  }
}
