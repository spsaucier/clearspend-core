package com.tranwall.capital.data.repository.impl;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.PagedList;
import com.blazebit.persistence.PaginatedCriteriaBuilder;
import com.tranwall.capital.common.data.model.TypedMutable;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.data.model.Allocation;
import com.tranwall.capital.data.model.Card;
import com.tranwall.capital.data.model.User;
import com.tranwall.capital.data.repository.UserRepositoryCustom;
import com.tranwall.capital.service.UserFilterCriteria;
import com.tranwall.capital.service.type.PageToken;
import java.util.List;
import java.util.Map;
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
public class UserRepositoryImpl implements UserRepositoryCustom {

  private final EntityManager entityManager;
  private final CriteriaBuilderFactory creCriteriaBuilderFactory;

  public record CardAndAllocationName(Card card, String allocationName) {}

  @Override
  public Page<FilteredUserWithCardListRecord> find(
      TypedId<BusinessId> businessId, UserFilterCriteria criteria) {
    // query
    CriteriaBuilder<User> userCriteriaBuilder =
        creCriteriaBuilderFactory
            .create(entityManager, User.class)
            .from(User.class, "user")
            .select("user");

    if (businessId != null) {
      userCriteriaBuilder.where("user.businessId").eqLiteral(businessId);
    }

    userCriteriaBuilder.orderByAsc("user.id");

    PageToken pageToken = criteria.getPageToken();
    int maxResults = pageToken.getPageSize();
    int firstResult = pageToken.getPageNumber() * maxResults;
    PaginatedCriteriaBuilder<User> page = userCriteriaBuilder.page(firstResult, maxResults);
    PagedList<User> paged = page.getResultList();

    List<TypedId<UserId>> listOfUserId =
        paged.stream().map(TypedMutable::getId).collect(Collectors.toList());

    // query
    Map<TypedId<UserId>, List<CardAndAllocationName>> cardsGroupByUserId =
        creCriteriaBuilderFactory
            .create(entityManager, Tuple.class)
            .from(Card.class, "card")
            .leftJoinOn(Allocation.class, "allocation")
            .on("card.allocationId")
            .eqExpression("allocation")
            .end()
            .select("card")
            .select("allocation.name")
            .where("card.userId")
            .in(listOfUserId)
            .getResultList()
            .stream()
            .map(
                tuple -> {
                  Card card = (Card) tuple.get(0);
                  return new CardAndAllocationName(card, tuple.get(1).toString());
                })
            .collect(
                Collectors.groupingBy(
                    cardAndAllocationName -> cardAndAllocationName.card.getUserId()));

    return new PageImpl<>(
        paged.stream()
            .map(
                user ->
                    new FilteredUserWithCardListRecord(user, cardsGroupByUserId.get(user.getId())))
            .collect(Collectors.toList()),
        PageRequest.of(
            criteria.getPageToken().getPageNumber(), criteria.getPageToken().getPageSize()),
        paged.getTotalSize());
  }
}
