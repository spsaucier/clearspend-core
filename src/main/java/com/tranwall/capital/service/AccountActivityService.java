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
import com.tranwall.capital.data.model.AccountActivity;
import com.tranwall.capital.data.model.Adjustment;
import com.tranwall.capital.data.model.Allocation;
import com.tranwall.capital.data.model.Hold;
import com.tranwall.capital.data.model.User;
import com.tranwall.capital.data.model.embedded.MerchantDetails;
import com.tranwall.capital.data.model.enums.AccountActivityStatus;
import com.tranwall.capital.data.model.enums.AccountActivityType;
import com.tranwall.capital.data.model.enums.MerchantType;
import com.tranwall.capital.data.repository.AccountActivityRepository;
import com.tranwall.capital.data.repository.CardRepositoryCustom.CardDetailsRecord;
import com.tranwall.capital.service.type.NetworkCommon;
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
            REALLOCATE,
            AccountActivityStatus.PROCESSED,
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
    AccountActivity accountActivity =
        new AccountActivity(
            common.getBusinessId(),
            allocation.getId(),
            allocation.getName(),
            common.getAccount().getId(),
            common.getNetworkMessageType().getAccountActivityType(),
            common.getAccountActivity().getAccountActivityStatus(),
            common.getAccountActivity().getActivityTime(),
            amount);
    accountActivity.setUserId(common.getCard().getUserId());

    accountActivity.setMerchant(
        new MerchantDetails(
            common.getMerchantName(),
            MerchantType.OTHERS,
            common.getMerchantNumber(),
            common.getMerchantCategoryCode(),
            common.getAccountActivity().getMerchantLogoUrl(),
            common.getAccountActivity().getMerchantLatitude(),
            common.getAccountActivity().getMerchantLongitude()));

    User cardOwner = userService.retrieveUser(common.getCard().getUserId());
    accountActivity.setCard(
        new com.tranwall.capital.data.model.embedded.CardDetails(
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

  public AccountActivity retrieveAccountActivity(TypedId<AccountActivityId> accountActivityId) {
    return accountActivityRepository.getById(accountActivityId);
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
}
