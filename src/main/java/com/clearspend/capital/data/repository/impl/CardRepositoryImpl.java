package com.clearspend.capital.data.repository.impl;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.JoinType;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.util.SqlResourceLoader;
import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.Item;
import com.clearspend.capital.controller.type.card.SearchCardData;
import com.clearspend.capital.controller.type.user.UserData;
import com.clearspend.capital.crypto.Crypto;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.Hold;
import com.clearspend.capital.data.model.TransactionLimit;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.HoldStatus;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.model.enums.card.CardStatus;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.repository.CardRepositoryCustom;
import com.clearspend.capital.data.repository.impl.JDBCUtils.MustacheQueryConfig;
import com.clearspend.capital.service.BeanUtils;
import com.clearspend.capital.service.CardFilterCriteria;
import com.clearspend.capital.service.type.CurrentUser;
import com.clearspend.capital.service.type.PageToken;
import com.clearspend.capital.util.function.ThrowableFunctions;
import com.clearspend.capital.util.function.TypeFunctions;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.validation.constraints.NotNull;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

@Repository
public class CardRepositoryImpl implements CardRepositoryCustom {

  private final EntityManager entityManager;
  private final CriteriaBuilderFactory criteriaBuilderFactory;
  private final Crypto crypto;

  private final Template template;
  private final String filterCardsQueryText;

  public record CardDetailsWithUserRecord(
      Card card,
      Allocation allocation,
      Account account,
      User user,
      TransactionLimit transactionLimit) {}

  public CardRepositoryImpl(
      EntityManager entityManager,
      CriteriaBuilderFactory criteriaBuilderFactory,
      Crypto crypto,
      @Value("classpath:db/sql/cardRepository/filterCards.sql") Resource filterCardsQueryFile) {
    this.entityManager = entityManager;
    this.criteriaBuilderFactory = criteriaBuilderFactory;
    this.crypto = crypto;
    this.filterCardsQueryText = SqlResourceLoader.load(filterCardsQueryFile);
    this.template = Mustache.compiler().compile(this.filterCardsQueryText);
  }

  private SearchCardData cardFilterRowMapper(final ResultSet resultSet, final int rowNum)
      throws SQLException {
    final Currency currency =
        TypeFunctions.nullableStringToEnum(
            resultSet.getString("ledger_balance_currency"), Currency::valueOf);
    final BigDecimal amount =
        Optional.ofNullable(resultSet.getBigDecimal("ledger_balance_amount"))
            .map(
                ThrowableFunctions.sneakyThrows(
                    value -> value.add(resultSet.getBigDecimal("hold_total"))))
            .orElse(null);
    // These should both either be null or non-null
    final com.clearspend.capital.controller.type.Amount ledgerAmount =
        Optional.ofNullable(currency)
            .map(cur -> new com.clearspend.capital.controller.type.Amount(currency, amount))
            .orElse(null);

    final TypedId<AllocationId> allocationId =
        TypeFunctions.nullableUuidToTypedId(resultSet.getObject("allocation_id", UUID.class));
    final String allocationName = resultSet.getString("allocation_name");
    // These should both either be null or non-null
    final Item<TypedId<AllocationId>> allocationItem =
        Optional.ofNullable(allocationId).map(id -> new Item<>(id, allocationName)).orElse(null);

    return new SearchCardData(
        new TypedId<>(resultSet.getObject("card_id", UUID.class)),
        resultSet.getString("card_number"),
        new UserData(
            new TypedId<>(resultSet.getObject("user_id", UUID.class)),
            UserType.valueOf(resultSet.getString("user_type")),
            new String(crypto.decrypt(resultSet.getBytes("user_first_name_enc"))),
            new String(crypto.decrypt(resultSet.getBytes("user_last_name_enc")))),
        allocationItem,
        ledgerAmount,
        CardStatus.valueOf(resultSet.getString("card_status")),
        CardType.valueOf(resultSet.getString("card_type")),
        resultSet.getBoolean("card_activated"),
        resultSet.getObject("card_activation_date", OffsetDateTime.class));
  }

  @Override
  public Page<SearchCardData> filter(final CardFilterCriteria criteria) {
    final MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", TypeFunctions.nullableTypedIdToUUID(criteria.getBusinessId()))
            .addValue(
                "invokingUser", TypeFunctions.nullableTypedIdToUUID(criteria.getInvokingUser()))
            .addValue("javaNow", OffsetDateTime.now())
            .addValue("globalRoles", CurrentUser.get().roles().toArray(String[]::new), Types.ARRAY)
            .addValue("permission", criteria.getPermission())
            .addValue(
                "cardHolders",
                TypeFunctions.nullableTypedIdListToUuidList(criteria.getCardHolders()))
            .addValue(
                "allocationIds",
                TypeFunctions.nullableTypedIdListToUuidList(criteria.getAllocationIds()))
            .addValue("searchText", criteria.getSearchText())
            .addValue("likeSearchText", "%%%s%%".formatted(criteria.getSearchText()))
            .addValue("searchStringHash", criteria.getSearchStringHash())
            .addValue(
                "statuses", TypeFunctions.nullableEnumListToStringList(criteria.getStatuses()))
            .addValue("types", TypeFunctions.nullableEnumListToStringList(criteria.getTypes()))
            .addValue("pageSize", criteria.getPageToken().getPageSize())
            .addValue("firstResult", criteria.getPageToken().getFirstResult())
            .addValue("minimumBalance", criteria.getMinimumBalance())
            .addValue("maximumBalance", criteria.getMaximumBalance());

    final List<SearchCardData> rawOutput =
        JDBCUtils.executeMustacheQuery(
                entityManager,
                template,
                MustacheQueryConfig.<SearchCardData>builder()
                    .rowMapper(this::cardFilterRowMapper)
                    .parameterSource(params)
                    .build())
            .result();
    PageToken pageToken = criteria.getPageToken();

    if (rawOutput.size() < pageToken.getPageSize() && pageToken.getPageNumber() == 0) {
      return new PageImpl<>(
          rawOutput,
          PageRequest.of(pageToken.getPageNumber(), pageToken.getPageSize()),
          rawOutput.size());
    }

    final MapSqlParameterSource countParams = params.addValue("count", true);
    final long totalElements =
        JDBCUtils.executeMustacheQuery(
                entityManager,
                template,
                MustacheQueryConfig.<Long>builder()
                    .parameterSource(countParams)
                    .rowMapper((resultSet, row) -> resultSet.getLong(1))
                    .build())
            .result()
            .get(0);
    return new PageImpl<>(
        rawOutput,
        PageRequest.of(pageToken.getPageNumber(), pageToken.getPageSize()),
        totalElements);
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

    calculateAvailableBalance(
        result.stream()
            .map(CardDetailsRecord::account)
            .filter(Objects::nonNull)
            .distinct()
            .toList());

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

    BlazePersistenceUtils.joinOnForeignKey(builder, Card.class, Allocation.class, JoinType.LEFT);
    BlazePersistenceUtils.joinOnForeignKey(builder, Card.class, Account.class, JoinType.LEFT);
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
