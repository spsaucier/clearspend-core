package com.tranwall.capital.data.repository.impl;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.PagedList;
import com.blazebit.persistence.PaginatedCriteriaBuilder;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.crypto.HashUtil;
import com.tranwall.capital.crypto.data.ByteString;
import com.tranwall.capital.data.model.AccountActivity;
import com.tranwall.capital.data.model.Card;
import com.tranwall.capital.data.repository.AccountActivityRepositoryCustom;
import com.tranwall.capital.service.AccountActivityFilterCriteria;
import com.tranwall.capital.service.type.PageToken;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import lombok.RequiredArgsConstructor;
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
  public Page<FilteredAccountActivityRecord> find(
      TypedId<BusinessId> businessId, AccountActivityFilterCriteria criteria) {
    // query
    CriteriaBuilder<Tuple> criteriaBuilder =
        creCriteriaBuilderFactory.create(entityManager, Tuple.class);

    CriteriaBuilder<Tuple> select =
        criteriaBuilder
            .from(AccountActivity.class, "a")
            .leftJoinOn(Card.class, "c")
            .on("a.card")
            .eqExpression("c")
            .end()
            .select("a")
            .select("c");

    if (businessId != null) {
      select.where("a.businessId").eqLiteral(businessId);
    }
    if (criteria.getUserId() != null) {
      select.where("a.userId").eqLiteral(criteria.getUserId());
    }
    if (criteria.getAllocationId() != null) {
      select.where("a.allocationId").eqLiteral(criteria.getAllocationId());
    }
    if (criteria.getCardId() != null) {
      select.where("a.card.cardId").eqLiteral(criteria.getCardId());
    }
    if (criteria.getType() != null) {
      select.where("a.type").eqLiteral(criteria.getType());
    }
    if (criteria.getFrom() != null || criteria.getTo() != null) {
      select.where("a.activityTime").between(criteria.getFrom()).and(criteria.getTo());
    }

    if (criteria.getSearchText() != null) {
      ByteString byteString = HashUtil.normalizedHash(criteria.getSearchText());
      select
          .whereOr()
          .where("c.cardNumber")
          .like()
          .literal(byteString)
          .noEscape()
          .where("a.merchant.name")
          .like(false)
          .value(criteria.getSearchText())
          .noEscape()
          .where("a.amount.amount")
          .like()
          .value(criteria.getSearchText())
          .noEscape()
          .where("a.activityTime")
          .like()
          .value(criteria.getSearchText())
          .noEscape()
          .endOr();
    }

    select.orderByAsc("a.id");

    PageToken pageToken = criteria.getPageToken();
    int maxResults = pageToken.getPageSize();
    int firstResult = pageToken.getPageNumber() * maxResults;
    PaginatedCriteriaBuilder<Tuple> page = select.page(firstResult, maxResults);
    PagedList<Tuple> paged = page.getResultList();

    return new PageImpl<>(
        paged.stream()
            .map(
                tuple ->
                    new FilteredAccountActivityRecord(
                        tuple.get(0, AccountActivity.class), tuple.get(1, Card.class)))
            .collect(Collectors.toList()),
        PageRequest.of(
            criteria.getPageToken().getPageNumber(), criteria.getPageToken().getPageSize()),
        paged.getTotalSize());
  }
}
