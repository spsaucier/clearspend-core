package com.tranwall.capital.data.repository.impl;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.JoinType;
import com.blazebit.persistence.PagedList;
import com.tranwall.capital.crypto.HashUtil;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Allocation;
import com.tranwall.capital.data.model.Card;
import com.tranwall.capital.data.model.User;
import com.tranwall.capital.data.repository.CardRepositoryCustom;
import com.tranwall.capital.service.CardFilterCriteria;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CardRepositoryImpl implements CardRepositoryCustom {

  private final EntityManager entityManager;
  private final CriteriaBuilderFactory criteriaBuilderFactory;

  @Override
  public Page<FilteredCardRecord> find(CardFilterCriteria criteria) {
    CriteriaBuilder<Tuple> builder =
        criteriaBuilderFactory
            .create(entityManager, Tuple.class)
            .from(Card.class, "card")
            // allocation join
            .joinOn(Allocation.class, "allocation", JoinType.INNER)
            .on("card.allocationId")
            .eqExpression("allocation.id")
            .end()
            // account join
            .joinOn(Account.class, "account", JoinType.INNER)
            .on("allocation.accountId")
            .eqExpression("account.id")
            .end()
            // user join
            .joinOn(User.class, "user", JoinType.INNER)
            .on("card.userId")
            .eqExpression("user.id")
            .end();

    if (criteria.getBusinessId() != null) {
      builder.where("card.businessId").eq(criteria.getBusinessId());
    }

    if (criteria.getUserId() != null) {
      builder.where("card.userId").eq(criteria.getUserId());
    }

    if (criteria.getAllocationId() != null) {
      builder.where("allocation.id").eq(criteria.getAllocationId());
    }

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

    PagedList<Tuple> results =
        builder
            .select("card")
            .select("allocation")
            .select("account")
            .select("user")
            .orderByAsc("card.id")
            .page(
                criteria.getPageToken().getPageNumber().intValue(),
                criteria.getPageToken().getPageSize().intValue())
            .getResultList();

    return new PageImpl<>(
        results.stream()
            .map(
                r ->
                    new FilteredCardRecord(
                        r.get(0, Card.class),
                        r.get(1, Allocation.class),
                        r.get(2, Account.class),
                        r.get(3, User.class)))
            .collect(Collectors.toList()),
        PageRequest.of(
            criteria.getPageToken().getPageNumber(), criteria.getPageToken().getPageSize()),
        results.getTotalSize());
  }
}
