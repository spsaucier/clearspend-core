package com.clearspend.capital.service;

import com.clearspend.capital.client.clearbit.ClearbitClient;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.model.Versioned;
import com.clearspend.capital.common.error.LimitViolationException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.typedid.data.HoldId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.data.model.Decline;
import com.clearspend.capital.data.model.Hold;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.HoldStatus;
import com.clearspend.capital.data.model.enums.card.CardStatus;
import com.clearspend.capital.data.model.enums.network.DeclineReason;
import com.clearspend.capital.data.model.enums.network.NetworkMessageType;
import com.clearspend.capital.data.model.network.NetworkMerchant;
import com.clearspend.capital.data.model.network.NetworkMessage;
import com.clearspend.capital.data.repository.network.NetworkMerchantRepository;
import com.clearspend.capital.data.repository.network.NetworkMessageRepository;
import com.clearspend.capital.service.AccountService.AdjustmentRecord;
import com.clearspend.capital.service.AccountService.HoldRecord;
import com.clearspend.capital.service.CardService.CardRecord;
import com.clearspend.capital.service.type.NetworkCommon;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.annotations.VisibleForTesting;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Optional;
import javax.transaction.Transactional;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NetworkMessageService {

  private final NetworkMessageRepository networkMessageRepository;
  private final NetworkMerchantRepository networkMerchantRepository;

  private final AccountService accountService;
  private final AccountActivityService accountActivityService;
  private final AllocationService allocationService;
  private final CardService cardService;
  private final TransactionLimitService transactionLimitService;

  public final ObjectMapper objectMapper =
      new ObjectMapper()
          .registerModule(new JavaTimeModule())
          .registerModule(new Jdk8Module())
          .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
          .disable(DeserializationFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE)
          .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

  private final ClearbitClient clearbitClient;

  @Transactional
  public void processNetworkMessage(NetworkCommon common) {
    retrieveCardAndNetworkMessages(common);

    storeMerchantAsync(common);

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
  }

  private void retrieveCardAndNetworkMessages(NetworkCommon common) {
    // update common with data we have locally
    CardRecord cardRecord;
    try {
      cardRecord = cardService.getCardByExternalRef(common.getCardExternalRef());
    } catch (RecordNotFoundException e) {
      // if the card isn't found, set decline bits and return to caller
      common.getDeclineReasons().add(DeclineReason.CARD_NOT_FOUND);
      common.setPostDecline(true);
      log.error("failed to find card with externalRef: " + common.getCardExternalRef());
      // TODO(kuchlein): sort out how we want to handle errors since we always want to write at
      //   at least StripWebhookLog
      return;
    }

    common.setBusinessId(cardRecord.card().getBusinessId());
    common.setCard(cardRecord.card());

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
              common.getBusinessId(), common.earliestNetworkMessage.getAllocationId()));

      // get the most recently created Hold if any
      Optional<NetworkMessage> optionalNetworkMessage =
          common.getPriorNetworkMessages().stream()
              .filter(networkMessage -> networkMessage.getHoldId() != null)
              .min(Comparator.comparing(Versioned::getCreated));
      if (optionalNetworkMessage.isPresent()) {
        TypedId<HoldId> holdId = optionalNetworkMessage.get().getHoldId();
        common.setPriorHold(
            common.getAccount().getHolds().stream()
                .filter(hold -> hold.getId().equals(holdId))
                .findFirst()
                .orElse(null));
        if (common.getPriorHold() == null) {
          log.warn("prior hold {} not found for account {}", holdId, common.getAccount().getId());
        }
      }
    } else {
      common.setAccount(cardRecord.account());
      common.setAllocation(
          allocationService.retrieveAllocation(
              common.getBusinessId(), cardRecord.card().getAllocationId()));
    }
  }

  private void postHold(NetworkCommon common, NetworkMessage networkMessage) {
    if (common.isPostHold()) {
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

  private void postAdjustment(NetworkCommon common, NetworkMessage networkMessage) {
    if (common.isPostAdjustment()) {
      AdjustmentRecord adjustmentRecord =
          accountService.recordNetworkAdjustment(common.getAccount(), common.getRequestedAmount());
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
          common.getRequestedAmount(),
          networkMessage.getAdjustmentId(),
          common.getAccount().getAvailableBalance(),
          common.getAccount().getLedgerBalance());
    }
  }

  private void postDecline(NetworkCommon common, NetworkMessage networkMessage) {
    if (common.isPostDecline()) {
      Decline decline =
          accountService.recordNetworkDecline(
              common.getAccount(),
              common.getCard(),
              common.getPaddedAmount(),
              common.getDeclineReasons());
      common.setDecline(decline);
      networkMessage.setDeclineId(decline.getId());
      common.getAccountActivityDetails().setAccountActivityStatus(AccountActivityStatus.DECLINED);
      common.getAccountActivityDetails().setActivityTime(OffsetDateTime.now());
      accountActivityService.recordNetworkDeclineAccountActivity(common);
      log.warn(
          "networkMessage {} for {} declined (available {} / ledger {})",
          networkMessage.getId(),
          common.getPaddedAmount(),
          common.getAccount().getAvailableBalance(),
          common.getAccount().getLedgerBalance());
    }
  }

  @VisibleForTesting
  void setPaddedAmountAndHoldPeriod(@NonNull NetworkCommon common) {
    switch (common.getMerchantType()) {
      case AUTOMATED_FUEL_DISPENSERS -> {
        common.setHoldExpiration(OffsetDateTime.now().plusHours(2));
        common.setPaddedAmount(
            Amount.of(common.getRequestedAmount().getCurrency(), BigDecimal.valueOf(-100)));
      }
      case AIRLINES_AIR_CARRIERS,
          CAR_RENTAL_AGENCIES,
          CRUISE_LINES,
          HOTELS_MOTELS_AND_RESORTS,
          DIRECT_MARKETING_TRAVEL,
          DIRECT_MARKETING_OUTBOUND_TELEMARKETING,
          DIRECT_MARKETING_INBOUND_TELEMARKETING,
          DIRECT_MARKETING_SUBSCRIPTION -> {
        common.setHoldExpiration(OffsetDateTime.now().plusWeeks(1));
        common.setPaddedAmount(common.getRequestedAmount().mul(BigDecimal.valueOf(1.15)));
      }
      case DRINKING_PLACES,
          HEALTH_AND_BEAUTY_SPAS,
          EATING_PLACES_RESTAURANTS,
          FAST_FOOD_RESTAURANTS,
          TAXICABS_LIMOUSINES -> {
        common.setHoldExpiration(OffsetDateTime.now().plusDays(3));
        common.setPaddedAmount(common.getRequestedAmount().mul(BigDecimal.valueOf(1.20)));
      }
      default -> {
        common.setHoldExpiration(OffsetDateTime.now().plusDays(5));
        common.setPaddedAmount(common.getRequestedAmount());
      }
    }

    common.getPaddedAmount().ensureNegative();
  }

  private void processAuthorizationUpdated(NetworkCommon common) {
    common.setNetworkMessageGroupId(common.earliestNetworkMessage.getNetworkMessageGroupId());
    // TODO(kuchlein): handle the case when the hold amount changes
  }

  private void processAuthorizationCreated(NetworkCommon common) {
    common.setNetworkMessageGroupId(common.earliestNetworkMessage.getNetworkMessageGroupId());
  }

  private void processAuthorizationRequest(NetworkCommon common) {
    setPaddedAmountAndHoldPeriod(common);

    if (!common.getCard().getStatus().equals(CardStatus.ACTIVE)) {
      common.getDeclineReasons().add(DeclineReason.INVALID_CARD_STATUS);
      common.setPostDecline(true);
      return;
    }

    // account has no money at all
    if (common.getAccount().getAvailableBalance().isLessThanOrEqualToZero()) {
      common.getDeclineReasons().add(DeclineReason.INSUFFICIENT_FUNDS);
      common.setPostDecline(true);
      return;
    }

    // account has insufficient funds and partial approval isn't an option
    if (!common.isAllowPartialApproval()) {
      if (common
          .getAccount()
          .getAvailableBalance()
          .add(common.getPaddedAmount())
          .isLessThanZero()) {
        common.getDeclineReasons().add(DeclineReason.INSUFFICIENT_FUNDS);
        common.setPostDecline(true);
        return;
      }
      common.setApprovedAmount(common.getPaddedAmount());
    } else {
      common.setApprovedAmount(
          Amount.min(common.getAccount().getAvailableBalance(), common.getPaddedAmount().abs()));
    }
    common.setHoldAmount(common.getApprovedAmount());

    if (common.isIncrementalAuthorization()) {
      if (common.getPriorHold() != null) {
        common.getPriorHold().setStatus(HoldStatus.RELEASED);
        common.getUpdatedHolds().add(common.getPriorHold());
      }
      log.info(
          "IncrementalHoldAmount: {} + {}",
          common.getApprovedAmount(),
          common.getPriorHoldAmount().negate());
      common.setHoldAmount(common.getApprovedAmount().add(common.getPriorHoldAmount().negate()));
    }

    // Card spending limits and settings checks
    try {
      transactionLimitService.ensureWithinLimit(
          common.getBusinessId(),
          common.getAllocation().getId(),
          common.getCard().getId(),
          common.getApprovedAmount());
    } catch (LimitViolationException e) {
      log.warn("Failed to accept a transaction due to a limit violation: {}", e.getMessage());
      common.getDeclineReasons().add(DeclineReason.LIMIT_EXCEEDED);
      common.setPostDecline(true);
      return;
    }

    common.getAccountActivityDetails().setAccountActivityStatus(AccountActivityStatus.PENDING);
    common.setPostHold(true);
  }

  private void processTransactionCreated(NetworkCommon common) {
    common.setApprovedAmount(common.getRequestedAmount());

    common.getAccountActivityDetails().setAccountActivityStatus(AccountActivityStatus.APPROVED);
    common.setPostAdjustment(true);

    Hold hold = common.getPriorHold();
    if (hold != null) {
      hold.setStatus(HoldStatus.RELEASED);
      common.getUpdatedHolds().add(hold);
      common.getAccount().recalculateAvailableBalance();
      accountActivityService.recordNetworkHoldReleaseAccountActivity(hold);
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

  private void storeMerchantAsync(NetworkCommon common) {
    Optional<NetworkMerchant> networkMerchantOptional =
        networkMerchantRepository.findByMerchantNameAndMerchantCategoryCode(
            common.getMerchantName(), common.getMerchantCategoryCode());
    if (networkMerchantOptional.isPresent()) {
      common
          .getAccountActivityDetails()
          .setMerchantLogoUrl(networkMerchantOptional.get().getMerchantLogoUrl());

      return;
    }
    // asynchronously store the merchant details so that they can be used to define limits
    new Thread(
            () -> {
              try {
                NetworkMerchant networkMerchant =
                    new NetworkMerchant(common.getMerchantName(), common.getMerchantCategoryCode());
                networkMerchant.setMerchantLogoUrl(
                    clearbitClient.getLogo(common.getMerchantName()));
                networkMerchant = networkMerchantRepository.save(networkMerchant);
                // TODO if the accountActivity is written before the following assignment due to a
                //   race condition add a batch job to update it
                common
                    .getAccountActivityDetails()
                    .setMerchantLogoUrl(networkMerchant.getMerchantLogoUrl());
              } catch (org.springframework.dao.DataIntegrityViolationException
                  | org.hibernate.exception.ConstraintViolationException e) {
                log.warn(
                    "NetworkMerchant already present: {} {}",
                    common.getMerchantName(),
                    common.getMerchantCategoryCode());
              } catch (Exception e) {
                log.error(
                    "Unexpected failure to create NetworkMerchant record ({} {})",
                    common.getMerchantName(),
                    common.getMerchantCategoryCode(),
                    e);
              }
            })
        .start();
  }
}
