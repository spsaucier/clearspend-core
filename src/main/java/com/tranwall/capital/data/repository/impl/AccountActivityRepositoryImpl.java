package com.tranwall.capital.data.repository.impl;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.PagedList;
import com.blazebit.persistence.PaginatedCriteriaBuilder;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.data.model.AccountActivity;
import com.tranwall.capital.data.repository.AccountActivityRepositoryCustom;
import com.tranwall.capital.service.AccountActivityFilterCriteria;
import com.tranwall.capital.service.type.PageToken;
import java.util.ArrayList;
import javax.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class AccountActivityRepositoryImpl implements AccountActivityRepositoryCustom {

  private final EntityManager entityManager;
  private final CriteriaBuilderFactory creCriteriaBuilderFactory;

  @Override
  public Page<AccountActivity> find(
      TypedId<BusinessId> businessId, AccountActivityFilterCriteria criteria) {
    // query
    CriteriaBuilder<AccountActivity> select =
        creCriteriaBuilderFactory
            .create(entityManager, AccountActivity.class, "accountActivity")
            .select("accountActivity");

    if (businessId != null) {
      select.where("accountActivity.businessId").eqLiteral(businessId);
    }
    if (criteria.getUserId() != null) {
      select.where("accountActivity.userId").eqLiteral(criteria.getUserId());
    }
    if (criteria.getAllocationId() != null) {
      select.where("accountActivity.allocationId").eqLiteral(criteria.getAllocationId());
    }
    if (criteria.getCardId() != null) {
      select.where("accountActivity.card.cardId").eqLiteral(criteria.getCardId());
    }
    if (criteria.getType() != null) {
      select.where("accountActivity.type").eqLiteral(criteria.getType());
    }
    if (criteria.getFrom() != null || criteria.getTo() != null) {
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

    select.orderByAsc("accountActivity.id");

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
}
