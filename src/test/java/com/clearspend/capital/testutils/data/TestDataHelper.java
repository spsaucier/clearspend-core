package com.clearspend.capital.testutils.data;

import com.clearspend.capital.TestHelper;
import com.clearspend.capital.common.data.model.Address;
import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.typedid.data.AccountId;
import com.clearspend.capital.common.typedid.data.AdjustmentId;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.data.ledger.JournalEntryId;
import com.clearspend.capital.common.typedid.data.ledger.LedgerAccountId;
import com.clearspend.capital.common.typedid.data.ledger.PostingId;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.Adjustment;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.TransactionLimit;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.embedded.MerchantDetails;
import com.clearspend.capital.data.model.enums.AccountActivityIntegrationSyncStatus;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.model.enums.AdjustmentType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.model.enums.FundingType;
import com.clearspend.capital.data.model.enums.LedgerAccountType;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.MerchantType;
import com.clearspend.capital.data.model.enums.TransactionLimitType;
import com.clearspend.capital.data.model.enums.card.BinType;
import com.clearspend.capital.data.model.enums.card.CardStatus;
import com.clearspend.capital.data.model.enums.card.CardStatusReason;
import com.clearspend.capital.data.model.enums.card.CardType;
import com.clearspend.capital.data.model.ledger.JournalEntry;
import com.clearspend.capital.data.model.ledger.LedgerAccount;
import com.clearspend.capital.data.model.ledger.Posting;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.data.repository.AdjustmentRepository;
import com.clearspend.capital.data.repository.CardRepository;
import com.clearspend.capital.data.repository.TransactionLimitRepository;
import com.clearspend.capital.data.repository.ledger.JournalEntryRepository;
import com.clearspend.capital.data.repository.ledger.LedgerAccountRepository;
import com.clearspend.capital.data.repository.ledger.PostingRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import javax.annotation.Nullable;
import lombok.Builder;
import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
public class TestDataHelper {
  private final AdjustmentRepository adjustmentRepo;
  private final LedgerAccountRepository ledgerAccountRepo;
  private final JournalEntryRepository journalEntryRepo;
  private final PostingRepository postingRepo;
  private final AccountActivityRepository accountActivityRepo;
  private final CardRepository cardRepo;
  private final TransactionLimitRepository transactionLimitRepo;

  private Amount createAmount() {
    final Amount amount = new Amount();
    amount.setCurrency(Currency.USD);
    amount.setAmount(new BigDecimal(10));
    return amount;
  }

  public LedgerAccount createLedgerAccount() {
    final LedgerAccount ledgerAccount = new LedgerAccount();
    ledgerAccount.setType(LedgerAccountType.BANK);
    ledgerAccount.setCurrency(Currency.USD);
    return ledgerAccountRepo.save(ledgerAccount);
  }

  public JournalEntry createJournalEntry() {
    return journalEntryRepo.save(new JournalEntry());
  }

  public Posting createPosting(
      @NonNull final TypedId<LedgerAccountId> ledgerAccountId,
      @NonNull final JournalEntry journalEntry) {
    final Posting posting = new Posting();
    posting.setLedgerAccountId(ledgerAccountId);
    posting.setAmount(createAmount());
    posting.setJournalEntry(journalEntry);
    return postingRepo.save(posting);
  }

  public Adjustment createAdjustment(@NonNull AdjustmentConfig config) {
    final Adjustment adjustment = new Adjustment();
    adjustment.setBusinessId(config.getBusinessId());
    adjustment.setAllocationId(config.getAllocationId());
    adjustment.setAccountId(config.getAccountId());
    adjustment.setLedgerAccountId(config.getLedgerAccountId());
    adjustment.setJournalEntryId(config.getJournalEntryId());
    adjustment.setPostingId(config.getPostingId());
    adjustment.setType(AdjustmentType.DEPOSIT);
    adjustment.setEffectiveDate(OffsetDateTime.now());
    adjustment.setAmount(createAmount());

    return adjustmentRepo.save(adjustment);
  }

  public AdjustmentRecord createAdjustmentWithJoins(
      @NonNull final AdjustmentWithJoinsConfig adjWithJoinsConfig) {
    final LedgerAccount ledgerAccount = createLedgerAccount();
    final JournalEntry journalEntry = createJournalEntry();
    final Posting posting = createPosting(ledgerAccount.getId(), journalEntry);
    final AdjustmentConfig adjustmentConfig =
        AdjustmentConfig.builder()
            .businessId(adjWithJoinsConfig.getBusinessId())
            .allocationId(adjWithJoinsConfig.getAllocationId())
            .accountId(adjWithJoinsConfig.getAccountId())
            .ledgerAccountId(ledgerAccount.getId())
            .journalEntryId(journalEntry.getId())
            .postingId(posting.getId())
            .build();
    final Adjustment adjustment = createAdjustment(adjustmentConfig);
    return new AdjustmentRecord(adjustment, ledgerAccount, posting, journalEntry);
  }

  public AccountActivity createAccountActivity(final AccountActivityConfig config) {
    final AccountActivity accountActivity =
        new AccountActivity(
            config.getBusiness().getId(),
            config.getAllocation().getId(),
            config.getAllocation().getName(),
            config.getAccountId(),
            AccountActivityType.BANK_DEPOSIT,
            AccountActivityStatus.APPROVED,
            OffsetDateTime.now(),
            Amount.of(config.getBusiness().getCurrency(), BigDecimal.ONE),
            Amount.of(config.getBusiness().getCurrency(), BigDecimal.ONE),
            AccountActivityIntegrationSyncStatus.NOT_READY);
    accountActivity.setNotes("");

    final MerchantDetails merchantDetails = new MerchantDetails();
    merchantDetails.setName("");
    merchantDetails.setType(MerchantType.AC_REFRIGERATION_REPAIR);
    merchantDetails.setMerchantCategoryCode(1);
    merchantDetails.setMerchantCategoryGroup(MccGroup.CHILD_CARE);
    accountActivity.setMerchant(merchantDetails);

    config.getOwnerId().ifPresent(accountActivity::setUserId);
    config.getAdjustmentId().ifPresent(accountActivity::setAdjustmentId);
    return accountActivityRepo.save(accountActivity);
  }

  public TransactionLimit createTransactionLimit(
      final TypedId<BusinessId> businessId, final UUID ownerId) {
    final TransactionLimit transactionLimit = new TransactionLimit();
    transactionLimit.setBusinessId(businessId);
    transactionLimit.setType(TransactionLimitType.CARD);
    transactionLimit.setOwnerId(ownerId);
    transactionLimit.setLimits(Map.of());
    transactionLimit.setDisabledMccGroups(Set.of());
    transactionLimit.setDisabledPaymentTypes(Set.of());
    return transactionLimitRepo.save(transactionLimit);
  }

  public CardAndLimit createCardAndLimit(final CardConfig cardConfig) {
    final Card card = createCard(cardConfig);
    final TransactionLimit transactionLimit =
        createTransactionLimit(card.getBusinessId(), card.getId().toUuid());
    return new CardAndLimit(card, transactionLimit);
  }

  public Card createCard(final CardConfig cardConfig) {
    final Card card = new Card();
    card.setBusinessId(cardConfig.getBusinessId());
    card.setAllocationId(cardConfig.getAllocationId());
    card.setAccountId(cardConfig.getAccountId());
    card.setUserId(cardConfig.getUserId());
    card.setStatus(CardStatus.ACTIVE);
    card.setStatusReason(CardStatusReason.NONE);
    card.setBinType(BinType.DEBIT);
    card.setFundingType(FundingType.INDIVIDUAL);
    card.setType(CardType.VIRTUAL);
    card.setIssueDate(OffsetDateTime.now().minusDays(1));
    card.setExpirationDate(LocalDate.now().plusYears(10));
    card.setActivated(true);
    card.setActivationDate(OffsetDateTime.now());
    card.setCardLine3("");
    card.setCardLine4("");
    card.setLastFour("");
    card.setShippingAddress(new Address());

    return cardRepo.save(card);
  }

  @Data
  @Builder
  public static class CardConfig {
    @NonNull private final TypedId<BusinessId> businessId;
    @NonNull private final TypedId<AllocationId> allocationId;
    @NonNull private final TypedId<AccountId> accountId;
    @NonNull private final TypedId<UserId> userId;

    public static CardConfigBuilder fromCreateBusinessRecord(
        final TestHelper.CreateBusinessRecord createBusinessRecord) {
      return CardConfig.builder()
          .businessId(createBusinessRecord.business().getId())
          .allocationId(createBusinessRecord.allocationRecord().allocation().getId())
          .accountId(createBusinessRecord.allocationRecord().account().getId())
          .userId(createBusinessRecord.user().getId());
    }
  }

  @Data
  @Builder
  public static class AccountActivityConfig {
    @NonNull private final Business business;
    @NonNull private final Allocation allocation;
    @NonNull private final TypedId<AccountId> accountId;
    @Nullable private final TypedId<UserId> ownerId;
    @Nullable private final TypedId<AdjustmentId> adjustmentId;

    public static AccountActivityConfigBuilder fromCreateBusinessRecord(
        final TestHelper.CreateBusinessRecord createBusinessRecord) {
      return AccountActivityConfig.builder()
          .business(createBusinessRecord.business())
          .allocation(createBusinessRecord.allocationRecord().allocation())
          .accountId(createBusinessRecord.allocationRecord().account().getId());
    }

    public Optional<TypedId<UserId>> getOwnerId() {
      return Optional.ofNullable(ownerId);
    }

    public Optional<TypedId<AdjustmentId>> getAdjustmentId() {
      return Optional.ofNullable(adjustmentId);
    }
  }

  @Data
  @Builder
  public static class AdjustmentConfig {
    @NonNull private final TypedId<BusinessId> businessId;
    @NonNull private final TypedId<AllocationId> allocationId;
    @NonNull private final TypedId<AccountId> accountId;
    @NonNull private final TypedId<LedgerAccountId> ledgerAccountId;
    @NonNull private final TypedId<JournalEntryId> journalEntryId;
    @NonNull private final TypedId<PostingId> postingId;

    public static AdjustmentConfigBuilder fromCreateBusinessRecord(
        final TestHelper.CreateBusinessRecord createBusinessRecord) {
      return AdjustmentConfig.builder()
          .businessId(createBusinessRecord.business().getId())
          .allocationId(createBusinessRecord.allocationRecord().allocation().getId())
          .accountId(createBusinessRecord.allocationRecord().account().getId());
    }
  }

  @Data
  @Builder
  public static class AdjustmentWithJoinsConfig {
    @NonNull private final TypedId<BusinessId> businessId;
    @NonNull private final TypedId<AllocationId> allocationId;
    @NonNull private final TypedId<AccountId> accountId;

    public static AdjustmentWithJoinsConfigBuilder fromCreateBusinessRecord(
        final TestHelper.CreateBusinessRecord createBusinessRecord) {
      return AdjustmentWithJoinsConfig.builder()
          .businessId(createBusinessRecord.business().getId())
          .allocationId(createBusinessRecord.allocationRecord().allocation().getId())
          .accountId(createBusinessRecord.allocationRecord().account().getId());
    }
  }

  public record AdjustmentRecord(
      Adjustment adjustment,
      LedgerAccount ledgerAccount,
      Posting posting,
      JournalEntry journalEntry) {}

  public record CardAndLimit(Card card, TransactionLimit transactionLimit) {}
}
