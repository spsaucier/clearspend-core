package com.clearspend.capital.service;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.model.Versioned;
import com.clearspend.capital.common.error.LimitViolationException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.SpendControlViolationException;
import com.clearspend.capital.common.typedid.data.HoldId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.decline.AddressPostalCodeMismatch;
import com.clearspend.capital.data.model.decline.Decline;
import com.clearspend.capital.data.model.decline.DeclineDetails;
import com.clearspend.capital.data.model.decline.LimitExceeded;
import com.clearspend.capital.data.model.decline.SpendControlViolated;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.clearspend.capital.data.model.enums.HoldStatus;
import com.clearspend.capital.data.model.enums.MerchantType;
import com.clearspend.capital.data.model.enums.card.CardStatus;
import com.clearspend.capital.data.model.enums.network.DeclineReason;
import com.clearspend.capital.data.model.enums.network.NetworkMessageType;
import com.clearspend.capital.data.model.enums.network.VerificationResultType;
import com.clearspend.capital.data.model.network.NetworkMessage;
import com.clearspend.capital.data.repository.network.NetworkMerchantRepository;
import com.clearspend.capital.data.repository.network.NetworkMessageRepository;
import com.clearspend.capital.service.AccountService.AdjustmentRecord;
import com.clearspend.capital.service.AccountService.HoldRecord;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.CardService.CardRecord;
import com.clearspend.capital.service.type.NetworkCommon;
import com.clearspend.capital.service.type.PushNotificationEvent;
import com.google.common.annotations.VisibleForTesting;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import javax.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NetworkMessageService {
  private final NetworkMessageRepository networkMessageRepository;
  private final NetworkMerchantRepository networkMerchantRepository;

  private final ApplicationEventPublisher applicationEventPublisher;

  private final AccountService accountService;
  private final BusinessService businessService;
  private final BusinessSettingsService businessSettingsService;
  private final AccountActivityService accountActivityService;
  private final AllocationService allocationService;
  private final CardService cardService;
  private final TransactionLimitService transactionLimitService;
  private final UserService userService;

  private void retrieveCardAndNetworkMessages(NetworkCommon common) {
    // update common with data we have locally
    CardRecord cardRecord;
    try {
      cardRecord = cardService.getCardByExternalRef(common.getCardExternalRef());
    } catch (RecordNotFoundException e) {
      common.getDeclineDetails().add(new DeclineDetails(DeclineReason.CARD_NOT_FOUND));
      common.setPostDecline(true);
      throw e;
    }

    common.setBusiness(
        businessService.retrieveBusinessForService(cardRecord.card().getBusinessId(), true));
    common.setBusinessSettings(
        businessSettingsService.retrieveBusinessSettingsForService(
            cardRecord.card().getBusinessId()));
    common.setCard(cardRecord.card());
    common.setUser(userService.retrieveUserForService(cardRecord.card().getUserId()));

    // get any prior network messages as we'll need to pull the allocationId and accountId from them
    // as the one of the card record may have been updated
    common.earliestNetworkMessage =
        getPriorAuthNetworkMessage(common.getStripeAuthorizationExternalRef());
    if (common.earliestNetworkMessage != null) {
      common.setNetworkMessageGroupId(common.earliestNetworkMessage.getNetworkMessageGroupId());
      common.setPriorNetworkMessages(
          networkMessageRepository.findByNetworkMessageGroupId(common.getNetworkMessageGroupId()));

      common.setAccount(
          accountService.retrieveAccountById(common.earliestNetworkMessage.getAccountId(), true));
      common.setAllocation(
          allocationService.retrieveAllocation(
              common.getBusiness().getId(), common.earliestNetworkMessage.getAllocationId()));

      // get the most recently created Hold if any
      List<TypedId<HoldId>> holdIds =
          common.getPriorNetworkMessages().stream()
              .map(NetworkMessage::getHoldId)
              .filter(Objects::nonNull)
              .toList();
      if (!holdIds.isEmpty()) {
        common.setPriorHold(
            common.getAccount().getHolds().stream()
                .filter(
                    hold ->
                        holdIds.contains(hold.getId())
                            && hold.getStatus().equals(HoldStatus.PLACED))
                .max(Comparator.comparing(Versioned::getCreated))
                .orElse(null));
        if (common.getPriorHold() == null) {
          log.warn("prior hold not found for account {}", common.getAccount().getId());
        }
      }
    } else {
      common.setAccount(cardRecord.account());
      Optional.ofNullable(cardRecord.card().getAllocationId())
          .ifPresent(
              allocationId ->
                  common.setAllocation(
                      allocationService.retrieveAllocation(
                          common.getBusiness().getId(), allocationId)));
    }

    if (NetworkCommonUtils.shouldUseRootAllocation(common)) {
      final AllocationRecord rootAllocation =
          allocationService.getRootAllocation(common.getBusiness().getId());
      common.setAllocation(rootAllocation.allocation());
      common.setAccount(rootAllocation.account());
    }
  }

  private void postHold(NetworkCommon common, NetworkMessage networkMessage) {
    if (common.isPostHold()) {
      log.debug("holdExpiration: {}", common.getHoldExpiration());
      HoldRecord holdRecord =
          accountService.recordNetworkHold(
              common.getAccount(), common.getHoldAmount(), common.getHoldExpiration());
      networkMessage.setHoldId(holdRecord.hold().getId());
      common.setHold(holdRecord.hold());
      common.getAccountActivityDetails().setActivityTime(holdRecord.hold().getCreated());
      common.getAccountActivityDetails().setHideAfter(holdRecord.hold().getExpirationDate());
      accountActivityService.recordNetworkHoldAccountActivity(common, holdRecord.hold());
      log.debug(
          "networkMessage {} for {} hold {} (available {} / ledger {})",
          networkMessage.getId(),
          common.getHoldAmount(),
          networkMessage.getHoldId(),
          common.getAccount().getAvailableBalance(),
          common.getAccount().getLedgerBalance());
    }
  }

  @Transactional
  @PreAuthorize("hasGlobalPermission('APPLICATION')")
  public void processNetworkMessage(NetworkCommon common) {
    retrieveCardAndNetworkMessages(common);

    //    storeMerchantAsync(common);

    // TODO(kuchlein): lookup local merchantName table to retrieve logo (needs to be done async and
    //    potentially in a async batch job)
    // common.getAccountActivity().setMerchantLogoUrl();

    // TODO(kuchlein): lookup local merchantAddress table to retrieve lat/long (needs to be done
    //    async and potentially in a async batch job)
    // common.getAccountActivity().setMerchantLatitude();
    // common.getAccountActivity().setMerchantLongitude();

    // actually process the network message from Stripe
    switch (common.getNetworkMessageType()) {
      case AUTH_REQUEST -> processAuthorizationRequest(common);
      case AUTH_CREATED -> processAuthorizationCreated(common);
      case AUTH_UPDATED -> processAuthorizationUpdated(common);
      case TRANSACTION_CREATED -> processTransactionCreated(common);
      default -> throw new IllegalArgumentException(
          "invalid networkMessageType " + common.getNetworkMessageType());
    }

    if (common.isPostAdjustment() || common.isPostDecline() || common.isPostHold()) {
      // store any data that resulted from processing the network message from Stripe
      NetworkMessage networkMessage = common.toNetworkMessage();

      // card may be null in the case of Stripe sending us transactions for cards that we've not
      // issued
      // TODO(kuchlein): determine if Stripe handle this for us or not
      if (common.getCard() != null) {
        networkMessage.setCardId(common.getCard().getId());
      }

      postHold(common, networkMessage);
      postAdjustment(common, networkMessage);
      postDecline(common, networkMessage);

      common.setNetworkMessage(networkMessageRepository.save(networkMessage));
      // notifications to firebase
      applicationEventPublisher.publishEvent(new PushNotificationEvent(common));
    }
  }

  private void postAdjustment(NetworkCommon common, NetworkMessage networkMessage) {
    if (common.isPostAdjustment()) {
      AdjustmentRecord adjustmentRecord =
          accountService.recordNetworkAdjustment(
              common.getAllocation().getAllocationId(),
              common.getAccount(),
              common.getApprovedAmount());
      common.setAdjustmentRecord(adjustmentRecord);
      networkMessage.setAdjustmentId(adjustmentRecord.adjustment().getId());
      common
          .getAccountActivityDetails()
          .setActivityTime(adjustmentRecord.adjustment().getCreated());
      accountActivityService.recordNetworkAdjustmentAccountActivity(
          common, adjustmentRecord.adjustment());
      log.debug(
          "networkMessage {} for {} adjustment {} (available {} / ledger {})",
          networkMessage.getId(),
          common.getApprovedAmount(),
          networkMessage.getAdjustmentId(),
          common.getAccount().getAvailableBalance(),
          common.getAccount().getLedgerBalance());
    }
  }

  private void postDecline(NetworkCommon common, NetworkMessage networkMessage) {
    if (common.isPostDecline()) {
      Decline decline =
          accountService.recordNetworkDecline(
              common.getBusiness().getId(),
              common.getCard().getId(),
              Optional.ofNullable(common.getAccount()).map(Account::getId).orElse(null),
              common.getPaddedAmount(),
              common.getDeclineDetails());
      common.setDecline(decline);
      networkMessage.setDeclineId(decline.getId());
      common.getAccountActivityDetails().setAccountActivityStatus(AccountActivityStatus.DECLINED);
      common.getAccountActivityDetails().setActivityTime(decline.getCreated());
      accountActivityService.recordNetworkDeclineAccountActivity(common);
      final AccountAmounts accountAmounts =
          Optional.ofNullable(common.getAccount())
              .map(
                  account ->
                      new AccountAmounts(account.getAvailableBalance(), account.getLedgerBalance()))
              .orElse(new AccountAmounts(null, null));
      log.warn(
          "networkMessage {} for {} declined (available {} / ledger {})",
          networkMessage.getId(),
          common.getPaddedAmount(),
          accountAmounts.availableBalance(),
          accountAmounts.ledgerBalance());
    }
  }

  private record AccountAmounts(Amount availableBalance, Amount ledgerBalance) {}

  @VisibleForTesting
  void setPaddedAmountAndHoldPeriod(@NonNull NetworkCommon common) {
    switch (common.getMerchantType()) {
      case AUTOMATED_FUEL_DISPENSERS -> {
        common.setHoldExpiration(OffsetDateTime.now(ZoneOffset.UTC).plusHours(2));
        common.setPaddedAmount(
            Amount.of(common.getRequestedAmount().getCurrency(), BigDecimal.valueOf(-100)));
        common.setAllowPartialApproval(false);
      }
      case AIRLINES_AIR_CARRIERS,
          CAR_RENTAL_AGENCIES,
          CRUISE_LINES,
          HOTELS_MOTELS_AND_RESORTS,
          DIRECT_MARKETING_TRAVEL,
          DIRECT_MARKETING_OUTBOUND_TELEMARKETING,
          DIRECT_MARKETING_INBOUND_TELEMARKETING,
          DIRECT_MARKETING_SUBSCRIPTION -> {
        common.setHoldExpiration(OffsetDateTime.now(ZoneOffset.UTC).plusWeeks(1));
        common.setPaddedAmount(common.getRequestedAmount().mul(BigDecimal.valueOf(1.15)));
      }
      case DRINKING_PLACES,
          HEALTH_AND_BEAUTY_SPAS,
          EATING_PLACES_RESTAURANTS,
          FAST_FOOD_RESTAURANTS,
          TAXICABS_LIMOUSINES -> {
        common.setHoldExpiration(OffsetDateTime.now(ZoneOffset.UTC).plusDays(3));
        common.setPaddedAmount(common.getRequestedAmount().mul(BigDecimal.valueOf(1.20)));
      }
      default -> {
        common.setHoldExpiration(OffsetDateTime.now(ZoneOffset.UTC).plusDays(5));
        common.setPaddedAmount(common.getRequestedAmount());
      }
    }

    if (common.getForeignTransaction()) {
      Amount foreignTransactionFee =
          common
              .getRequestedAmount()
              .percents(common.getBusinessSettings().getForeignTransactionFeePercents());
      common.setForeignTransactionFee(foreignTransactionFee.abs());
      common.setPaddedAmount(common.getPaddedAmount().add(foreignTransactionFee));
    }

    common.getPaddedAmount().ensureNegative();
  }

  private void processAuthorizationUpdated(NetworkCommon common) {
    if (common.earliestNetworkMessage != null) {
      common.setNetworkMessageGroupId(common.earliestNetworkMessage.getNetworkMessageGroupId());
      // TODO(kuchlein): handle the case when the hold amount changes
      if (common.getPriorHold() != null
          && common.getPriorHold().getAmount().isNotEqual(common.getRequestedAmount())) {
        releasePriorHold(common);
        common.setApprovedAmount(common.getRequestedAmount());
        common.setHoldAmount(common.getApprovedAmount());
        common.setHoldExpiration(
            common.getMerchantType() == MerchantType.AUTOMATED_FUEL_DISPENSERS
                ? OffsetDateTime.now(ZoneOffset.UTC).plusDays(3)
                : common.getPriorHold().getExpirationDate());
        common.setPostHold(common.getRequestedAmount().isLessThanZero());
        common.getAccountActivityDetails().setAccountActivityStatus(AccountActivityStatus.PENDING);
      }
    }
  }

  private void processAuthorizationCreated(NetworkCommon common) {
    if (common.earliestNetworkMessage != null) {
      common.setNetworkMessageGroupId(common.earliestNetworkMessage.getNetworkMessageGroupId());
    }
  }

  private void processTransactionCreated(NetworkCommon common) {
    common.setApprovedAmount(common.getRequestedAmount());

    if (common.getForeignTransaction()) {
      Amount foreignTransactionFee =
          common
              .getApprovedAmount()
              .percents(common.getBusinessSettings().getForeignTransactionFeePercents());
      common.setApprovedAmount(common.getApprovedAmount().add(foreignTransactionFee));
      common.setForeignTransactionFee(foreignTransactionFee.abs());
    }

    AccountActivityType accountActivityType = common.getAccountActivityType();
    common
        .getAccountActivityDetails()
        .setAccountActivityStatus(
            accountActivityType == AccountActivityType.NETWORK_REFUND
                ? AccountActivityStatus.PROCESSED
                : AccountActivityStatus.APPROVED);

    common.setPostAdjustment(true);

    releasePriorHold(common);
  }

  private void processAuthorizationRequest(NetworkCommon common) {
    setPaddedAmountAndHoldPeriod(common);

    if (common.getBusiness().getStatus() == BusinessStatus.SUSPENDED) {
      common.getDeclineDetails().add(new DeclineDetails(DeclineReason.BUSINESS_SUSPENSION));
      common.setPostDecline(true);
    }

    // decline if we have any mismatches on address, CVN or card expiration date
    if (common.getAddressPostalCodeCheck() == VerificationResultType.MISMATCH) {
      common.getDeclineDetails().add(new AddressPostalCodeMismatch(common.getAddressPostalCode()));
      common.setPostDecline(true);
    }
    if (common.getCvcCheck() == VerificationResultType.MISMATCH) {
      common.getDeclineDetails().add(new DeclineDetails(DeclineReason.CVC_MISMATCH));
      common.setPostDecline(true);
    }
    if (common.getExpiryCheck() == VerificationResultType.MISMATCH) {
      common.getDeclineDetails().add(new DeclineDetails(DeclineReason.EXPIRY_MISMATCH));
      common.setPostDecline(true);
    }

    // card must be active
    if (!common.getCard().getStatus().equals(CardStatus.ACTIVE)) {
      common.getDeclineDetails().add(new DeclineDetails(DeclineReason.INVALID_CARD_STATUS));
      common.setPostDecline(true);
    }

    if (common.getAccount() == null || common.getAllocation() == null) {
      common.getDeclineDetails().add(new DeclineDetails(DeclineReason.UNLINKED_CARD));
      common.setPostDecline(true);
    }

    // account has no money at all
    if (common.getAccount() != null
        && common.getAccount().getAvailableBalance().isLessThanOrEqualToZero()) {
      common.getDeclineDetails().add(new DeclineDetails(DeclineReason.INSUFFICIENT_FUNDS));
      common.setPostDecline(true);
    }

    if (!common.getDeclineDetails().isEmpty()) {
      return;
    }

    // account has insufficient funds and partial approval isn't an option
    if (!common.isAllowPartialApproval()) {
      if (common
          .getAccount()
          .getAvailableBalance()
          .add(common.getPaddedAmount())
          .isLessThanZero()) {
        common.getDeclineDetails().add(new DeclineDetails(DeclineReason.INSUFFICIENT_FUNDS));
        common.setPostDecline(true);
        return;
      }
      common.setApprovedAmount(common.getPaddedAmount());
    } else {
      common.setApprovedAmount(
          Amount.min(common.getAccount().getAvailableBalance(), common.getPaddedAmount().abs())
              .negate());
    }
    common.setHoldAmount(common.getApprovedAmount());

    if (common.isIncrementalAuthorization()) {
      releasePriorHold(common);
      log.info(
          "IncrementalHoldAmount: {} + {}",
          common.getApprovedAmount(),
          common.getPriorHoldAmount().negate());
      common.setHoldAmount(common.getApprovedAmount().add(common.getPriorHoldAmount().negate()));
    }

    // We will have already declined the authorization if this is null
    if (common.getAllocation() != null) {
      // Card spending limits and settings checks
      try {
        transactionLimitService.ensureWithinLimit(
            common.getBusiness().getId(),
            common.getAllocation().getId(),
            common.getCard().getId(),
            common.getApprovedAmount(),
            common.getMerchantCategoryCode(),
            common.getAuthorizationMethod(),
            common.getForeignTransaction());
      } catch (LimitViolationException | SpendControlViolationException e) {
        log.warn("Failed to accept a transaction due to a decline: {}", e.getMessage());
        if (e instanceof LimitViolationException limitViolationException) {
          common.getDeclineDetails().add(LimitExceeded.from(limitViolationException));
        } else {
          common
              .getDeclineDetails()
              .add(SpendControlViolated.from((SpendControlViolationException) e));
        }
        common.setPostDecline(true);
        return;
      }
    }

    common.getAccountActivityDetails().setAccountActivityStatus(AccountActivityStatus.PENDING);
    common.setPostHold(true);
  }

  private void releasePriorHold(NetworkCommon common) {
    if (common.getPriorHold() != null
        && common.getPriorHold().getStatus().equals(HoldStatus.PLACED)) {
      common.getPriorHold().setStatus(HoldStatus.RELEASED);
      common.getUpdatedHolds().add(common.getPriorHold());
      common.getAccount().recalculateAvailableBalance();
      common.setPriorAccountActivity(
          accountActivityService.recordHoldReleaseAccountActivity(common.getPriorHold()));
    }
  }

  private NetworkMessage getPriorAuthNetworkMessage(String externalRef) {
    // this should always return a network message if we received and processed the initial
    // issuing_authorization.request
    return networkMessageRepository
        .findByExternalRefAndTypeOrderByCreatedDesc(externalRef, NetworkMessageType.AUTH_REQUEST)
        .stream()
        .findFirst()
        .orElse(null);
  }
}
