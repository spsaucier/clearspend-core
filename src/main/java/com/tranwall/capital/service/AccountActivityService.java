package com.tranwall.capital.service;

import static com.tranwall.capital.data.model.enums.AccountActivityType.REALLOCATE;

import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.error.IdMismatchException;
import com.tranwall.capital.common.error.IdMismatchException.IdType;
import com.tranwall.capital.common.typedid.data.AccountId;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.CardId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.controller.type.activity.AccountActivityResponse;
import com.tranwall.capital.controller.type.activity.CardDetails;
import com.tranwall.capital.controller.type.activity.Merchant;
import com.tranwall.capital.data.model.AccountActivity;
import com.tranwall.capital.data.model.Adjustment;
import com.tranwall.capital.data.model.Allocation;
import com.tranwall.capital.data.model.Hold;
import com.tranwall.capital.data.model.enums.AccountActivityType;
import com.tranwall.capital.data.repository.AccountActivityRepository;
import com.tranwall.capital.service.CardService.CardRecord;
import com.tranwall.capital.service.type.PageToken;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.criteria.Predicate;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountActivityService {

  public static final int PAGE_SIZE = 20;
  private final AccountActivityRepository accountActivityRepository;

  private final CardService cardService;

  @Transactional(TxType.REQUIRED)
  public AccountActivity recordBankAccountAccountActivity(
      AccountActivityType type, Adjustment adjustment) {
    return recordAccountActivity(
        adjustment.getBusinessId(),
        null,
        null,
        adjustment.getAccountId(),
        type,
        adjustment.getEffectiveDate(),
        adjustment.getAmount());
  }

  @Transactional(TxType.REQUIRED)
  public AccountActivity recordReallocationAccountActivity(
      String allocationName, Adjustment adjustment) {
    return recordAccountActivity(
        adjustment.getBusinessId(),
        adjustment.getAllocationId(),
        allocationName,
        adjustment.getAccountId(),
        REALLOCATE,
        adjustment.getEffectiveDate(),
        adjustment.getAmount());
  }

  @Transactional(TxType.REQUIRED)
  public AccountActivity recordNetworkHoldAccountAccountActivity(
      AccountActivityType type, Allocation allocation, Hold hold) {
    return recordAccountActivity(
        hold.getBusinessId(),
        allocation.getId(),
        allocation.getName(),
        hold.getAccountId(),
        type,
        hold.getCreated(),
        hold.getAmount());
  }

  @Transactional(TxType.REQUIRED)
  public AccountActivity recordNetworkAdjustmentAccountAccountActivity(
      AccountActivityType type, Allocation allocation, Adjustment adjustment) {
    return recordAccountActivity(
        adjustment.getBusinessId(),
        allocation.getId(),
        allocation.getName(),
        adjustment.getAccountId(),
        type,
        adjustment.getCreated(),
        adjustment.getAmount());
  }

  private AccountActivity recordAccountActivity(
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      String allocationName,
      TypedId<AccountId> accountId,
      AccountActivityType type,
      OffsetDateTime activityTime,
      Amount amount) {

    AccountActivity accountActivity =
        new AccountActivity(businessId, accountId, type, activityTime, amount);
    accountActivity.setAllocationId(allocationId);
    accountActivity.setAllocationName(allocationName);

    return accountActivityRepository.save(accountActivity);
  }

  public Page<AccountActivityResponse> getCardAccountActivity(
      TypedId<BusinessId> businessId,
      TypedId<UserId> userId,
      TypedId<CardId> cardId,
      AccountActivityFilterCriteria accountActivityFilterCriteria) {
    CardRecord card = cardService.getCard(businessId, cardId);
    if (!card.card().getUserId().equals(userId)) {
      throw new IdMismatchException(IdType.USER_ID, userId, card.card().getUserId());
    }

    accountActivityFilterCriteria.setCardId(card.card().getId());
    accountActivityFilterCriteria.setAllocationId(card.card().getAllocationId());
    accountActivityFilterCriteria.setAccountId(card.card().getAccountId());

    return getFilteredAccountActivity(businessId, accountActivityFilterCriteria);
  }

  public Page<AccountActivityResponse> getFilteredAccountActivity(
      TypedId<BusinessId> businessId, AccountActivityFilterCriteria accountActivityFilterCriteria) {

    PageToken pageToken = accountActivityFilterCriteria.getPageToken();
    Page<AccountActivity> all =
        accountActivityRepository.findAll(
            getAccountActivitySpecifications(businessId, accountActivityFilterCriteria),
            org.springframework.data.domain.PageRequest.of(
                pageToken.getPageNumber(), pageToken.getPageSize()));

    // TODO(kuchlein): we may want to see if we can avoid passing back what is in effect an API type
    return new PageImpl<>(
        all.stream()
            .map(
                accountActivity ->
                    new AccountActivityResponse(
                        accountActivity.getActivityTime(),
                        accountActivity.getAllocationName(),
                        new CardDetails(accountActivity.getCard()),
                        new Merchant(accountActivity.getMerchant()),
                        accountActivity.getType(),
                        accountActivity.getAmount()))
            .collect(Collectors.toList()),
        all.getPageable(),
        all.getTotalElements());
  }

  private Specification<AccountActivity> getAccountActivitySpecifications(
      TypedId<BusinessId> businessId, AccountActivityFilterCriteria criteria) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (businessId != null) {
        predicates.add(criteriaBuilder.equal(root.get("businessId"), businessId));
      }
      if (criteria.getAccountId() != null) {
        predicates.add(criteriaBuilder.equal(root.get("accountId"), criteria.getAccountId()));
      }
      if (criteria.getAllocationId() != null) {
        predicates.add(criteriaBuilder.equal(root.get("allocationId"), criteria.getAllocationId()));
      }
      if (criteria.getCardId() != null) {
        predicates.add(criteriaBuilder.equal(root.get("cardId"), criteria.getCardId()));
      }
      if (criteria.getType() != null) {
        predicates.add(criteriaBuilder.equal(root.get("type"), criteria.getType()));
      }
      if (criteria.getFrom() != null) {
        predicates.add(
            criteriaBuilder.greaterThanOrEqualTo(root.get("activityTime"), criteria.getFrom()));
      }
      if (criteria.getTo() != null) {
        predicates.add(criteriaBuilder.lessThan(root.get("activityTime"), criteria.getTo()));
      }

      if (criteria.getPageToken() != null
          && criteria.getPageToken().getOrderBy() != null
          && !criteria.getPageToken().getOrderBy().isEmpty()) {

        query.orderBy(
            criteria.getPageToken().getOrderBy().stream()
                .map(
                    ord ->
                        ord.getDirection() == Direction.ASC
                            ? criteriaBuilder.asc(root.get(ord.getItem().getName()))
                            : criteriaBuilder.desc(root.get(ord.getItem().getName())))
                .collect(Collectors.toList()));
      } else {
        query.orderBy(criteriaBuilder.desc(root.get("activityTime")));
      }

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }
}
