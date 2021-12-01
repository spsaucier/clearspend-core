package com.tranwall.capital.data.repository.impl;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.PagedList;
import com.blazebit.persistence.PaginatedCriteriaBuilder;
import com.tranwall.capital.common.data.model.TypedMutable;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.data.model.Card;
import com.tranwall.capital.data.model.User;
import com.tranwall.capital.data.repository.UserRepositoryCustom;
import com.tranwall.capital.service.UserFilterCriteria;
import com.tranwall.capital.service.type.PageToken;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
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
    Map<TypedId<UserId>, List<Card>> cardsGroupByUserId =
        creCriteriaBuilderFactory
            .create(entityManager, Card.class)
            .from(Card.class, "card")
            .select("card")
            .where("card.userId")
            .in(listOfUserId)
            .getResultList()
            .stream()
            .collect(Collectors.groupingBy(Card::getUserId));

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
