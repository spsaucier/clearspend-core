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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AccountActivityRepositoryImpl implements AccountActivityRepositoryCustom {

  public static final int LIMIT_SIZE_FOR_CHART = 4;

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
  public DashboardData findDataForLineGraph(
      @NonNull TypedId<BusinessId> businessId, GraphFilterCriteria criteria) {
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

    // This will take into consideration only card transactionm => spend
    query.where("accountActivity.card.cardId").isNotNull();

    query.orderByAsc("accountActivity.activityTime");
    query.orderByAsc("accountActivity.id");

    List<GraphData> graphDataList =
        query
            .select("accountActivity.activityTime")
            .select("accountActivity.amount.amount")
            .getResultList()
            .stream()
            .map(tuple -> new GraphData((BigDecimal) tuple.get(1), (OffsetDateTime) tuple.get(0)))
            .collect(Collectors.toList());
    BigDecimal totalAmount =
        graphDataList.stream()
            .map(GraphData::getAmount)
            .reduce(BigDecimal::add)
            .orElse(BigDecimal.ZERO);
    return new DashboardData(totalAmount, graphDataList);
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

    // This will take into consideration only card transactionm => spend
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
          .groupBy("accountActivity.merchant.name");
      case MERCHANT_CATEGORY -> query
          .select("accountActivity.merchant.merchantCategoryCode")
          .select("accountActivity.merchant.type")
          .groupBy("accountActivity.merchant.type");
    }

    query.select("COALESCE(SUM(accountActivity.amount.amount), 0)", "s");
    query.select("accountActivity.amount.currency");
    query.orderByDesc("s");

    CriteriaBuilder<Tuple> queryOthers = query.copy(Tuple.class);
    queryOthers.setFirstResult(LIMIT_SIZE_FOR_CHART);

    query.setMaxResults(LIMIT_SIZE_FOR_CHART);

    List<Tuple> resultList = query.getResultList();

    ChartData chartData =
        switch (criteria.getChartFilterType()) {
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
                                  Currency.valueOf(tuple.get(5).toString()),
                                  (BigDecimal) tuple.get(4)),
                              (Integer) tuple.get(3),
                              MerchantType.valueOf(tuple.get(1).toString()),
                              tuple.get(2).toString(),
                              tuple.get(0).toString()))
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

    if (resultList.size() == LIMIT_SIZE_FOR_CHART) {
      List<Tuple> resultListForOthers = queryOthers.getResultList();
      BigDecimal amount =
          resultListForOthers.stream()
              .map(tuple -> new BigDecimal(tuple.get(tuple.getElements().size() - 2).toString()))
              .reduce(BigDecimal::add)
              .orElse(BigDecimal.ZERO);
      if (amount.doubleValue() > 0) {
        chartData.addOtherCategory(criteria.getChartFilterType(), amount);
      }
    }

    return chartData;
  }
}
