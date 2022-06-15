package com.clearspend.capital.data.repository.impl;

import com.blazebit.persistence.CriteriaBuilderFactory;
import com.clearspend.capital.common.data.model.TypedMutable;
import com.clearspend.capital.common.data.util.MustacheResourceLoader;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.crypto.data.converter.canonicalization.Canonicalizer;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.repository.UserRepositoryCustom;
import com.clearspend.capital.data.repository.impl.JDBCUtils.MustacheQueryConfig;
import com.clearspend.capital.service.UserFilterCriteria;
import com.clearspend.capital.service.type.CurrentUser;
import com.clearspend.capital.service.type.PageToken;
import com.samskivert.mustache.Template;
import com.vladmihalcea.hibernate.type.array.StringArrayType;
import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.Tuple;
import lombok.NonNull;
import org.hibernate.jpa.TypedParameterValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepositoryImpl implements UserRepositoryCustom {

  private final EntityManager entityManager;
  private final CriteriaBuilderFactory creCriteriaBuilderFactory;
  private final Template findUsersTemplate;

  public UserRepositoryImpl(
      final EntityManager entityManager,
      final CriteriaBuilderFactory creCriteriaBuilderFactory,
      @Value("classpath:db/sql/userRepository/findUsers.sql") final Resource findUsersQuery) {
    this.entityManager = entityManager;
    this.creCriteriaBuilderFactory = creCriteriaBuilderFactory;
    this.findUsersTemplate = MustacheResourceLoader.load(findUsersQuery);
  }

  public record CardAndAllocationName(Card card, String allocationName) {}

  @Override
  public Page<FilteredUserWithCardListRecord> find(
      @NonNull TypedId<BusinessId> businessId, UserFilterCriteria criteria) {

    final boolean includeArchived =
        Optional.ofNullable(criteria.getIncludeArchived()).orElse(false);
    final List<UUID> allocations =
        Optional.ofNullable(criteria.getAllocations())
            .filter(list -> !list.isEmpty())
            .map(list -> list.stream().map(TypedId::toUuid).toList())
            .orElse(null);

    PageToken pageToken = criteria.getPageToken();
    int maxResults = pageToken.getPageSize();
    int firstResult = pageToken.getPageNumber() * maxResults;

    final UUID invokingUser =
        Optional.ofNullable(CurrentUser.getUserId()).map(TypedId::toUuid).orElse(null);
    final String[] globalRoles =
        Optional.ofNullable(CurrentUser.getRoles())
            .map(roles -> roles.toArray(String[]::new))
            .orElse(new String[0]);

    final MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("businessId", businessId.toUuid())
            .addValue("withoutCard", criteria.getWithoutCard())
            .addValue("hasVirtualCard", criteria.getHasVirtualCard())
            .addValue("hasPhysicalCard", criteria.getHasPhysicalCard())
            .addValue("includeArchived", includeArchived)
            .addValue("allocations", allocations)
            .addValue("searchText", criteria.getSearchText())
            .addValue("likeSearchText", "%%%s%%".formatted(criteria.getSearchText()))
            .addValue(
                "hashedName", Canonicalizer.NAME.getCanonicalizedHash(criteria.getSearchText()))
            .addValue(
                "hashedEmail", Canonicalizer.EMAIL.getCanonicalizedHash(criteria.getSearchText()))
            .addValue(
                "hashedPhone", Canonicalizer.PHONE.getCanonicalizedHash(criteria.getSearchText()))
            .addValue("firstResult", firstResult)
            .addValue("pageSize", pageToken.getPageSize())
            .addValue("invokingUser", invokingUser)
            .addValue(
                "globalRoles", new TypedParameterValue(StringArrayType.INSTANCE, globalRoles));

    final List<User> users =
        JDBCUtils.executeMustacheQuery(
                entityManager,
                findUsersTemplate,
                MustacheQueryConfig.<User>builder()
                    .parameterSource(params)
                    .entityClass(User.class)
                    .build())
            .result();

    final long totalElements;
    if (users.size() < pageToken.getPageSize() && pageToken.getPageNumber() == 0) {
      final SqlParameterSource countParams =
          params
              // Difference in behavior between using a JdbcTemplate (below) and a NativeQuery
              // (above) requires this
              .addValue("globalRoles", globalRoles, Types.ARRAY)
              .addValue("count", true);
      totalElements =
          JDBCUtils.executeMustacheQuery(
                  entityManager,
                  findUsersTemplate,
                  MustacheQueryConfig.<Long>builder()
                      .parameterSource(countParams)
                      .rowMapper((resultSet, rowNum) -> resultSet.getLong(1))
                      .build())
              .result()
              .get(0);
    } else {
      totalElements = users.size();
    }

    final List<TypedId<UserId>> userIds = users.stream().map(TypedMutable::getId).toList();

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
            .in(userIds)
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
        users.stream()
            .map(
                user ->
                    new FilteredUserWithCardListRecord(user, cardsGroupByUserId.get(user.getId())))
            .toList(),
        PageRequest.of(
            criteria.getPageToken().getPageNumber(), criteria.getPageToken().getPageSize()),
        totalElements);
  }
}
