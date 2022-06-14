package com.clearspend.capital.service;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.AdjustmentId;
import com.clearspend.capital.common.typedid.data.AllocationId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.Adjustment;
import com.clearspend.capital.data.model.enums.AdjustmentType;
import com.clearspend.capital.data.model.ledger.JournalEntry;
import com.clearspend.capital.data.repository.AdjustmentRepository;
import com.clearspend.capital.data.repository.AdjustmentRepositoryCustom.LedgerBalancePeriod;
import com.clearspend.capital.service.LedgerService.BankJournalEntry;
import com.clearspend.capital.service.LedgerService.ManualAdjustmentJournalEntry;
import com.clearspend.capital.service.LedgerService.NetworkJournalEntry;
import com.clearspend.capital.service.LedgerService.ReallocationJournalEntry;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdjustmentService {

  private final LedgerService ledgerService;

  private final AdjustmentRepository adjustmentRepository;

  private final ApplicationEventPublisher eventPublisher;

  public record ReallocateFundsRecord(
      ReallocationJournalEntry bankJournalEntry,
      Adjustment fromAdjustment,
      Adjustment toAdjustment) {}

  public record AdjustmentPersistedEvent(Adjustment adjustment) {}

  public record AdjustmentRecord(JournalEntry journalEntry, Adjustment adjustment) {}

  @Transactional(TxType.REQUIRED)
  Adjustment recordDepositFunds(Account account, OffsetDateTime effectiveDate, Amount amount) {
    BankJournalEntry bankJournalEntry =
        ledgerService.recordDepositFunds(account.getLedgerAccountId(), amount);

    return saveAndPublish(
        new Adjustment(
            account.getBusinessId(),
            account.getAllocationId(),
            account.getId(),
            account.getLedgerAccountId(),
            bankJournalEntry.journalEntry().getId(),
            bankJournalEntry.accountPosting().getId(),
            AdjustmentType.DEPOSIT,
            effectiveDate,
            amount));
  }

  @Transactional(TxType.REQUIRED)
  Adjustment recordReturnFunds(Account account, Amount amount) {
    BankJournalEntry bankJournalEntry =
        ledgerService.recordDepositFunds(account.getLedgerAccountId(), amount);

    return saveAndPublish(
        new Adjustment(
            account.getBusinessId(),
            account.getAllocationId(),
            account.getId(),
            account.getLedgerAccountId(),
            bankJournalEntry.journalEntry().getId(),
            bankJournalEntry.accountPosting().getId(),
            AdjustmentType.RETURN,
            OffsetDateTime.now(ZoneOffset.UTC),
            amount));
  }

  @Transactional(TxType.REQUIRED)
  Adjustment recordCardReturnFunds(Account account, Amount amount) {
    NetworkJournalEntry networkJournalEntry =
        ledgerService.recordNetworkAdjustment(account.getLedgerAccountId(), amount);

    return saveAndPublish(
        new Adjustment(
            account.getBusinessId(),
            account.getAllocationId(),
            account.getId(),
            account.getLedgerAccountId(),
            networkJournalEntry.journalEntry().getId(),
            networkJournalEntry.accountPosting().getId(),
            AdjustmentType.RETURN,
            OffsetDateTime.now(ZoneOffset.UTC),
            amount));
  }

  @Transactional(TxType.REQUIRED)
  Adjustment recordWithdrawFunds(Account account, Amount amount) {
    BankJournalEntry bankJournalEntry =
        ledgerService.recordWithdrawFunds(account.getLedgerAccountId(), amount);

    return saveAndPublish(
        new Adjustment(
            account.getBusinessId(),
            account.getAllocationId(),
            account.getId(),
            account.getLedgerAccountId(),
            bankJournalEntry.journalEntry().getId(),
            bankJournalEntry.accountPosting().getId(),
            AdjustmentType.WITHDRAW,
            OffsetDateTime.now(ZoneOffset.UTC),
            amount.negate()));
  }

  @Transactional(TxType.REQUIRED)
  Adjustment recordApplyFee(Account account, Amount amount) {
    BankJournalEntry bankJournalEntry =
        ledgerService.recordApplyFee(account.getLedgerAccountId(), amount);

    return saveAndPublish(
        new Adjustment(
            account.getBusinessId(),
            account.getAllocationId(),
            account.getId(),
            account.getLedgerAccountId(),
            bankJournalEntry.journalEntry().getId(),
            bankJournalEntry.accountPosting().getId(),
            AdjustmentType.FEE,
            OffsetDateTime.now(ZoneOffset.UTC),
            amount.negate()));
  }

  @Transactional(TxType.REQUIRED)
  ReallocateFundsRecord reallocateFunds(Account fromAccount, Account toAccount, Amount amount) {
    ReallocationJournalEntry bankJournalEntry =
        ledgerService.recordReallocateFunds(
            fromAccount.getLedgerAccountId(), toAccount.getLedgerAccountId(), amount);

    Adjustment fromAdjustment =
        saveAndPublish(
            new Adjustment(
                fromAccount.getBusinessId(),
                fromAccount.getAllocationId(),
                fromAccount.getId(),
                fromAccount.getLedgerAccountId(),
                bankJournalEntry.journalEntry().getId(),
                bankJournalEntry.fromPosting().getId(),
                AdjustmentType.REALLOCATE,
                OffsetDateTime.now(ZoneOffset.UTC),
                amount.negate()));

    Adjustment toAdjustment =
        saveAndPublish(
            new Adjustment(
                toAccount.getBusinessId(),
                toAccount.getAllocationId(),
                toAccount.getId(),
                toAccount.getLedgerAccountId(),
                bankJournalEntry.journalEntry().getId(),
                bankJournalEntry.toPosting().getId(),
                AdjustmentType.REALLOCATE,
                OffsetDateTime.now(ZoneOffset.UTC),
                amount));

    return new ReallocateFundsRecord(bankJournalEntry, fromAdjustment, toAdjustment);
  }

  @Transactional(TxType.REQUIRED)
  AdjustmentRecord recordManualAdjustment(Account account, Amount amount) {
    ManualAdjustmentJournalEntry manualAdjustmentJournalEntry =
        ledgerService.recordManualAdjustment(account.getLedgerAccountId(), amount);

    Adjustment adjustment =
        saveAndPublish(
            new Adjustment(
                account.getBusinessId(),
                account.getAllocationId(),
                account.getId(),
                account.getLedgerAccountId(),
                manualAdjustmentJournalEntry.journalEntry().getId(),
                manualAdjustmentJournalEntry.accountPosting().getId(),
                AdjustmentType.MANUAL,
                OffsetDateTime.now(ZoneOffset.UTC),
                manualAdjustmentJournalEntry.accountPosting().getAmount()));

    account.setLedgerBalance(account.getLedgerBalance().add(amount));
    account.recalculateAvailableBalance();

    return new AdjustmentRecord(manualAdjustmentJournalEntry.journalEntry(), adjustment);
  }

  @Transactional(TxType.REQUIRED)
  AdjustmentRecord recordNetworkAdjustment(
      @NonNull final TypedId<AllocationId> allocationId,
      @NonNull final Account account,
      @NonNull final Amount amount) {
    NetworkJournalEntry networkJournalEntry =
        ledgerService.recordNetworkAdjustment(account.getLedgerAccountId(), amount);

    Adjustment adjustment =
        saveAndPublish(
            new Adjustment(
                account.getBusinessId(),
                allocationId,
                account.getId(),
                account.getLedgerAccountId(),
                networkJournalEntry.journalEntry().getId(),
                networkJournalEntry.accountPosting().getId(),
                AdjustmentType.NETWORK,
                OffsetDateTime.now(ZoneOffset.UTC),
                networkJournalEntry.accountPosting().getAmount()));

    return new AdjustmentRecord(networkJournalEntry.journalEntry(), adjustment);
  }

  List<Adjustment> retrieveBusinessAdjustments(
      TypedId<BusinessId> businessId, List<AdjustmentType> adjustmentTypes, int daysAgo) {
    OffsetDateTime before = OffsetDateTime.now(ZoneOffset.UTC).minusDays(daysAgo);
    return adjustmentRepository.findByBusinessIdAndTypeInAndEffectiveDateAfter(
        businessId, adjustmentTypes, before);
  }

  Adjustment retrieveAdjustment(
      TypedId<BusinessId> businessId, TypedId<AdjustmentId> adjustmentId) {
    return adjustmentRepository
        .findByBusinessIdAndId(businessId, adjustmentId)
        .orElseThrow(() -> new RecordNotFoundException(Table.ADJUSTMENT, businessId, adjustmentId));
  }

  LedgerBalancePeriod getBusinessLedgerBalanceForPeriod(
      TypedId<BusinessId> businessId, OffsetDateTime from, OffsetDateTime to) {
    return adjustmentRepository.findBusinessLedgerBalanceForPeriod(businessId, from, to);
  }

  Adjustment updateEffectiveDate(
      TypedId<BusinessId> businessId,
      TypedId<AdjustmentId> adjustmentId,
      OffsetDateTime effectiveDate) {
    Adjustment adjustment = retrieveAdjustment(businessId, adjustmentId);
    adjustment.setEffectiveDate(effectiveDate);

    return saveAndPublish(adjustment);
  }

  private Adjustment saveAndPublish(Adjustment adjustment) {
    Adjustment savedAdjustment = adjustmentRepository.save(adjustment);
    eventPublisher.publishEvent(new AdjustmentPersistedEvent(savedAdjustment));

    return savedAdjustment;
  }
}
