package com.clearspend.capital.data.repository.impl;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.util.SqlResourceLoader;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.ReceiptId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.crypto.Crypto;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.embedded.CardDetails;
import com.clearspend.capital.data.model.embedded.ExpenseDetails;
import com.clearspend.capital.data.model.embedded.MerchantDetails;
import com.clearspend.capital.data.model.embedded.ReceiptDetails;
import com.clearspend.capital.data.model.enums.AccountActivityIntegrationSyncStatus;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.MerchantType;
import com.clearspend.capital.data.model.enums.UserType;
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
import com.clearspend.capital.service.type.DashboardData;
import com.clearspend.capital.service.type.GraphData;
import com.clearspend.capital.service.type.GraphFilterCriteria;
import com.clearspend.capital.service.type.MerchantCategoryChartData;
import com.clearspend.capital.service.type.MerchantChartData;
import com.clearspend.capital.service.type.PageToken;
import com.clearspend.capital.service.type.UserChartData;
import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.internal.SessionImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort.Direction;
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

  private final String findAccountActivityQuery;
  private final Template template;

  @SneakyThrows
  public AccountActivityRepositoryImpl(
      Crypto crypto,
      EntityManager entityManager,
      CriteriaBuilderFactory criteriaBuilderFactory,
      @Value("classpath:db/sql/accountActivityRepository/findAccountActivity.sql")
          Resource findAccountActivityQuery) {
    this.entityManager = entityManager;
    this.criteriaBuilderFactory = criteriaBuilderFactory;
    this.findAccountActivityQuery = SqlResourceLoader.load(findAccountActivityQuery);
    this.crypto = crypto;
    this.template = Mustache.compiler().compile(this.findAccountActivityQuery);
  }

  @Override
  public Page<AccountActivity> find(
      @NonNull TypedId<BusinessId> businessId, AccountActivityFilterCriteria criteria) {

    criteria.setBusinessId(businessId);
    StringWriter out = new StringWriter();
    template.execute(criteria, new JDBCUtils.CountObjectForSqlQuery(false), out);
    String query = out.toString();

    List<AccountActivity> result =
        JDBCUtils.query(
            entityManager,
            query,
            new MapSqlParameterSource(),
            (resultSet, rowNum) -> {
              AccountActivity accountActivity =
                  new AccountActivity(
                      new TypedId<>(resultSet.getObject("business_id", UUID.class)),
                      new TypedId<>(resultSet.getObject("allocation_id", UUID.class)),
                      resultSet.getString("allocation_name"),
                      new TypedId<>(resultSet.getObject("account_id", UUID.class)),
                      AccountActivityType.valueOf(resultSet.getString("type")),
                      AccountActivityStatus.valueOf(resultSet.getString("status")),
                      resultSet.getObject("activity_time", OffsetDateTime.class),
                      Amount.of(
                          Currency.valueOf(resultSet.getString("amount_currency")),
                          resultSet.getObject("amount_amount", BigDecimal.class)),
                      Amount.of(
                          Currency.valueOf(resultSet.getString("requested_amount_currency")),
                          resultSet.getObject("requested_amount_amount", BigDecimal.class)),
                      AccountActivityIntegrationSyncStatus.valueOf(
                          resultSet.getString("integration_sync_status")));
              accountActivity.setId(new TypedId<>(resultSet.getObject("id", UUID.class)));
              UUID userId = resultSet.getObject("user_id", UUID.class);
              BeanUtils.setNotNull(userId, id -> accountActivity.setUserId(new TypedId<>(id)));
              UUID adjustmentId = resultSet.getObject("adjustment_id", UUID.class);
              BeanUtils.setNotNull(
                  adjustmentId, id -> accountActivity.setAdjustmentId(new TypedId<>(id)));
              accountActivity.setNotes(resultSet.getString("notes"));
              BigDecimal iconRef = resultSet.getBigDecimal("expense_details_icon_ref");
              if (iconRef != null) {
                accountActivity.setExpenseDetails(
                    new ExpenseDetails(
                        iconRef.intValue(), resultSet.getString("expense_details_category_name")));
              }

              Array receiptReceiptIds = resultSet.getArray("receipt_receipt_ids");
              if (receiptReceiptIds != null) {
                ResultSet rs = receiptReceiptIds.getResultSet();
                Set<TypedId<ReceiptId>> receiptIds = new HashSet<>();
                while (rs.next()) {
                  receiptIds.add(new TypedId<>(rs.getObject(2, UUID.class)));
                }

                accountActivity.setReceipt(new ReceiptDetails(receiptIds));
              }

              UUID cardId = resultSet.getObject("card_card_id", UUID.class);
              if (cardId != null) {
                accountActivity.setCard(
                    new CardDetails(
                        new TypedId<>(cardId),
                        resultSet.getString("card_last_four"),
                        new RequiredEncryptedStringWithHash(
                            new String(
                                crypto.decrypt(
                                    resultSet.getBytes("card_owner_first_name_encrypted")))),
                        new RequiredEncryptedStringWithHash(
                            new String(
                                crypto.decrypt(
                                    resultSet.getBytes("card_owner_last_name_encrypted")))),
                        (resultSet.getString("card_external_ref"))));
              }

              BigDecimal categoryCode = resultSet.getBigDecimal("merchant_merchant_category_code");
              String merchantName = resultSet.getString("merchant_name");
              if (merchantName != null && categoryCode != null) {
                accountActivity.setMerchant(
                    new MerchantDetails(
                        merchantName,
                        MerchantType.valueOf(resultSet.getString("merchant_type")),
                        resultSet.getString("merchant_merchant_number"),
                        categoryCode.intValue(),
                        MccGroup.valueOf(resultSet.getString("merchant_merchant_category_group")),
                        resultSet.getString("merchant_logo_url"),
                        resultSet.getBigDecimal("merchant_latitude"),
                        resultSet.getBigDecimal("merchant_longitude")));
              }

              accountActivity.setVersion(resultSet.getLong("version"));
              accountActivity.setCreated(resultSet.getObject("created", OffsetDateTime.class));
              accountActivity.setUpdated(resultSet.getObject("updated", OffsetDateTime.class));

              return accountActivity;
            });

    PageToken pageToken = criteria.getPageToken();

    StringWriter outCounter = new StringWriter();
    template.execute(criteria, new JDBCUtils.CountObjectForSqlQuery(true), outCounter);
    long totalElements =
        JDBCUtils.query(
                entityManager,
                outCounter.toString(),
                new MapSqlParameterSource(),
                (resultSet, row) -> resultSet.getLong(1))
            .get(0);

    return new PageImpl<>(
        result, PageRequest.of(pageToken.getPageNumber(), pageToken.getPageSize()), totalElements);
  }

  @Override
  @Transactional(TxType.REQUIRED)
  public DashboardData findDataForLineGraph(
      @NonNull TypedId<BusinessId> businessId, GraphFilterCriteria criteria) {

    entityManager.flush();
    List<GraphData> graphDataList =
        entityManager
            .unwrap(SessionImpl.class)
            .doReturningWork(
                connection -> {
                  StringBuilder stringBuilder =
                      new StringBuilder(
                          "select custom_time_series.startdate, "
                              + " custom_time_series.enddate,  "
                              + " (select coalesce(sum(account_activity.amount_amount), 0) "
                              + "  from account_activity "
                              + "  where account_activity.business_id = ? "
                              + "    and account_activity.type = ? "
                              + "    and account_activity.activity_time >= custom_time_series.startdate "
                              + "    and account_activity.activity_time < custom_time_series.enddate ");
                  BeanUtils.setNotNull(
                      criteria.getUserId(),
                      userId -> stringBuilder.append(" and account_activity.user_id = ? "));
                  BeanUtils.setNotNull(
                      criteria.getAllocationId(),
                      allocationId ->
                          stringBuilder.append(" and account_activity.allocation_id = ? "));
                  stringBuilder.append(
                      " and account_activity.card_card_id is not null ),"
                          + " (select coalesce(count(*), 0) "
                          + "  from account_activity "
                          + "  where account_activity.business_id = ? "
                          + "    and account_activity.type = ? "
                          + "    and account_activity.activity_time >= custom_time_series.startdate "
                          + "    and account_activity.activity_time < custom_time_series.enddate  ");
                  BeanUtils.setNotNull(
                      criteria.getUserId(),
                      userId -> stringBuilder.append(" and account_activity.user_id = ? "));
                  BeanUtils.setNotNull(
                      criteria.getAllocationId(),
                      allocationId ->
                          stringBuilder.append(" and account_activity.allocation_id = ? "));
                  stringBuilder.append(
                      " and account_activity.card_card_id is not null )"
                          + " from (select day as startdate, day + ((( ?::timestamp - ?::timestamp) / ?)) as enddate "
                          + " from generate_series ( ?::timestamp, "
                          + "                        ?::timestamp, "
                          + "                       (( ?::timestamp - ?::timestamp) / ?)) day limit ?) as custom_time_series ");

                  PreparedStatement preparedStatement =
                      connection.prepareStatement(stringBuilder.toString());

                  int parameterIndex = 0;
                  for (int i = 0; i < 2; i++) {
                    preparedStatement.setObject(++parameterIndex, businessId.toUuid());
                    preparedStatement.setObject(
                        ++parameterIndex, AccountActivityType.NETWORK_CAPTURE.name());
                    if (criteria.getUserId() != null) {
                      preparedStatement.setObject(++parameterIndex, criteria.getUserId().toUuid());
                    }
                    if (criteria.getAllocationId() != null) {
                      preparedStatement.setObject(
                          ++parameterIndex, criteria.getAllocationId().toUuid());
                    }
                  }

                  int slices =
                      ChronoUnit.DAYS.between(
                                  criteria.getFrom().toInstant(), criteria.getTo().toInstant())
                              > DEFAULT_SLICES_FOR_WEEK
                          ? DEFAULT_SLICES
                          : DEFAULT_SLICES_FOR_WEEK;

                  preparedStatement.setString(
                      ++parameterIndex, Timestamp.from(criteria.getTo().toInstant()).toString());
                  preparedStatement.setString(
                      ++parameterIndex, Timestamp.from(criteria.getFrom().toInstant()).toString());
                  preparedStatement.setInt(++parameterIndex, slices);
                  preparedStatement.setString(
                      ++parameterIndex, Timestamp.from(criteria.getFrom().toInstant()).toString());
                  preparedStatement.setString(
                      ++parameterIndex, Timestamp.from(criteria.getTo().toInstant()).toString());
                  preparedStatement.setString(
                      ++parameterIndex, Timestamp.from(criteria.getTo().toInstant()).toString());
                  preparedStatement.setString(
                      ++parameterIndex, Timestamp.from(criteria.getFrom().toInstant()).toString());
                  preparedStatement.setInt(++parameterIndex, slices);
                  preparedStatement.setInt(++parameterIndex, slices);

                  preparedStatement.execute();
                  ResultSet resultSet = preparedStatement.getResultSet();

                  String zoneOffset = OffsetDateTime.now().getOffset().getId();
                  List<GraphData> dataList = new ArrayList<>();
                  while (resultSet.next()) {
                    dataList.add(
                        new GraphData(
                            resultSet
                                .getTimestamp(1)
                                .toInstant()
                                .atOffset(ZoneOffset.of(zoneOffset)),
                            resultSet
                                .getTimestamp(2)
                                .toInstant()
                                .atOffset(ZoneOffset.of(zoneOffset)),
                            resultSet.getBigDecimal(3),
                            resultSet.getBigDecimal(4)));
                  }
                  return dataList;
                });

    BigDecimal totalAmount =
        graphDataList.stream().map(GraphData::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    BigDecimal totalNumberOfElements =
        graphDataList.stream().map(GraphData::getCount).reduce(BigDecimal.ZERO, BigDecimal::add);

    if (totalNumberOfElements.equals(BigDecimal.ZERO)) {
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
    // query
    CriteriaBuilder<Tuple> query =
        criteriaBuilderFactory
            .create(entityManager, Tuple.class)
            .from(AccountActivity.class, "accountActivity")
            .where("accountActivity.businessId")
            .eqLiteral(businessId)
            .where("accountActivity.type") // only include posted card transactions
            .eqLiteral(AccountActivityType.NETWORK_CAPTURE)
            .where("accountActivity.activityTime")
            .ge(criteria.getFrom())
            .where("accountActivity.activityTime")
            .lt(criteria.getTo());

    BeanUtils.setNotNull(
        criteria.getUserId(), userId -> query.where("accountActivity.userId").eqLiteral(userId));
    BeanUtils.setNotNull(
        criteria.getAllocationId(),
        allocationId -> query.where("accountActivity.allocationId").eqLiteral(allocationId));

    switch (criteria.getChartFilterType()) {
      case ALLOCATION -> query
          .select("accountActivity.allocationId")
          .select("accountActivity.allocationName")
          .groupBy("accountActivity.allocationId");
      case EMPLOYEE -> query
          .leftJoinOn(User.class, "user")
          .on("accountActivity.userId")
          .eqExpression("user.id")
          .end()
          .select("accountActivity.userId")
          .select("user.type")
          .select("user.firstName")
          .select("user.lastName")
          .groupBy("accountActivity.userId");
      case MERCHANT -> query
          .select("accountActivity.merchant.name")
          .select("accountActivity.merchant.type")
          .select("accountActivity.merchant.merchantNumber")
          .select("accountActivity.merchant.merchantCategoryCode")
          .select("accountActivity.merchant.logoUrl")
          .groupBy("accountActivity.merchant.name");
      case MERCHANT_CATEGORY -> query
          .select("accountActivity.merchant.merchantCategoryCode")
          .select("accountActivity.merchant.type")
          .groupBy("accountActivity.merchant.type");
    }

    query.select("coalesce(sum(accountActivity.amount.amount), 0)", "sumAmount");
    query.select("accountActivity.amount.currency");
    if (criteria.getDirection() == Direction.DESC) {
      query.orderByDesc("sumAmount");
    } else {
      query.orderByAsc("sumAmount");
    }

    query.setMaxResults(LIMIT_SIZE_FOR_CHART);

    List<Tuple> resultList = query.getResultList();

    return switch (criteria.getChartFilterType()) {
      case MERCHANT_CATEGORY -> new ChartData(
          resultList.stream()
              .map(
                  tuple ->
                      new MerchantCategoryChartData(
                          MerchantType.valueOf(tuple.get(1).toString()),
                          new Amount(
                              Currency.valueOf(tuple.get(3).toString()),
                              (BigDecimal) tuple.get(2))))
              .toList(),
          null,
          null,
          null);
      case ALLOCATION -> new ChartData(
          null,
          resultList.stream()
              .map(
                  tuple ->
                      new AllocationChartData(
                          (TypedId<AllocationId>) tuple.get(0),
                          tuple.get(1).toString(),
                          new Amount(
                              Currency.valueOf(tuple.get(3).toString()),
                              (BigDecimal) tuple.get(2))))
              .toList(),
          null,
          null);

      case MERCHANT -> new ChartData(
          null,
          null,
          null,
          resultList.stream()
              .map(
                  tuple ->
                      new MerchantChartData(
                          new Amount(
                              Currency.valueOf(tuple.get(6).toString()), (BigDecimal) tuple.get(5)),
                          (Integer) tuple.get(3),
                          MerchantType.valueOf(tuple.get(1).toString()),
                          tuple.get(0) != null ? tuple.get(0).toString() : "",
                          tuple.get(2) != null ? tuple.get(2).toString() : "",
                          tuple.get(4) != null ? tuple.get(4).toString() : ""))
              .toList());

      case EMPLOYEE -> new ChartData(
          null,
          null,
          resultList.stream()
              .map(
                  tuple ->
                      new UserChartData(
                          (TypedId<UserId>) tuple.get(0),
                          UserType.valueOf(tuple.get(1).toString()),
                          ((RequiredEncryptedStringWithHash) tuple.get(2)).getEncrypted(),
                          ((RequiredEncryptedStringWithHash) tuple.get(3)).getEncrypted(),
                          new Amount(
                              Currency.valueOf(tuple.get(5).toString()),
                              (BigDecimal) tuple.get(4))))
              .toList(),
          null);
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
            .addValue("timeFrom", LocalDate.now().minusDays(daysAgo)),
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
}
