package com.tranwall.capital.service;

import com.tranwall.capital.common.data.model.Amount;
import com.tranwall.capital.common.typedid.data.AccountId;
import com.tranwall.capital.common.typedid.data.AllocationId;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.activity.AccountActivityRequest;
import com.tranwall.capital.controller.type.activity.AccountActivityResponse;
import com.tranwall.capital.controller.type.activity.CardDetails;
import com.tranwall.capital.controller.type.activity.Merchant;
import com.tranwall.capital.controller.type.activity.PageRequest;
import com.tranwall.capital.data.model.AccountActivity;
import com.tranwall.capital.data.model.Adjustment;
import com.tranwall.capital.data.model.enums.AccountActivityType;
import com.tranwall.capital.data.repository.AccountActivityRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
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

  @Transactional(TxType.REQUIRED)
  public AccountActivity recordAccountActivity(
      TypedId<BusinessId> businessId,
      TypedId<AllocationId> allocationId,
      TypedId<AccountId> accountId,
      AccountActivityType type,
      OffsetDateTime activityTime,
      Amount amount) {
    AccountActivity accountActivity =
        new AccountActivity(businessId, accountId, type, "", activityTime, amount);
    accountActivity.setAllocationId(allocationId);

    return accountActivityRepository.save(accountActivity);
  }

  @Transactional(TxType.REQUIRED)
  public AccountActivity recordAccountActivity(AccountActivityType type, Adjustment adjustment) {
    AccountActivity accountActivity =
        new AccountActivity(
            adjustment.getBusinessId(),
            adjustment.getAccountId(),
            type,
            "Allocation Name", // TODO allocation Record or specific name of activity record
            adjustment.getEffectiveDate(),
            adjustment.getAmount());
    accountActivity.setAllocationId(adjustment.getAllocationId());

    return accountActivityRepository.save(accountActivity);
  }

  public Page<AccountActivityResponse> getFilteredAccountActivity(
      TypedId<BusinessId> businessId, AccountActivityRequest accountActivityRequest) {

    PageRequest pageRequest = accountActivityRequest.getPageRequest();
    Page<AccountActivity> all =
        accountActivityRepository.findAll(
            getAccountActivitySpecifications(businessId, accountActivityRequest),
            pageRequest != null
                ? org.springframework.data.domain.PageRequest.of(
                    pageRequest.getPageNumber(), pageRequest.getPageSize())
                : org.springframework.data.domain.PageRequest.ofSize(PAGE_SIZE));
    return new PageImpl<>(
        all.stream()
            .map(mapAccountActivityToAccountActivityResponse())
            .collect(Collectors.toList()),
        all.getPageable(),
        all.getTotalElements());
  }

  private Function<AccountActivity, AccountActivityResponse>
      mapAccountActivityToAccountActivityResponse() {
    return accountActivity ->
        AccountActivityResponse.builder()
            .activityTime(accountActivity.getActivityTime())
            .accountName(accountActivity.getAccountId().getName())
            .card(
                accountActivity.getCard() != null
                    ? new CardDetails(
                        accountActivity.getCard().getNumber(),
                        accountActivity.getCard().getOwner().toString())
                    : null)
            .merchant(
                accountActivity.getMerchant() != null
                    ? new Merchant(
                        accountActivity.getMerchant().getName(),
                        accountActivity.getMerchant().getType())
                    : null)
            .amount(accountActivity.getAmount())
            .type(accountActivity.getType())
            .build();
  }

  private Specification<AccountActivity> getAccountActivitySpecifications(
      TypedId<BusinessId> businessId, AccountActivityRequest request) {
    return (root, query, criteriaBuilder) -> {
      List<Predicate> predicates = new ArrayList<>();
      if (businessId != null) {
        predicates.add(criteriaBuilder.equal(root.get("businessId"), businessId));
      }
      if (request.getAccountId() != null) {
        predicates.add(criteriaBuilder.equal(root.get("accountId"), request.getAccountId()));
      }
      if (request.getAllocationId() != null) {
        predicates.add(criteriaBuilder.equal(root.get("allocationId"), request.getAllocationId()));
      }
      if (request.getType() != null) {
        predicates.add(criteriaBuilder.equal(root.get("type"), request.getType()));
      }
      if (request.getFrom() != null && request.getTo() != null) {
        predicates.add(
            criteriaBuilder.between(root.get("activityTime"), request.getFrom(), request.getTo()));
      }

      if (request.getPageRequest() != null
          && request.getPageRequest().getOrderable() != null
          && request.getPageRequest().getOrderable().size() > 0) {

        query.orderBy(
            request.getPageRequest().getOrderable().stream()
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
