package com.clearspend.capital.service;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.error.IdMismatchException;
import com.clearspend.capital.common.error.IdMismatchException.IdType;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.RecordNotFoundException.Table;
import com.clearspend.capital.common.typedid.data.AccountActivityId;
import com.clearspend.capital.common.typedid.data.AdjustmentId;
import com.clearspend.capital.common.typedid.data.BusinessId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.Adjustment;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Hold;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.embedded.CardDetails;
import com.clearspend.capital.data.model.embedded.MerchantDetails;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.data.repository.CardRepositoryCustom.CardDetailsRecord;
import com.clearspend.capital.service.type.NetworkCommon;
import java.time.OffsetDateTime;
import java.util.Optional;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountActivityService {

  private final AccountActivityRepository accountActivityRepository;

  private final CardService cardService;

  private final UserService userService;

  @Transactional(TxType.REQUIRED)
  public void recordBankAccountAccountActivity(
      Allocation allocation, AccountActivityType type, Adjustment adjustment, Hold hold) {
    AccountActivity adjustmentAccountActivity =
        new AccountActivity(
            adjustment.getBusinessId(),
            allocation.getId(),
            allocation.getName(),
            adjustment.getAccountId(),
            type,
            AccountActivityStatus.PROCESSED,
            adjustment.getEffectiveDate(),
            adjustment.getAmount());
    adjustmentAccountActivity.setAdjustmentId(adjustment.getId());

    if (hold != null) {
      adjustmentAccountActivity.setVisibleAfter(hold.getExpirationDate());

      AccountActivity holdAccountActivity =
          new AccountActivity(
              adjustment.getBusinessId(),
              allocation.getId(),
              allocation.getName(),
              adjustment.getAccountId(),
              type,
              AccountActivityStatus.PENDING,
              hold.getCreated(),
              adjustment.getAmount());
      holdAccountActivity.setHideAfter(hold.getExpirationDate());

      accountActivityRepository.save(holdAccountActivity);
    }

    accountActivityRepository.save(adjustmentAccountActivity);
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
            AccountActivityType.REALLOCATE,
            AccountActivityStatus.PROCESSED,
            adjustment.getEffectiveDate(),
            adjustment.getAmount());
    accountActivity.setAdjustmentId(adjustment.getId());

    return accountActivityRepository.save(accountActivity);
  }

  @Transactional(TxType.REQUIRED)
  public void recordNetworkHoldAccountActivity(NetworkCommon common, Hold hold) {
    recordNetworkAccountActivity(common, hold.getAmount(), hold, null);
  }

  @Transactional(TxType.REQUIRED)
  public void recordNetworkHoldReleaseAccountActivity(Hold hold) {
    Optional<AccountActivity> accountActivityOptional =
        accountActivityRepository.findByHoldId(hold.getId());
    if (accountActivityOptional.isPresent()) {
      AccountActivity accountActivity = accountActivityOptional.get();
      accountActivity.setHideAfter(OffsetDateTime.now());
      accountActivityRepository.save(accountActivity);
    }
  }

  @Transactional(TxType.REQUIRED)
  public void recordNetworkAdjustmentAccountActivity(NetworkCommon common, Adjustment adjustment) {
    recordNetworkAccountActivity(common, adjustment.getAmount(), null, adjustment);
  }

  @Transactional(TxType.REQUIRED)
  public void recordNetworkDeclineAccountActivity(NetworkCommon common) {
    recordNetworkAccountActivity(common, common.getRequestedAmount(), null, null);
  }

  private void recordNetworkAccountActivity(
      NetworkCommon common, Amount amount, Hold hold, Adjustment adjustment) {

    Allocation allocation = common.getAllocation();
    AccountActivity accountActivity =
        new AccountActivity(
            common.getBusinessId(),
            allocation.getId(),
            allocation.getName(),
            common.getAccount().getId(),
            common.getNetworkMessageType().getAccountActivityType(),
            common.getAccountActivityDetails().getAccountActivityStatus(),
            common.getAccountActivityDetails().getActivityTime(),
            amount);
    accountActivity.setUserId(common.getCard().getUserId());

    accountActivity.setMerchant(
        new MerchantDetails(
            common.getMerchantName(),
            common.getMerchantType(),
            common.getMerchantNumber(),
            common.getMerchantCategoryCode(),
            common.getAccountActivityDetails().getMerchantLogoUrl(),
            common.getAccountActivityDetails().getMerchantLatitude(),
            common.getAccountActivityDetails().getMerchantLongitude()));

    User cardOwner = userService.retrieveUser(common.getCard().getUserId());
    accountActivity.setCard(
        new CardDetails(
            common.getCard().getId(),
            common.getCard().getLastFour(),
            cardOwner.getFirstName(),
            cardOwner.getLastName()));

    if (adjustment != null) {
      accountActivity.setAdjustmentId(adjustment.getId());
    }
    if (hold != null) {
      accountActivity.setHoldId(hold.getId());
    }

    common.setAccountActivity(accountActivityRepository.save(accountActivity));
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

  public AccountActivity retrieveAccountActivity(
      TypedId<BusinessId> businessId, TypedId<AccountActivityId> accountActivityId) {
    return accountActivityRepository
        .findByBusinessIdAndId(businessId, accountActivityId)
        .orElse(null);
  }

  public Page<AccountActivity> getCardAccountActivity(
      TypedId<BusinessId> businessId,
      TypedId<UserId> userId,
      TypedId<CardId> cardId,
      AccountActivityFilterCriteria accountActivityFilterCriteria) {
    CardDetailsRecord cardDetailsRecord = cardService.getCard(businessId, cardId);
    if (!cardDetailsRecord.card().getUserId().equals(userId)) {
      throw new IdMismatchException(IdType.USER_ID, userId, cardDetailsRecord.card().getUserId());
    }

    accountActivityFilterCriteria.setCardId(cardDetailsRecord.card().getId());
    accountActivityFilterCriteria.setAllocationId(cardDetailsRecord.card().getAllocationId());
    accountActivityFilterCriteria.setUserId(userId);

    return accountActivityRepository.find(businessId, accountActivityFilterCriteria);
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

  public AccountActivity getUserAccountActivity(
      TypedId<BusinessId> businessId, TypedId<AdjustmentId> adjustmentId) {

    return accountActivityRepository
        .findByBusinessIdAndAdjustmentId(businessId, adjustmentId)
        .orElseThrow(
            () -> new RecordNotFoundException(Table.ACCOUNT_ACTIVITY, businessId, adjustmentId));
  }
}
