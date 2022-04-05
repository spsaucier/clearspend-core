package com.clearspend.capital.data.repository.impl;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.util.MustacheResourceLoader;
import com.clearspend.capital.common.data.util.SqlResourceLoader;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.activity.ChartFilterType;
import com.clearspend.capital.crypto.Crypto;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.enums.AccountActivityIntegrationSyncStatus;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.repository.AccountActivityRepositoryCustom;
import com.clearspend.capital.service.AccountActivityFilterCriteria;
import com.clearspend.capital.service.BeanUtils;
import com.clearspend.capital.service.type.AllocationChartData;
import com.clearspend.capital.service.type.CardAllocationSpendingDaily;
import com.clearspend.capital.service.type.CardStatementActivity;
import com.clearspend.capital.service.type.CardStatementData;
import com.clearspend.capital.service.type.CardStatementFilterCriteria;
import com.clearspend.capital.service.type.ChartData;
import com.clearspend.capital.service.type.ChartFilterCriteria;
import com.clearspend.capital.service.type.CurrentUser;
import com.clearspend.capital.service.type.DashboardData;
import com.clearspend.capital.service.type.GraphData;
import com.clearspend.capital.service.type.GraphFilterCriteria;
import com.clearspend.capital.service.type.MerchantCategoryChartData;
import com.clearspend.capital.service.type.MerchantChartData;
import com.clearspend.capital.service.type.PageToken;
import com.clearspend.capital.service.type.UserChartData;
import com.samskivert.mustache.Template;
import com.vladmihalcea.hibernate.type.array.StringArrayType;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.sql.Array;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.Session;
import org.hibernate.jpa.TypedParameterValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
public class AccountActivityRepositoryImpl implements AccountActivityRepositoryCustom {

  public static final int LIMIT_SIZE_FOR_CHART = 5;
  public static final int DEFAULT_SLICES_FOR_WEEK = 7;
  public static final int DEFAULT_SLICES = 10;

  private final EntityManager entityManager;
  private final CriteriaBuilderFactory criteriaBuilderFactory;
  private final Crypto crypto;

  private final Template findAccountActivityTemplate;
  private final Template findAccountActivityForLineGraphTemplate;
  private final Template findAccountActivityForChartTemplate;

  @SneakyThrows
  public AccountActivityRepositoryImpl(
      EntityManager entityManager,
      CriteriaBuilderFactory criteriaBuilderFactory,
      Crypto crypto,
      @Value("classpath:db/sql/accountActivityRepository/findAccountActivity.sql")
          Resource findAccountActivityQuery,
      @Value("classpath:db/sql/accountActivityRepository/findAccountActivityForLineGraph.sql")
          Resource findAccountActivityForLineGraphQuery,
      @Value("classpath:db/sql/accountActivityRepository/findAccountActivityForChart.sql")
          Resource findAccountActivityForChartQuery) {
    this.entityManager = entityManager;
    this.criteriaBuilderFactory = criteriaBuilderFactory;
    this.crypto = crypto;
    this.findAccountActivityTemplate = MustacheResourceLoader.load(findAccountActivityQuery);
    this.findAccountActivityForLineGraphTemplate =
        MustacheResourceLoader.load(findAccountActivityForLineGraphQuery);
    this.findAccountActivityForChartTemplate =
        MustacheResourceLoader.load(findAccountActivityForChartQuery);
  }

  private Timestamp nullableDateTimeToTimestamp(final OffsetDateTime offsetDateTime) {
    return Optional.ofNullable(offsetDateTime)
        .map(OffsetDateTime::toInstant)
        .map(Timestamp::from)
        .orElse(null);
  }

  @Override
  @SuppressWarnings("unchecked")
  public Page<AccountActivity> find(
      @NonNull TypedId<BusinessId> businessId, AccountActivityFilterCriteria criteria) {

    final PageToken pageToken = criteria.getPageToken();

    final String query = JDBCUtils.generateQuery(findAccountActivityTemplate, criteria, false);
    final BiConsumer<Query, Boolean> setParams =
        (jpaQuery, isCount) -> {
          final Array array =
              entityManager
                  .unwrap(Session.class)
                  .doReturningWork(
                      conn -> conn.createArrayOf("VARCHAR", CurrentUser.get().roles().toArray()));

          jpaQuery
              .setParameter("businessId", businessId.toUuid())
              .setParameter("invokingUser", CurrentUser.getUserId().toUuid())
              .setParameter(
                  "globalRoles",
                  new TypedParameterValue(
                      StringArrayType.INSTANCE, CurrentUser.get().roles().toArray(String[]::new)));
          if (!isCount) {
            jpaQuery
                .setParameter("limit", pageToken.getPageSize())
                .setParameter("offset", pageToken.getFirstResult());
          }

          BeanUtils.setNotEmpty(
              criteria.getTypes(),
              types -> {
                final List<String> typesNames =
                    types.stream().map(AccountActivityType::name).toList();
                jpaQuery.setParameter("types", typesNames);
              });

          BeanUtils.setNotEmpty(
              criteria.getStatuses(),
              statuses -> {
                final List<String> statusesNames =
                    statuses.stream().map(AccountActivityStatus::name).toList();
                jpaQuery.setParameter("statuses", statusesNames);
              });

          BeanUtils.setNotNull(
              criteria.getCategories(),
              categories -> jpaQuery.setParameter("categories", categories));

          BeanUtils.setNotNull(
              criteria.getSyncStatuses(),
              syncStatuses -> {
                final List<String> syncStatusNames =
                    syncStatuses.stream()
                        .map(AccountActivityIntegrationSyncStatus::name)
                        .collect(Collectors.toList());
                jpaQuery.setParameter("syncStatuses", syncStatusNames);
              });

          BeanUtils.setNotNull(criteria.getMin(), min -> jpaQuery.setParameter("min", min));
          BeanUtils.setNotNull(criteria.getMax(), max -> jpaQuery.setParameter("max", max));

          BeanUtils.setNotEmpty(
              criteria.getSearchText(),
              searchText -> jpaQuery.setParameter("searchText", "%%%s%%".formatted(searchText)));
          BeanUtils.setNotNull(
              criteria.getUserId(),
              userId -> jpaQuery.setParameter("userId", nullableTypedIdToUuid(userId)));
          BeanUtils.setNotNull(
              criteria.getCardId(),
              cardId -> jpaQuery.setParameter("cardId", nullableTypedIdToUuid(cardId)));
          BeanUtils.setNotNull(
              criteria.getAllocationId(),
              allocationId ->
                  jpaQuery.setParameter("allocationId", nullableTypedIdToUuid(allocationId)));
          BeanUtils.setNotNull(
              criteria.getFrom(),
              from -> jpaQuery.setParameter("from", nullableDateTimeToTimestamp(from)));
          BeanUtils.setNotNull(
              criteria.getTo(), to -> jpaQuery.setParameter("to", nullableDateTimeToTimestamp(to)));
        };

    final Query nativeQuery = entityManager.createNativeQuery(query, AccountActivity.class);
    setParams.accept(nativeQuery, false);
    final List<AccountActivity> result = nativeQuery.getResultList();

    if (pageToken.getPageNumber() == 0 && result.size() < pageToken.getPageSize()) {
      return new PageImpl<>(
          result,
          PageRequest.of(pageToken.getPageNumber(), pageToken.getPageSize()),
          result.size());
    }

    final String countQuery = JDBCUtils.generateQuery(findAccountActivityTemplate, criteria, true);
    final Query nativeCountQuery = entityManager.createNativeQuery(countQuery);
    setParams.accept(nativeCountQuery, true);
    final long totalElements = ((BigInteger) nativeCountQuery.getSingleResult()).longValue();

    return new PageImpl<>(
        result, PageRequest.of(pageToken.getPageNumber(), pageToken.getPageSize()), totalElements);
  }

  private UUID nullableTypedIdToUuid(@Nullable final TypedId<?> typedId) {
    return Optional.ofNullable(typedId).map(TypedId::toUuid).orElse(null);
  }

  @Override
  @Transactional(TxType.REQUIRED)
  public DashboardData findDataForLineGraph(
      @NonNull TypedId<BusinessId> businessId, GraphFilterCriteria criteria) {

    final String query = findAccountActivityForLineGraphTemplate.execute(criteria);
    final String zoneOffset = OffsetDateTime.now(ZoneOffset.UTC).getOffset().getId();

    final int slices =
        ChronoUnit.DAYS.between(criteria.getFrom().toInstant(), criteria.getTo().toInstant())
                > DEFAULT_SLICES_FOR_WEEK
            ? DEFAULT_SLICES
            : DEFAULT_SLICES_FOR_WEEK;

    final UUID userId = nullableTypedIdToUuid(criteria.getUserId());
    final UUID allocationId = nullableTypedIdToUuid(criteria.getAllocationId());

    final MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("to", Timestamp.from(criteria.getTo().toInstant()))
            .addValue("from", Timestamp.from(criteria.getFrom().toInstant()))
            .addValue("slices", slices)
            .addValue("businessId", businessId.toUuid())
            .addValue("type", AccountActivityType.NETWORK_CAPTURE.name())
            .addValue("userId", userId)
            .addValue("allocationId", allocationId)
            .addValue("owningUserId", CurrentUser.getUserId().toUuid())
            .addValue("globalRoles", CurrentUser.get().roles().toArray(new String[0]), Types.ARRAY);

    final List<GraphData> graphDataList =
        JDBCUtils.query(
            entityManager,
            query,
            params,
            (resultSet, rowNum) ->
                new GraphData(
                    resultSet.getTimestamp(1).toInstant().atOffset(ZoneOffset.of(zoneOffset)),
                    resultSet.getTimestamp(2).toInstant().atOffset(ZoneOffset.of(zoneOffset)),
                    resultSet.getBigDecimal(3),
                    resultSet.getBigDecimal(4)));

    final BigDecimal totalAmount =
        graphDataList.stream().map(GraphData::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalNumberOfElements =
        graphDataList.stream().map(GraphData::getCount).reduce(BigDecimal.ZERO, BigDecimal::add);

    if (totalNumberOfElements.signum() == 0) {
      totalNumberOfElements = BigDecimal.ONE;
    }

    return new DashboardData(
        totalAmount,
        totalAmount.divide(totalNumberOfElements, 2, RoundingMode.DOWN),
        graphDataList);
  }

  @Override
  public ChartData findDataForChart(
      @NonNull TypedId<BusinessId> businessId, ChartFilterCriteria criteria) {
    final String direction =
        Optional.ofNullable(criteria.getDirection())
            .map(Sort.Direction::name)
            .orElse(Direction.ASC.name());
    final ChartFilterContext context =
        ChartFilterContext.fromChartFilterType(criteria.getChartFilterType())
            .direction(direction)
            .userId(nullableTypedIdToUuid(criteria.getUserId()))
            .allocationId(nullableTypedIdToUuid(criteria.getAllocationId()))
            .build();

    final MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("limit", LIMIT_SIZE_FOR_CHART)
            .addValue("businessId", businessId.toUuid())
            .addValue("allocationId", nullableTypedIdToUuid(criteria.getAllocationId()))
            .addValue("userId", nullableTypedIdToUuid(criteria.getUserId()))
            .addValue("type", AccountActivityType.NETWORK_CAPTURE.name())
            .addValue("from", nullableDateTimeToTimestamp(criteria.getFrom()))
            .addValue("to", nullableDateTimeToTimestamp(criteria.getTo()))
            .addValue("owningUserId", CurrentUser.getUserId().toUuid())
            .addValue("globalRoles", CurrentUser.get().roles().toArray(new String[0]), Types.ARRAY);

    final String query = findAccountActivityForChartTemplate.execute(context);
    final ChartData chartData = new ChartData(null, null, null, null);

    final RowMapper<?> rowMapper =
        switch (criteria.getChartFilterType()) {
          case MERCHANT_CATEGORY -> AccountActivityRowMappers.MERCHANT_CATEGORY_CHART_MAPPER;
          case MERCHANT -> AccountActivityRowMappers.MERCHANT_CHART_MAPPER;
          case ALLOCATION -> AccountActivityRowMappers.ALLOCATION_CHART_MAPPER;
          case EMPLOYEE -> AccountActivityRowMappers.USER_CHART_MAPPER;
        };

    final List<?> chartRecords = JDBCUtils.query(entityManager, query, params, rowMapper);

    return switch (criteria.getChartFilterType()) {
      case MERCHANT_CATEGORY -> chartData.withMerchantCategoryChartData(
          (List<MerchantCategoryChartData>) chartRecords);
      case MERCHANT -> chartData.withMerchantChartData((List<MerchantChartData>) chartRecords);
      case ALLOCATION -> chartData.withAllocationChartData(
          (List<AllocationChartData>) chartRecords);
      case EMPLOYEE -> {
        List<UserChartData> chartRecords1 = (List<UserChartData>) chartRecords;
        chartRecords1.forEach(
            userChartData -> {
              userChartData.setFirstName(
                  new String(crypto.decrypt(userChartData.getFirstNameEncrypted())));
              userChartData.setLastName(
                  new String(crypto.decrypt(userChartData.getLastNameEncrypted())));
            });
        yield chartData.withUserChartData(chartRecords1);
      }
    };
  }

  @Override
  public CardStatementData findDataForCardStatement(
      @NonNull TypedId<BusinessId> businessId, CardStatementFilterCriteria criteria) {

    CriteriaBuilder<Tuple> query =
        criteriaBuilderFactory
            .create(entityManager, Tuple.class)
            .from(AccountActivity.class, "accountActivity");

    query.where("accountActivity.businessId").eq(businessId);
    query.where("accountActivity.activityTime").between(criteria.getFrom()).and(criteria.getTo());
    query.where("accountActivity.card.cardId").eq(criteria.getCardId());
    query.where("accountActivity.type").eq(AccountActivityType.NETWORK_CAPTURE);

    query
        .select("accountActivity.activityTime")
        .select("accountActivity.merchant.name")
        .select("accountActivity.amount.amount");

    List<Tuple> resultList = query.getResultList();

    List<CardStatementActivity> activities =
        resultList.stream()
            .map(
                tuple ->
                    new CardStatementActivity(
                        (OffsetDateTime) tuple.get(0),
                        tuple.get(1) != null ? tuple.get(1).toString() : "-",
                        ((BigDecimal) tuple.get(2)).negate()))
            .collect(Collectors.toList());

    BigDecimal totalAmount =
        activities.stream()
            .map(CardStatementActivity::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    return new CardStatementData(activities, totalAmount);
  }

  @Override
  public CardAllocationSpendingDaily findCardAllocationSpendingDaily(
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      TypedId<CardId> cardId,
      int daysAgo) {

    return JDBCUtils.query(
        entityManager,
        SqlResourceLoader.load("db/sql/accountActivityRepository/cardAllocationSpendingDaily.sql"),
        new MapSqlParameterSource()
            .addValue("businessId", businessId.toUuid())
            .addValue("cardId", cardId.toUuid())
            .addValue("allocationId", allocationId.toUuid())
            .addValue("activityType", AccountActivityType.NETWORK_CAPTURE.name())
            .addValue(
                "statuses",
                List.of(
                    AccountActivityStatus.PENDING.name(), AccountActivityStatus.APPROVED.name()))
            .addValue("timeFrom", LocalDate.now(ZoneOffset.UTC).minusDays(daysAgo)),
        (resultSet) -> {
          CardAllocationSpendingDaily spendingTotal =
              new CardAllocationSpendingDaily(new HashMap<>(), new HashMap<>());
          while (resultSet.next()) {
            Currency currency = Currency.of(resultSet.getString("currency"));
            LocalDate activityTime = resultSet.getObject("activity_date", LocalDate.class);

            spendingTotal
                .getCardSpendings()
                .computeIfAbsent(currency, key -> new HashMap<>())
                .put(activityTime, Amount.of(currency, resultSet.getBigDecimal("card_total")));

            spendingTotal
                .getAllocationSpendings()
                .computeIfAbsent(currency, key -> new HashMap<>())
                .put(
                    activityTime, Amount.of(currency, resultSet.getBigDecimal("allocation_total")));
          }

          return spendingTotal;
        });
  }

  @Getter
  @SuppressWarnings("MissingSummary")
  @Builder
  private static class ChartFilterContext {

    private final boolean isAllocation;
    private final boolean isEmployee;
    private final boolean isMerchant;
    private final boolean isMerchantCategory;
    private final String direction;
    private final UUID userId;
    private final UUID allocationId;

    public static ChartFilterContextBuilder fromChartFilterType(final ChartFilterType type) {
      return ChartFilterContext.builder()
          .isAllocation(ChartFilterType.ALLOCATION == type)
          .isEmployee(ChartFilterType.EMPLOYEE == type)
          .isMerchant(ChartFilterType.MERCHANT == type)
          .isMerchantCategory(ChartFilterType.MERCHANT_CATEGORY == type);
    }
  }
}
