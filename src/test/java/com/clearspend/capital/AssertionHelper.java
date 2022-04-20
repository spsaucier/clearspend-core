package com.clearspend.capital;

import static org.assertj.core.api.Assertions.assertThat;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.model.Versioned;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.ledger.LedgerAccountId;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.Adjustment;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.Hold;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.decline.Decline;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.model.enums.AdjustmentType;
import com.clearspend.capital.data.model.enums.HoldStatus;
import com.clearspend.capital.data.model.enums.LedgerAccountType;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.ledger.JournalEntry;
import com.clearspend.capital.data.model.ledger.LedgerAccount;
import com.clearspend.capital.data.model.ledger.Posting;
import com.clearspend.capital.data.model.network.NetworkMessage;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.data.repository.AdjustmentRepository;
import com.clearspend.capital.data.repository.DeclineRepository;
import com.clearspend.capital.data.repository.HoldRepository;
import com.clearspend.capital.data.repository.ledger.JournalEntryRepository;
import com.clearspend.capital.data.repository.ledger.LedgerAccountRepository;
import com.clearspend.capital.data.repository.ledger.PostingRepository;
import com.clearspend.capital.data.repository.network.NetworkMessageRepository;
import com.clearspend.capital.service.AccountService;
import com.clearspend.capital.service.LedgerService;
import com.clearspend.capital.service.ServiceHelper;
import com.clearspend.capital.service.type.NetworkCommon;
import com.stripe.model.issuing.Authorization;
import com.stripe.model.issuing.Transaction;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AssertionHelper {

  @Autowired private AccountActivityRepository accountActivityRepository;
  @Autowired private AdjustmentRepository adjustmentRepository;
  @Autowired private DeclineRepository declineRepository;
  @Autowired private HoldRepository holdRepository;
  @Autowired private JournalEntryRepository journalEntryRepository;
  @Autowired private LedgerAccountRepository ledgerAccountRepository;
  @Autowired private NetworkMessageRepository networkMessageRepository;
  @Autowired private PostingRepository postingRepository;
  @Autowired private ServiceHelper serviceHelper;

  @Autowired private AccountService accountService;
  @Autowired private LedgerService ledgerService;

  public record AuthorizationRecord(NetworkCommon networkCommon, Authorization authorization) {}

  public void assertBalance(
      Business business,
      Allocation allocation,
      Account account,
      BigDecimal ledgerBalance,
      BigDecimal availableBalance) {
    Account foundAccount =
        serviceHelper
            .accountService()
            .retrieveAllocationAccount(
                business.getId(), business.getCurrency(), allocation.getId());
    assertThat(foundAccount.getLedgerBalance().getAmount()).isEqualByComparingTo(ledgerBalance);
    assertThat(foundAccount.getAvailableBalance().getAmount())
        .isEqualByComparingTo(availableBalance);
    assertThat(foundAccount.getLedgerBalance()).isEqualTo(account.getLedgerBalance());
    assertThat(foundAccount.getAvailableBalance()).isEqualTo(account.getAvailableBalance());
  }

  public void assertAdjustment(
      Adjustment adjustment, AdjustmentType type, Account account, Amount amount) {
    assertThat(adjustment.getBusinessId()).isEqualTo(account.getBusinessId());
    assertThat(adjustment.getAccountId()).isEqualTo(account.getId());
    assertThat(adjustment.getType()).isEqualTo(type);
    assertThat(adjustment.getAmount()).isEqualTo(amount);
  }

  public record PostingAssertion(
      TypedId<LedgerAccountId> ledgerAccountId, LedgerAccountType type, Amount amount) {}

  public void assertJournalEntry(
      JournalEntry journalEntry, List<PostingAssertion> postingAssertions) {

    JournalEntry foundJournalEntry =
        journalEntryRepository.findById(journalEntry.getId()).orElseThrow();
    assertThat(foundJournalEntry).isEqualTo(journalEntry);

    assertThat(journalEntry.getReversalJournalEntryId()).isNull();
    assertThat(journalEntry.getReversedJournalEntryId()).isNull();
    assertThat(journalEntry.getPostings()).hasSize(2);

    if (postingAssertions != null) {
      assertThat(journalEntry.getPostings()).hasSize(postingAssertions.size());
      for (PostingAssertion postingAssertion : postingAssertions) {
        Posting posting =
            journalEntry.getPostings().stream()
                .filter(e -> e.getLedgerAccountId().equals(postingAssertion.ledgerAccountId))
                .findFirst()
                .orElseThrow();
        assertThat(posting.getAmount()).isEqualTo(postingAssertion.amount);

        posting = postingRepository.findById(posting.getId()).orElseThrow();
        assertThat(posting.getAmount()).isEqualTo(postingAssertion.amount);

        LedgerAccount ledgerAccount =
            ledgerAccountRepository.findById(posting.getLedgerAccountId()).orElseThrow();
        assertThat(ledgerAccount.getType()).isEqualTo(postingAssertion.type);
        assertThat(ledgerAccount.getCurrency()).isEqualTo(postingAssertion.amount.getCurrency());
      }
    }
  }

  public void assertAdjustmentAccountActivity(
      AccountActivityType type,
      Adjustment adjustment,
      User user,
      Allocation allocation,
      Account account,
      @NonNull Amount amount,
      @NonNull Amount requestedAmount) {
    AccountActivity accountActivity =
        accountActivityRepository.findByAdjustmentId(adjustment.getId()).stream()
            .min(Comparator.comparing(Versioned::getCreated))
            .orElseThrow();

    assertAccountActivity(
        type,
        AccountActivityStatus.PROCESSED,
        accountActivity,
        user,
        allocation,
        account,
        amount,
        requestedAmount);
  }

  public void assertNetworkMessageRequestAccountActivity(
      @NonNull NetworkCommon common, Authorization authorization, User user) {
    AccountActivity accountActivity =
        accountActivityRepository.findById(common.getAccountActivity().getId()).orElseThrow();

    AccountActivityStatus status = null;
    Amount amount = common.getApprovedAmount();
    if (common.isPostDecline()) {
      assertDeclineAccountActivity(accountActivity, common.getDecline());
      status = AccountActivityStatus.DECLINED;
      amount = common.getPaddedAmount();
    }
    if (common.isPostAdjustment()) {
      assertAdjustmentAccountActivity(accountActivity, common.getAdjustmentRecord().adjustment());
      status = AccountActivityStatus.PROCESSED;
    }
    if (common.isPostHold()) {
      assertHoldAccountActivity(accountActivity, common.getHold());
      status = AccountActivityStatus.PENDING;
    }
    assertThat(status).isNotNull();

    assertAccountActivity(
        AccountActivityType.NETWORK_AUTHORIZATION,
        status,
        accountActivity,
        user,
        common.getAllocation(),
        common.getAccount(),
        amount,
        common.getRequestedAmount());
    assertCardAccountActivity(accountActivity, common.getUser(), common.getCard());
    if (authorization != null) {
      assertMerchantAccountActivity(
          accountActivity,
          common,
          accountActivity.getMerchant().getMerchantNumber(),
          accountActivity.getMerchant().getName());

      NetworkMessage networkMessage =
          networkMessageRepository.findById(common.getNetworkMessage().getId()).orElseThrow();
      assertThat(networkMessage.getExternalRef()).isEqualTo(authorization.getId());
    }
  }

  public void assertNetworkMessageCreatedAccountActivity(
      @NonNull NetworkCommon common, Authorization authorization, User user) {
    assertThat(common.isPostAdjustment()).isFalse();
    assertThat(common.isPostDecline()).isFalse();
    assertThat(common.isPostHold()).isFalse();
  }

  public void assertNetworkMessageCaptureAccountActivity(
      @NonNull NetworkCommon common,
      AuthorizationRecord authorize,
      @NonNull Transaction transaction,
      @NonNull User user,
      @NonNull Card card) {
    boolean priorAuthorization = authorize != null;

    AccountActivity accountActivity =
        accountActivityRepository.findById(common.getAccountActivity().getId()).orElseThrow();

    AccountActivityType accountActivityType = common.getAccountActivityType();

    assertAccountActivity(
        accountActivityType,
        accountActivityType == AccountActivityType.NETWORK_REFUND
            ? AccountActivityStatus.PROCESSED
            : AccountActivityStatus.APPROVED,
        accountActivity,
        user,
        common.getAllocation(),
        common.getAccount(),
        common.getApprovedAmount(),
        common.getRequestedAmount());
    assertThat(accountActivity.getHold()).isNull();

    assertThat(accountActivity.getHideAfter()).isNull();
    assertAdjustmentAccountActivity(accountActivity, common.getAdjustmentRecord().adjustment());

    assertMerchantAccountActivity(
        accountActivity,
        common,
        transaction.getMerchantData().getNetworkId(),
        transaction.getMerchantData().getName());
    assertCardAccountActivity(accountActivity, user, card);

    NetworkMessage networkMessage =
        networkMessageRepository.findById(common.getNetworkMessage().getId()).orElseThrow();
    assertThat(networkMessage.getExternalRef()).isEqualTo(transaction.getId());
    assertThat(
            networkMessageRepository.countByNetworkMessageGroupId(
                networkMessage.getNetworkMessageGroupId()))
        .isEqualTo(priorAuthorization ? 2 : 1);

    assertAdjustment(
        common.getAdjustmentRecord().adjustment(),
        AdjustmentType.NETWORK,
        common.getAccount(),
        accountActivity.getAmount());

    assertJournalEntry(
        common.getAdjustmentRecord().journalEntry(),
        List.of(
            new PostingAssertion(
                common.getAccount().getLedgerAccountId(),
                LedgerAccountType.ALLOCATION,
                accountActivity.getAmount()),
            new PostingAssertion(
                serviceHelper
                    .ledgerService()
                    .getOrCreateLedgerAccount(
                        LedgerAccountType.NETWORK, accountActivity.getAmount().getCurrency())
                    .getId(),
                LedgerAccountType.NETWORK,
                accountActivity.getAmount().negate())));

    if (priorAuthorization) {
      AccountActivity priorAccountActivity =
          accountActivityRepository
              .findById(authorize.networkCommon.getAccountActivity().getId())
              .orElseThrow();

      Hold priorHold =
          holdRepository.findById(authorize.networkCommon.getHold().getId()).orElseThrow();
      log.debug("x: {}", authorize.networkCommon.getAccountActivity());
      log.debug("priorAccountActivity: {}", priorAccountActivity);
      assertThat(priorAccountActivity.getHideAfter())
          .isEqualTo(authorize.networkCommon.getAccountActivity().getHideAfter());
      assertThat(priorAccountActivity.getHideAfter()).isNotNull();
      log.debug("priorHold: {}", priorHold);
      assertThat(priorHold.getStatus()).isEqualTo(HoldStatus.RELEASED);
    }

    common
        .getUpdatedHolds()
        .forEach(
            hold -> {
              AccountActivity holdAccountActivity =
                  accountActivityRepository.findByHoldId(hold.getId()).stream()
                      .min(Comparator.comparing(Versioned::getCreated))
                      .orElseThrow();
              log.debug("hold accountActivity: {}", holdAccountActivity);
            });
  }

  private void assertAccountActivity(
      @NonNull AccountActivityType type,
      @NonNull AccountActivityStatus status,
      @NonNull AccountActivity accountActivity,
      @NonNull User user,
      @NonNull Allocation allocation,
      @NonNull Account account,
      @NonNull Amount amount,
      @NonNull Amount requestedAmount) {

    assertThat(accountActivity.getBusinessId()).isEqualTo(account.getBusinessId());
    assertThat(accountActivity.getAllocationId()).isEqualTo(account.getAllocationId());
    assertThat(accountActivity.getAllocation().getName()).isEqualTo(allocation.getName());
    if (type == AccountActivityType.NETWORK_AUTHORIZATION
        || type == AccountActivityType.NETWORK_CAPTURE) {
      assertThat(accountActivity.getUserId()).isEqualTo(user.getId());
    } else {
      assertThat(accountActivity.getUserId()).isNull();
    }
    assertThat(accountActivity.getAccountId()).isEqualTo(account.getId());
    assertThat(accountActivity.getType()).isEqualTo(type);
    assertThat(accountActivity.getStatus()).isEqualTo(status);
    assertThat(accountActivity.getVisibleAfter()).isNull();

    assertThat(accountActivity.getReceipt()).isNull();
    assertThat(accountActivity.getAmount()).isEqualTo(amount);
    assertThat(accountActivity.getRequestedAmount()).isEqualTo(requestedAmount);
  }

  private void assertAdjustmentAccountActivity(
      @NonNull AccountActivity accountActivity, Adjustment adjustment) {
    if (adjustment != null) {
      assertThat(accountActivity.getActivityTime()).isEqualTo(adjustment.getCreated());
      assertThat(accountActivity.getAdjustmentId()).isEqualTo(adjustment.getId());
      assertThat(adjustment)
          .isEqualTo(adjustmentRepository.findById(adjustment.getId()).orElseThrow());
    } else {
      assertThat(accountActivity.getAdjustmentId()).isNull();
    }
  }

  private void assertCardAccountActivity(
      @NonNull AccountActivity accountActivity, @NonNull User user, Card card) {
    if (card != null) {
      assertThat(accountActivity.getCard()).isNotNull();
      assertThat(accountActivity.getCard().getCardId()).isEqualTo(card.getId());
      assertThat(accountActivity.getCard().getOwnerFirstName()).isEqualTo(user.getFirstName());
      assertThat(accountActivity.getCard().getOwnerLastName()).isEqualTo(user.getLastName());
    } else {
      assertThat(accountActivity.getCard()).isNull();
    }
  }

  private void assertDeclineAccountActivity(
      @NonNull AccountActivity accountActivity, Decline decline) {
    if (decline != null) {
      assertThat(accountActivity.getActivityTime()).isEqualTo(decline.getCreated());
      assertThat(accountActivity.getAdjustmentId()).isNull();
      assertThat(accountActivity.getHold()).isNull();
      assertThat(accountActivity.getAccountId()).isEqualTo(decline.getAccountId());
      assertThat(accountActivity.getAmount()).isEqualTo(decline.getAmount());
      assertThat(accountActivity.getHideAfter()).isNull();
      assertThat(decline).isEqualTo(declineRepository.findById(decline.getId()).orElseThrow());
      assertThat(decline.getDetails()).isEqualTo(accountActivity.getDeclineDetails());
    } else {
      assertThat(accountActivity.getHold()).isNull();
      assertThat(accountActivity.getHideAfter()).isNotNull();
    }
  }

  private void assertHoldAccountActivity(@NonNull AccountActivity accountActivity, Hold hold) {
    if (hold != null) {
      assertThat(accountActivity.getAdjustmentId()).isNull();
      assertThat(accountActivity.getActivityTime()).isEqualTo(hold.getCreated());
      assertThat(accountActivity.getAccountId()).isEqualTo(hold.getAccountId());
      assertThat(accountActivity.getAmount()).isEqualTo(hold.getAmount());
      assertThat(accountActivity.getHideAfter()).isEqualTo(hold.getExpirationDate());
      assertThat(hold).isEqualTo(holdRepository.findById(hold.getId()).orElseThrow());
      assertThat(hold.getStatus()).isEqualTo(HoldStatus.PLACED);
    } else {
      assertThat(accountActivity.getHold()).isNull();
      assertThat(accountActivity.getHideAfter()).isNotNull();
    }
  }

  private void assertMerchantAccountActivity(
      @NonNull AccountActivity accountActivity,
      @NonNull NetworkCommon networkCommon,
      String merchantNumber,
      String merchantName) {
    assertThat(accountActivity.getMerchant()).isNotNull();
    assertThat(accountActivity.getMerchant().getMerchantNumber()).isEqualTo(merchantNumber);
    assertThat(accountActivity.getMerchant().getName()).isEqualTo(merchantName);
    assertThat(accountActivity.getMerchant().getMerchantCategoryCode())
        .isEqualTo(networkCommon.getMerchantCategoryCode());
    assertThat(accountActivity.getMerchant().getMerchantCategoryGroup())
        .isEqualTo(MccGroup.fromMcc(networkCommon.getMerchantCategoryCode()));
    assertThat(accountActivity.getMerchant().getAmount())
        .isEqualTo(networkCommon.getMerchantAmount());
  }
}
