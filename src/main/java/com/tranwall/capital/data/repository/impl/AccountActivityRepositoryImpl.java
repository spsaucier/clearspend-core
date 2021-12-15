package com.tranwall.capital.data.repository.impl;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.PagedList;
import com.blazebit.persistence.PaginatedCriteriaBuilder;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.tranwall.capital.data.model.AccountActivity;
import com.tranwall.capital.data.model.User;
import com.tranwall.capital.data.repository.AccountActivityRepositoryCustom;
import com.tranwall.capital.service.AccountActivityFilterCriteria;
import com.tranwall.capital.service.BeanUtils;
import com.tranwall.capital.service.type.ChartFilterCriteria;
import com.tranwall.capital.service.type.DashboardData;
import com.tranwall.capital.service.type.GraphData;
import com.tranwall.capital.service.type.GraphFilterCriteria;
import com.tranwall.capital.service.type.PageToken;
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

  public record ChartData(String name, BigDecimal amount) {}

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
  public List<ChartData> findDataForChart(
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
          .select("user.firstName")
          .select("user.lastName")
          .groupBy("accountActivity.userId");
      case MERCHANT -> query
          .select("accountActivity.merchant.name")
          .groupBy("accountActivity.merchant.name");
      case MERCHANT_CATEGORY -> query
          .select("accountActivity.merchant.merchantCategoryCode")
          .select("accountActivity.merchant.type")
          .groupBy("accountActivity.merchant.type");
    }

    query.select("COALESCE(SUM(accountActivity.amount.amount), 0)", "s");
    query.orderByDesc("s");

    CriteriaBuilder<Tuple> queryOthers = query.copy(Tuple.class);
    queryOthers.setFirstResult(LIMIT_SIZE_FOR_CHART);

    query.setMaxResults(LIMIT_SIZE_FOR_CHART);

    List<Tuple> resultList = query.getResultList();

    List<ChartData> chartDataList =
        resultList.stream()
            .map(
                tuple ->
                    switch (criteria.getChartFilterType()) {
                      case MERCHANT_CATEGORY, ALLOCATION -> new ChartData(
                          tuple.get(1).toString(), (BigDecimal) tuple.get(2));
                      case MERCHANT -> new ChartData(
                          (String) tuple.get(0), (BigDecimal) tuple.get(1));
                      case EMPLOYEE -> new ChartData(
                          ((RequiredEncryptedStringWithHash) tuple.get(1)).getEncrypted()
                              + " "
                              + ((RequiredEncryptedStringWithHash) tuple.get(2)).getEncrypted(),
                          (BigDecimal) tuple.get(3));
                    })
            .collect(Collectors.toList());
    if (resultList.size() == LIMIT_SIZE_FOR_CHART) {
      List<Tuple> resultListForOthers = queryOthers.getResultList();
      BigDecimal amount =
          resultListForOthers.stream()
              .map(tuple -> new BigDecimal(tuple.get(tuple.getElements().size() - 1).toString()))
              .reduce(BigDecimal::add)
              .orElse(BigDecimal.ZERO);
      if (amount.doubleValue() > 0) {
        chartDataList.add(new ChartData("Others", amount));
      }
    }

    return chartDataList;
  }
}
