package com.clearspend.capital.data.repository.impl;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.PagedList;
import com.blazebit.persistence.PaginatedCriteriaBuilder;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.BusinessId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.MerchantType;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.repository.AccountActivityRepositoryCustom;
import com.clearspend.capital.service.AccountActivityFilterCriteria;
import com.clearspend.capital.service.BeanUtils;
import com.clearspend.capital.service.type.AllocationChartData;
import com.clearspend.capital.service.type.ChartData;
import com.clearspend.capital.service.type.ChartFilterCriteria;
import com.clearspend.capital.service.type.DashboardData;
import com.clearspend.capital.service.type.GraphData;
import com.clearspend.capital.service.type.GraphFilterCriteria;
import com.clearspend.capital.service.type.MerchantCategoryChartData;
import com.clearspend.capital.service.type.MerchantChartData;
import com.clearspend.capital.service.type.PageToken;
import com.clearspend.capital.service.type.UserChartData;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.internal.SessionImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class AccountActivityRepositoryImpl implements AccountActivityRepositoryCustom {

  public static final int LIMIT_SIZE_FOR_CHART = 5;
  public static final int DEFAULT_SLICES_FOR_WEEK = 7;
  public static final int DEFAULT_SLICES = 10;

  private final EntityManager entityManager;
  private final CriteriaBuilderFactory criteriaBuilderFactory;

  @Override
  public Page<AccountActivity> find(
      @NonNull TypedId<BusinessId> businessId, AccountActivityFilterCriteria criteria) {
    // query
    CriteriaBuilder<AccountActivity> select =
        criteriaBuilderFactory
            .create(entityManager, AccountActivity.class, "accountActivity")
            .select("accountActivity");

    select
        .whereOr()
        .where("accountActivity.hideAfter")
        .ge(OffsetDateTime.now())
        .where("accountActivity.hideAfter")
        .isNull()
        .endOr();
    select
        .whereOr()
        .where("accountActivity.visibleAfter")
        .le(OffsetDateTime.now())
        .where("accountActivity.visibleAfter")
        .isNull()
        .endOr();

    select.where("accountActivity.businessId").eqLiteral(businessId);
    BeanUtils.setNotNull(
        criteria.getUserId(), userId -> select.where("accountActivity.userId").eqLiteral(userId));
    BeanUtils.setNotNull(
        criteria.getAllocationId(),
        allocationId -> select.where("accountActivity.allocationId").eqLiteral(allocationId));
    BeanUtils.setNotNull(
        criteria.getCardId(),
        cardId -> select.where("accountActivity.card.cardId").eqLiteral(cardId));
    BeanUtils.setNotNull(
        criteria.getType(), type -> select.where("accountActivity.type").eqLiteral(type));

    if (criteria.getFrom() != null && criteria.getTo() != null) {
      select
          .where("accountActivity.activityTime")
          .between(criteria.getFrom())
          .and(criteria.getTo());
    }

    String searchText = criteria.getSearchText();
    if (StringUtils.isNotEmpty(searchText)) {
      String likeSearchString = "%" + searchText + "%";
      select
          .whereOr()
          .where("accountActivity.card.lastFour")
          .like()
          .value(likeSearchString)
          .noEscape()
          .where("accountActivity.merchant.name")
          .like()
          .value(likeSearchString)
          .noEscape()
          .where("CAST_STRING(accountActivity.amount.amount)")
          .like()
          .value(likeSearchString)
          .noEscape()
          .where("CAST_STRING(accountActivity.activityTime)")
          .like()
          .value(likeSearchString)
          .noEscape()
          .endOr();
    }

    select.orderByDesc("accountActivity.activityTime");
    select.orderByDesc("accountActivity.id");

    PageToken pageToken = criteria.getPageToken();
    int maxResults = pageToken.getPageSize();
    int firstResult = pageToken.getPageNumber() * maxResults;
    PaginatedCriteriaBuilder<AccountActivity> page = select.page(firstResult, maxResults);
    PagedList<AccountActivity> paged = page.getResultList();

    return new PageImpl<>(
        new ArrayList<>(paged),
        PageRequest.of(
            criteria.getPageToken().getPageNumber(), criteria.getPageToken().getPageSize()),
        paged.getTotalSize());
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
                              + "          from account_activity "
                              + "          where account_activity.business_id = ? "
                              + "          and account_activity.activity_time >= custom_time_series.startdate "
                              + "          and account_activity.activity_time < custom_time_series.enddate ");
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
                          + "          from account_activity "
                          + "          where account_activity.business_id = ? "
                          + "          and account_activity.activity_time >= custom_time_series.startdate "
                          + "          and account_activity.activity_time < custom_time_series.enddate  ");
                  BeanUtils.setNotNull(
                      criteria.getUserId(),
                      userId -> stringBuilder.append(" and account_activity.user_id = ? "));
                  BeanUtils.setNotNull(
                      criteria.getAllocationId(),
                      allocationId ->
                          stringBuilder.append(" and account_activity.allocation_id = ? "));
                  stringBuilder.append(
                      " and account_activity.card_card_id is not null )"
                          + " from (SELECT day as startdate, day + ((( ?::timestamp - ?::timestamp) / ?)) as enddate "
                          + " FROM generate_series ( ?::timestamp, "
                          + "                        ?::timestamp, "
                          + "                       (( ?::timestamp - ?::timestamp) / ?)) day) as custom_time_series ");

                  PreparedStatement preparedStatement =
                      connection.prepareStatement(stringBuilder.toString());
                  int parameterIndex = 0;
                  preparedStatement.setObject(++parameterIndex, businessId.toUuid());
                  if (criteria.getUserId() != null) {
                    preparedStatement.setObject(++parameterIndex, criteria.getUserId().toUuid());
                  }
                  if (criteria.getAllocationId() != null) {
                    preparedStatement.setObject(
                        ++parameterIndex, criteria.getAllocationId().toUuid());
                  }

                  preparedStatement.setObject(++parameterIndex, businessId.toUuid());
                  if (criteria.getUserId() != null) {
                    preparedStatement.setObject(++parameterIndex, criteria.getUserId().toUuid());
                  }
                  if (criteria.getAllocationId() != null) {
                    preparedStatement.setObject(
                        ++parameterIndex, criteria.getAllocationId().toUuid());
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
            .from(AccountActivity.class, "accountActivity");

    query.where("accountActivity.businessId").eqLiteral(businessId);
    BeanUtils.setNotNull(
        criteria.getUserId(), userId -> query.where("accountActivity.userId").eqLiteral(userId));
    BeanUtils.setNotNull(
        criteria.getAllocationId(),
        allocationId -> query.where("accountActivity.allocationId").eqLiteral(allocationId));

    query.where("accountActivity.activityTime").between(criteria.getFrom()).and(criteria.getTo());

    // This will take into consideration only card transaction => spend
    query.where("accountActivity.card.cardId").isNotNull();

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

    query.select("COALESCE(SUM(accountActivity.amount.amount), 0)", "s");
    query.select("accountActivity.amount.currency");
    query.orderByDesc("s");

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
              .collect(Collectors.toList()),
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
              .collect(Collectors.toList()),
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
              .collect(Collectors.toList()));

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
              .collect(Collectors.toList()),
          null);
    };
  }
}
