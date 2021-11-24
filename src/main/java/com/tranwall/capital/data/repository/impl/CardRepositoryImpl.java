package com.tranwall.capital.data.repository.impl;

import com.tranwall.capital.crypto.HashUtil;
import com.tranwall.capital.data.model.Account;
import com.tranwall.capital.data.model.Allocation;
import com.tranwall.capital.data.model.Card;
import com.tranwall.capital.data.model.User;
import com.tranwall.capital.data.repository.CardRepositoryCustom;
import com.tranwall.capital.service.CardFilterCriteria;
import com.tranwall.capital.service.type.PageToken;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class CardRepositoryImpl implements CardRepositoryCustom {

  private final EntityManager em;

  @Override
  public Page<FilteredCardRecord> find(CardFilterCriteria criteria) {
    // query
    CriteriaBuilder cb = em.getCriteriaBuilder();
    CriteriaQuery<Tuple> query = cb.createTupleQuery();

    // roots
    Root<Card> cardRoot = query.from(Card.class);
    Root<Allocation> allocationRoot = query.from(Allocation.class);
    Root<Account> accountRoot = query.from(Account.class);
    Root<User> userRoot = query.from(User.class);

    // predicates
    List<Predicate> predicates = new ArrayList<>();
    predicates.add(cb.equal(cardRoot.get("allocationId"), allocationRoot.get("id")));
    predicates.add(cb.equal(allocationRoot.get("accountId"), accountRoot.get("id")));
    predicates.add(cb.equal(cardRoot.get("userId"), userRoot.get("id")));

    if (criteria.getBusinessId() != null) {
      predicates.add(cb.equal(cardRoot.get("businessId"), criteria.getBusinessId()));
    }

    if (criteria.getUserId() != null) {
      predicates.add(cb.equal(cardRoot.get("userId"), criteria.getUserId()));
    }

    if (criteria.getAllocationId() != null) {
      predicates.add(cb.equal(allocationRoot.get("id"), criteria.getAllocationId()));
    }

    if (criteria.getSearchText() != null) {
      byte[] encryptedValue = HashUtil.calculateHash(criteria.getSearchText());
      predicates.add(
          cb.or(
              cb.equal(cardRoot.get("cardNumber").get("hash"), encryptedValue),
              cb.equal(userRoot.get("firstName").get("hash"), encryptedValue),
              cb.equal(userRoot.get("lastName").get("hash"), encryptedValue),
              cb.like(
                  cb.lower(allocationRoot.get("name")),
                  "%" + criteria.getSearchText().toLowerCase() + "%")));
    }

    // execute query
    query
        .multiselect(cardRoot, allocationRoot, accountRoot, userRoot)
        .where(cb.and(predicates.toArray(new Predicate[0])));

    PageToken pageToken = criteria.getPageToken();

    List<Tuple> results =
        em.createQuery(query)
            .setFirstResult(pageToken.getPageNumber())
            .setMaxResults(pageToken.getPageSize())
            .getResultList();

    // total number of elements
    long totalElements;
    if (pageToken.getPageNumber() == 0 && results.size() < pageToken.getPageSize()) {
      totalElements = results.size();
    } else {
      CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
      countQuery.select(cb.count(cardRoot)).where(predicates.toArray(new Predicate[0]));
      totalElements = em.createQuery(countQuery).getSingleResult();
    }

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
        PageRequest.of(pageToken.getPageNumber(), pageToken.getPageSize()),
        totalElements);
  }
}
