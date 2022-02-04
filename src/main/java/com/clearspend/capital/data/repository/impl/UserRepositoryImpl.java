package com.clearspend.capital.data.repository.impl;

import com.blazebit.persistence.CriteriaBuilder;
import com.blazebit.persistence.CriteriaBuilderFactory;
import com.blazebit.persistence.JoinType;
import com.blazebit.persistence.PagedList;
import com.blazebit.persistence.PaginatedCriteriaBuilder;
import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.crypto.HashUtil;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.repository.UserRepositoryCustom;
import com.clearspend.capital.service.BeanUtils;
import com.clearspend.capital.service.UserFilterCriteria;
import com.clearspend.capital.service.type.PageToken;
import java.util.List;
import java.util.Map;
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
public class UserRepositoryImpl implements UserRepositoryCustom {

  private final EntityManager entityManager;
  private final CriteriaBuilderFactory creCriteriaBuilderFactory;

  public record CardAndAllocationName(Card card, String allocationName) {}

  @Override
  public Page<FilteredUserWithCardListRecord> find(
      @NonNull TypedId<BusinessId> businessId, UserFilterCriteria criteria) {

    CriteriaBuilder<User> userCriteriaBuilder =
        creCriteriaBuilderFactory
            .create(entityManager, User.class)
            .from(User.class, "user")
            .joinOn(Card.class, "card", JoinType.LEFT)
            .on("user.id")
            .eqExpression("card.userId")
            .end()
            .joinOn(Allocation.class, "allocation", JoinType.LEFT)
            .on("card.allocationId")
            .eqExpression("allocation.id")
            .end()
            .select("user");

    userCriteriaBuilder.where("user.businessId").eqLiteral(businessId);

    BeanUtils.setNotNull(
        criteria.getWithoutCard(), withoutCard -> userCriteriaBuilder.where("card").isNull());

    BeanUtils.setNotNull(
        criteria.getHasVirtualCard(),
        hasVirtualCode -> userCriteriaBuilder.where("card.type").eqLiteral(CardType.VIRTUAL));

    BeanUtils.setNotNull(
        criteria.getHasPhysicalCard(),
        hasPlasticCode -> userCriteriaBuilder.where("card.type").eqLiteral(CardType.PHYSICAL));

    BeanUtils.setNotNull(
        criteria.getAllocations(),
        allocations -> userCriteriaBuilder.where("card.allocationId").in(allocations));

    if (criteria.getIncludeArchived() == null || !criteria.getIncludeArchived()) {
      userCriteriaBuilder.where("user.archived").eq(false);
    }

    String searchText = criteria.getSearchText();
    if (StringUtils.isNotEmpty(searchText)) {
      searchText = searchText.trim();
      String likeSearchString = "%" + searchText + "%";
      byte[] encryptedValue = HashUtil.calculateHash(searchText);
      userCriteriaBuilder
          .whereOr()
          .where("card.lastFour")
          .eq(searchText)
          .where("user.firstName.hash")
          .eq(encryptedValue)
          .where("user.lastName.hash")
          .eq(encryptedValue)
          .where("user.email.hash")
          .eq(encryptedValue)
          .where("allocation.name")
          .like(false)
          .value(likeSearchString)
          .noEscape()
          .endOr();
    }

    userCriteriaBuilder.groupBy("user.id");

    // we should order by an unique identifier to apply pagination
    userCriteriaBuilder.orderByDesc("user.created");
    userCriteriaBuilder.orderByDesc("user.id");

    PageToken pageToken = criteria.getPageToken();
    int maxResults = pageToken.getPageSize();
    int firstResult = pageToken.getPageNumber() * maxResults;
    PaginatedCriteriaBuilder<User> page = userCriteriaBuilder.page(firstResult, maxResults);
    PagedList<User> paged = page.getResultList();

    List<TypedId<UserId>> listOfUserId = paged.stream().map(TypedMutable::getId).toList();

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
            .toList(),
        PageRequest.of(
            criteria.getPageToken().getPageNumber(), criteria.getPageToken().getPageSize()),
        paged.getTotalSize());
  }
}
