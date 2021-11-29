package com.tranwall.capital.service;

import static com.tranwall.capital.data.model.enums.AccountActivityType.REALLOCATE;

import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.error.IdMismatchException;
import com.tranwall.capital.common.error.IdMismatchException.IdType;
import com.tranwall.capital.common.error.RecordNotFoundException;
import com.tranwall.capital.common.error.RecordNotFoundException.Table;
import com.tranwall.capital.common.typedid.data.AccountActivityId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.CardId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.common.typedid.data.UserId;
import com.tranwall.capital.controller.type.activity.AccountActivityResponse;
import com.tranwall.capital.data.model.AccountActivity;
import com.tranwall.capital.data.model.Adjustment;
import com.tranwall.capital.data.model.Allocation;
import com.tranwall.capital.data.model.Hold;
import com.tranwall.capital.data.model.embedded.MerchantDetails;
import com.tranwall.capital.data.model.enums.AccountActivityType;
import com.tranwall.capital.data.model.enums.MerchantType;
import com.tranwall.capital.data.repository.AccountActivityRepository;
import com.tranwall.capital.service.CardService.CardRecord;
import com.tranwall.capital.service.type.NetworkCommon;
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
import org.apache.commons.lang3.StringUtils;
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
      Allocation allocation, AccountActivityType type, Adjustment adjustment) {
    final AccountActivity accountActivity =
        new AccountActivity(
            adjustment.getBusinessId(),
            allocation.getId(),
            allocation.getName(),
            adjustment.getAccountId(),
            type,
            adjustment.getEffectiveDate(),
            adjustment.getAmount());
    accountActivity.setAdjustmentId(adjustment.getId());

    return accountActivityRepository.save(accountActivity);
  }

  @Transactional(TxType.REQUIRED)
  public AccountActivity recordReallocationAccountActivity(
      Allocation allocation, Adjustment adjustment) {
    final AccountActivity accountActivity =
        new AccountActivity(
            adjustment.getBusinessId(),
            allocation.getId(),
            allocation.getName(),
            adjustment.getAccountId(),
            REALLOCATE,
            adjustment.getEffectiveDate(),
            adjustment.getAmount());
    accountActivity.setAdjustmentId(adjustment.getId());

    return accountActivityRepository.save(accountActivity);
  }

  @Transactional(TxType.REQUIRED)
  public AccountActivity recordNetworkHoldAccountAccountActivity(NetworkCommon common, Hold hold) {
    return recordNetworkAccountActivity(common, hold.getAmount(), hold, null);
  }

  @Transactional(TxType.REQUIRED)
  public AccountActivity recordNetworkAdjustmentAccountAccountActivity(
      NetworkCommon common, Adjustment adjustment) {
    return recordNetworkAccountActivity(common, adjustment.getAmount(), null, adjustment);
  }

  @Transactional(TxType.REQUIRED)
  public AccountActivity recordNetworkDeclineAccountAccountActivity(NetworkCommon common) {
    return recordNetworkAccountActivity(common, common.getRequestedAmount(), null, null);
  }

  private AccountActivity recordNetworkAccountActivity(
      NetworkCommon common, Amount amount, Hold hold, Adjustment adjustment) {

    Allocation allocation = common.getAllocation();
    OffsetDateTime activityTime =
        hold != null
            ? hold.getCreated()
            : adjustment != null ? adjustment.getCreated() : OffsetDateTime.now();
    AccountActivity accountActivity =
        new AccountActivity(
            common.getBusinessId(),
            allocation.getId(),
            allocation.getName(),
            common.getAccount().getId(),
            common.getNetworkMessageType().getAccountActivityType(),
            activityTime,
            amount);
    accountActivity.setMerchant(
        new MerchantDetails(
            common.getMerchantName(),
            MerchantType.OTHERS,
            common.getMerchantNumber(),
            common.getMerchantCategoryCode()));
    accountActivity.setCard(
        new com.tranwall.capital.data.model.embedded.CardDetails(common.getCard().getId()));
    if (adjustment != null) {
      accountActivity.setAdjustmentId(adjustment.getId());
    }
    if (hold != null) {
      accountActivity.setHoldId(hold.getId());
    }

    return accountActivityRepository.save(accountActivity);
  }

  public AccountActivity updateAccountActivity(
      TypedId<BusinessId> businessId,
      TypedId<UserId> userId,
      TypedId<AccountActivityId> accountActivityId,
      String notes) {
    AccountActivity accountActivity = getUserAccountActivity(businessId, userId, accountActivityId);
    if (StringUtils.isNotBlank(notes)) {
      accountActivity.setNotes(notes);
    }

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

  public AccountActivity getUserAccountActivity(
      TypedId<BusinessId> businessId,
      TypedId<UserId> userId,
      TypedId<AccountActivityId> accountActivityId) {

    return accountActivityRepository
        .findByBusinessIdAndUserIdAndId(businessId, userId, accountActivityId)
        .orElseThrow(
            () ->
                new RecordNotFoundException(
                    Table.ACCOUNT_ACTIVITY, businessId, userId, accountActivityId));
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
        all.stream().map(AccountActivityResponse::new).collect(Collectors.toList()),
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
        predicates.add(criteriaBuilder.equal(root.get("card").get("cardId"), criteria.getCardId()));
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
                            ? criteriaBuilder.asc(root.get(ord.getItem()))
                            : criteriaBuilder.desc(root.get(ord.getItem())))
                .collect(Collectors.toList()));
      } else {
        query.orderBy(criteriaBuilder.desc(root.get("activityTime")));
      }

      return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
    };
  }
}
