package com.clearspend.capital.service;

import com.clearspend.capital.common.data.model.Amount;
import com.clearspend.capital.common.data.model.Versioned;
import com.clearspend.capital.common.error.DataAccessViolationException;
import com.clearspend.capital.common.error.IdMismatchException;
import com.clearspend.capital.common.error.IdMismatchException.IdType;
import com.clearspend.capital.common.error.InvalidStateException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.AccountActivityId;
import com.clearspend.capital.common.typedid.data.AdjustmentId;
import com.clearspend.capital.common.typedid.data.CardId;
import com.clearspend.capital.common.typedid.data.ExpenseCategoryId;
import com.clearspend.capital.common.typedid.data.ReceiptId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.Adjustment;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.Card;
import com.clearspend.capital.data.model.Hold;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.BusinessBankAccount;
import com.clearspend.capital.data.model.embedded.AllocationDetails;
import com.clearspend.capital.data.model.embedded.BankAccountDetails;
import com.clearspend.capital.data.model.embedded.CardDetails;
import com.clearspend.capital.data.model.embedded.ExpenseDetails;
import com.clearspend.capital.data.model.embedded.HoldDetails;
import com.clearspend.capital.data.model.embedded.MerchantDetails;
import com.clearspend.capital.data.model.embedded.PaymentDetails;
import com.clearspend.capital.data.model.embedded.UserDetails;
import com.clearspend.capital.data.model.enums.AccountActivityIntegrationSyncStatus;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.model.enums.AuthorizationMethod;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.PaymentType;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.data.repository.CardRepository;
import com.clearspend.capital.data.repository.CardRepositoryCustom.CardDetailsRecord;
import com.clearspend.capital.data.repository.ChartOfAccountsMappingRepository;
import com.clearspend.capital.data.repository.UserRepository;
import com.clearspend.capital.service.type.ChartData;
import com.clearspend.capital.service.type.ChartFilterCriteria;
import com.clearspend.capital.service.type.CurrentUser;
import com.clearspend.capital.service.type.DashboardData;
import com.clearspend.capital.service.type.GraphFilterCriteria;
import com.clearspend.capital.service.type.NetworkCommon;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javax.annotation.Nullable;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountActivityService {

  private final AccountActivityRepository accountActivityRepository;
  private final CardRepository cardRepository;
  private final ChartOfAccountsMappingRepository chartOfAccountsMappingRepository;
  private final ExpenseCategoryService expenseCategoryService;
  private final UserRepository userRepository;

  @Transactional(TxType.REQUIRED)
  void recordBankAccountAccountActivity(
      Allocation allocation,
      AccountActivityType type,
      Adjustment adjustment,
      Hold hold,
      BusinessBankAccount businessBankAccount,
      User user) {
    recordBankAccountAccountActivity(
        allocation, type, adjustment, hold, BankAccountDetails.of(businessBankAccount), user);
  }

  @Transactional(TxType.REQUIRED)
  void recordExternalBankAccountAccountActivity(
      Allocation allocation,
      AccountActivityType type,
      Adjustment adjustment,
      Hold hold,
      String bankName,
      String accountNumberLastFour) {
    BankAccountDetails bankAccountDetails = new BankAccountDetails(bankName);
    bankAccountDetails.setLastFour(accountNumberLastFour);

    recordBankAccountAccountActivity(allocation, type, adjustment, hold, bankAccountDetails, null);
  }

  private void recordBankAccountAccountActivity(
      Allocation allocation,
      AccountActivityType type,
      Adjustment adjustment,
      Hold hold,
      BankAccountDetails bankAccountDetails,
      User user) {
    AccountActivity adjustmentAccountActivity =
        new AccountActivity(
            adjustment.getBusinessId(),
            adjustment.getAccountId(),
            type,
            AccountActivityStatus.PROCESSED,
            AllocationDetails.of(allocation),
            adjustment.getEffectiveDate(),
            adjustment.getAmount(),
            adjustment.getAmount(),
            AccountActivityIntegrationSyncStatus.NOT_READY);
    adjustmentAccountActivity.setAdjustmentId(adjustment.getId());

    adjustmentAccountActivity.setBankAccount(bankAccountDetails);

    if (user != null) {
      adjustmentAccountActivity.setUser(UserDetails.of(user));
    }

    if (hold != null) {
      adjustmentAccountActivity.setVisibleAfter(hold.getExpirationDate());

      AccountActivity holdAccountActivity =
          new AccountActivity(
              adjustment.getBusinessId(),
              adjustment.getAccountId(),
              type,
              AccountActivityStatus.PENDING,
              AllocationDetails.of(allocation),
              hold.getCreated(),
              adjustment.getAmount(),
              adjustment.getAmount(),
              AccountActivityIntegrationSyncStatus.NOT_READY);
      holdAccountActivity.setHideAfter(hold.getExpirationDate());
      holdAccountActivity.setHold(HoldDetails.of(hold));

      accountActivityRepository.save(holdAccountActivity);
    }

    accountActivityRepository.save(adjustmentAccountActivity);
  }

  @Transactional(TxType.REQUIRED)
  AccountActivity recordReallocationAccountActivity(
      Allocation allocation, Allocation flipAllocation, Adjustment adjustment, User user) {
    final AccountActivity accountActivity =
        new AccountActivity(
            adjustment.getBusinessId(),
            adjustment.getAccountId(),
            AccountActivityType.REALLOCATE,
            AccountActivityStatus.PROCESSED,
            AllocationDetails.of(allocation),
            adjustment.getEffectiveDate(),
            adjustment.getAmount(),
            adjustment.getAmount(),
            AccountActivityIntegrationSyncStatus.NOT_READY);
    accountActivity.setAdjustmentId(adjustment.getId());

    if (flipAllocation != null) {
      accountActivity.setFlipAllocation(AllocationDetails.of(flipAllocation));
    }

    if (user != null) {
      accountActivity.setUser(UserDetails.of(user));
    }

    return accountActivityRepository.save(accountActivity);
  }

  @Transactional(TxType.REQUIRED)
  public AccountActivity recordApplyFeeActivity(
      Allocation allocation, Adjustment adjustment, String notes) {
    final AccountActivity accountActivity =
        new AccountActivity(
            adjustment.getBusinessId(),
            adjustment.getAccountId(),
            AccountActivityType.FEE,
            AccountActivityStatus.PROCESSED,
            AllocationDetails.of(allocation),
            adjustment.getEffectiveDate(),
            adjustment.getAmount(),
            adjustment.getAmount(),
            AccountActivityIntegrationSyncStatus.NOT_READY);
    accountActivity.setAdjustmentId(adjustment.getId());
    accountActivity.setNotes(notes);

    return accountActivityRepository.save(accountActivity);
  }

  @Transactional(TxType.REQUIRED)
  public AccountActivity recordCardReturnFundsActivity(
      Allocation allocation, Adjustment adjustment) {
    AccountActivity accountActivity =
        new AccountActivity(
            adjustment.getBusinessId(),
            adjustment.getAccountId(),
            AccountActivityType.CARD_FUND_RETURN,
            AccountActivityStatus.PROCESSED,
            AllocationDetails.of(allocation),
            adjustment.getEffectiveDate(),
            adjustment.getAmount(),
            adjustment.getAmount(),
            AccountActivityIntegrationSyncStatus.NOT_READY);
    accountActivity.setAdjustmentId(adjustment.getId());

    return accountActivityRepository.save(accountActivity);
  }

  @Transactional(TxType.REQUIRED)
  AccountActivity recordHoldReleaseAccountActivity(Hold hold) {
    AccountActivity accountActivity =
        accountActivityRepository.findByHoldId(hold.getId()).stream()
            .min(Comparator.comparing(Versioned::getCreated))
            .orElseThrow(() -> new RecordNotFoundException(Table.ACCOUNT_ACTIVITY, hold.getId()));

    accountActivity.setHideAfter(OffsetDateTime.now(Clock.systemUTC()));
    log.debug(
        "updating account activity {}: hideAfter: {}",
        accountActivity.getId(),
        accountActivity.getHideAfter());

    return accountActivityRepository.save(accountActivity);
  }

  @Transactional(TxType.REQUIRED)
  AccountActivity recordManualAdjustmentActivity(
      Allocation allocation, Adjustment adjustment, String notes) {
    final AccountActivity accountActivity =
        new AccountActivity(
            adjustment.getBusinessId(),
            adjustment.getAccountId(),
            AccountActivityType.MANUAL,
            AccountActivityStatus.PROCESSED,
            AllocationDetails.of(allocation),
            adjustment.getEffectiveDate(),
            adjustment.getAmount(),
            adjustment.getAmount(),
            AccountActivityIntegrationSyncStatus.NOT_READY);
    accountActivity.setAdjustmentId(adjustment.getId());
    accountActivity.setNotes(notes);

    return accountActivityRepository.save(accountActivity);
  }

  @Transactional(TxType.REQUIRED)
  void recordNetworkAdjustmentAccountActivity(NetworkCommon common, Adjustment adjustment) {
    recordNetworkAccountActivity(common, adjustment.getAmount(), null, adjustment);
  }

  @Transactional(TxType.REQUIRED)
  void recordNetworkHoldAccountActivity(NetworkCommon common, Hold hold) {
    recordNetworkAccountActivity(common, hold.getAmount(), hold, null);
  }

  @Transactional(TxType.REQUIRED)
  void recordNetworkDeclineAccountActivity(NetworkCommon common) {
    recordNetworkAccountActivity(common, common.getPaddedAmount(), null, null);
  }

  private void recordNetworkAccountActivity(
      NetworkCommon common, Amount amount, Hold hold, Adjustment adjustment) {

    Allocation allocation = common.getAllocation();
    User cardOwner = userRepository.findById(common.getCard().getUserId()).orElseThrow();

    AccountActivityType accountActivityType = common.getAccountActivityType();

    AccountActivity accountActivity =
        new AccountActivity(
            common.getBusinessId(),
            common.getAccount().getId(),
            accountActivityType,
            common.getAccountActivityDetails().getAccountActivityStatus(),
            AllocationDetails.of(allocation),
            common.getAccountActivityDetails().getActivityTime(),
            amount,
            common.getRequestedAmount(),
            AccountActivityIntegrationSyncStatus.NOT_READY);

    if (accountActivityType != AccountActivityType.NETWORK_REFUND) {
      accountActivity.setUser(UserDetails.of(cardOwner));
    }

    if (common.getDecline() != null) {
      // showing user only one decline reason to user
      accountActivity.setDeclineDetails(common.getDeclineDetails());
    }

    accountActivity.setMerchant(
        new MerchantDetails(
            common.getMerchantName(),
            common.getMerchantAmount(),
            common.getMerchantType(),
            common.getMerchantNumber(),
            common.getMerchantCategoryCode(),
            MccGroup.fromMcc(common.getMerchantCategoryCode()),
            common.getAccountActivityDetails().getMerchantLogoUrl(),
            common.getAccountActivityDetails().getMerchantLatitude(),
            common.getAccountActivityDetails().getMerchantLongitude()));

    accountActivity.setCard(
        new CardDetails(
            common.getCard().getId(),
            common.getCard().getLastFour(),
            cardOwner.getFirstName(),
            cardOwner.getLastName(),
            common.getCard().getExternalRef()));

    if (adjustment != null) {
      accountActivity.setAdjustmentId(adjustment.getId());
    }
    if (hold != null) {
      accountActivity.setHold(HoldDetails.of(hold));
      accountActivity.setHideAfter(hold.getExpirationDate());
    }
    if (common.getPriorAccountActivity() != null) {
      accountActivity.setExpenseDetails(common.getPriorAccountActivity().getExpenseDetails());
      accountActivity.setNotes(common.getPriorAccountActivity().getNotes());
      accountActivity.setReceipt(common.getPriorAccountActivity().getReceipt());
    }

    AuthorizationMethod authorizationMethod = common.getAuthorizationMethod();
    if (authorizationMethod != null) {
      accountActivity.setPaymentDetails(
          new PaymentDetails(authorizationMethod, PaymentType.from(authorizationMethod)));
    }

    accountActivity.setInterchange(common.getInterchange());

    common.setAccountActivity(accountActivityRepository.save(accountActivity));
  }

  @Transactional
  @PreAuthorize(
      "isSelfOwned(#accountActivity) or hasAllocationPermission(#accountActivity.allocationId, 'MANAGE_FUNDS')")
  public AccountActivity updateAccountActivity(
      AccountActivity accountActivity,
      String notes,
      @Nullable TypedId<ExpenseCategoryId> expenseCategoryId) {
    String note = StringUtils.isNotEmpty(notes) ? notes : "";
    accountActivity.setNotes(note);
    // FIXME
    if (expenseCategoryId != null) {
      accountActivity.setExpenseDetails(
          expenseCategoryService
              .getExpenseCategoryById(expenseCategoryId)
              .map(
                  category ->
                      new ExpenseDetails(
                          category.getIconRef(), category.getId(), category.getCategoryName()))
              .orElse(null));
      if (accountActivity
              .getIntegrationSyncStatus()
              .equals(AccountActivityIntegrationSyncStatus.NOT_READY)
          && chartOfAccountsMappingRepository.existsByBusinessIdAndExpenseCategoryId(
              accountActivity.getBusinessId(), expenseCategoryId)) {
        accountActivity.setIntegrationSyncStatus(
            accountActivity
                .getIntegrationSyncStatus()
                .validTransition(AccountActivityIntegrationSyncStatus.READY));
      }
    } else {
      accountActivity.setExpenseDetails(null);
    }
    log.debug(
        "Set expense details {} to accountActivity {}",
        accountActivity.getExpenseDetails(),
        accountActivity.getId());

    return accountActivityRepository.save(accountActivity);
  }

  AccountActivity updateAccountActivitySyncStatus(
      TypedId<BusinessId> businessId,
      TypedId<AccountActivityId> accountActivityId,
      AccountActivityIntegrationSyncStatus status) {
    AccountActivity accountActivity = retrieveAccountActivity(businessId, accountActivityId);
    accountActivity.setIntegrationSyncStatus(status);
    return accountActivityRepository.save(accountActivity);
  }

  AccountActivity updateAccountActivityStatus(
      TypedId<BusinessId> businessId,
      TypedId<AccountActivityId> accountActivityId,
      AccountActivityStatus status) {
    AccountActivity accountActivity = retrieveAccountActivity(businessId, accountActivityId);
    accountActivity.setStatus(status);
    return accountActivityRepository.save(accountActivity);
  }

  @PostAuthorize(
      "isSelfOwned(returnObject) or hasAllocationPermission(returnObject.allocationId, 'MANAGE_FUNDS')")
  public AccountActivity retrieveAccountActivity(
      TypedId<BusinessId> businessId, TypedId<AccountActivityId> accountActivityId) {
    return retrieveAccountActivityForService(businessId, accountActivityId);
  }

  AccountActivity retrieveAccountActivityForService(
      final TypedId<BusinessId> businessId, final TypedId<AccountActivityId> accountActivityId) {
    return accountActivityRepository
        .findByBusinessIdAndId(businessId, accountActivityId)
        .orElse(null);
  }

  AccountActivity retrieveAccountActivityByAdjustmentId(
      TypedId<BusinessId> businessId, TypedId<AdjustmentId> adjustmentId) {
    return accountActivityRepository
        .findByBusinessIdAndAdjustmentId(businessId, adjustmentId)
        .orElseThrow(
            () -> new RecordNotFoundException(Table.ACCOUNT_ACTIVITY, businessId, adjustmentId));
  }

  public List<AccountActivity> findAllSyncableForBusiness(TypedId<BusinessId> businessId) {
    return accountActivityRepository.findByIntegrationSyncStatusAndBusinessId(
        AccountActivityIntegrationSyncStatus.READY, businessId);
  }

  public record CardAccountActivity(Card card, Page<AccountActivity> activityPage) {}

  @PostAuthorize("hasAllocationPermission(returnObject.card().allocationId, 'MANAGE_FUNDS')")
  public CardAccountActivity getCardAccountActivity(
      TypedId<BusinessId> businessId,
      TypedId<UserId> userId,
      TypedId<CardId> cardId,
      AccountActivityFilterCriteria accountActivityFilterCriteria) {
    CardDetailsRecord cardDetailsRecord =
        cardRepository.findDetailsByBusinessIdAndId(businessId, cardId).orElseThrow();
    if (!cardDetailsRecord.card().getUserId().equals(userId)) {
      throw new IdMismatchException(IdType.USER_ID, userId, cardDetailsRecord.card().getUserId());
    }

    accountActivityFilterCriteria.setCardId(cardDetailsRecord.card().getId());
    accountActivityFilterCriteria.setAllocationId(cardDetailsRecord.card().getAllocationId());
    accountActivityFilterCriteria.setUserId(userId);

    final Page<AccountActivity> activityPage =
        accountActivityRepository.find(businessId, accountActivityFilterCriteria);
    return new CardAccountActivity(cardDetailsRecord.card(), activityPage);
  }

  AccountActivity findByReceiptId(TypedId<BusinessId> businessId, TypedId<ReceiptId> receiptId) {

    List<AccountActivity> accountActivities =
        accountActivityRepository.findByReceiptId(receiptId.toUuid());
    if (accountActivities.isEmpty()) {
      throw new RecordNotFoundException(Table.ACCOUNT_ACTIVITY, receiptId);
    }
    if (accountActivities.size() > 1) {
      throw new InvalidStateException(Table.ACCOUNT_ACTIVITY, "Unexpected multiple records");
    }

    AccountActivity accountActivity = accountActivities.get(0);
    if (!accountActivity.getBusinessId().equals(businessId)) {
      throw new DataAccessViolationException(
          Table.ACCOUNT_ACTIVITY, receiptId, businessId, accountActivity.getBusinessId());
    }

    return accountActivity;
  }

  @PostAuthorize(
      "isSelfOwned(returnObject) or hasAllocationPermission(returnObject.allocationId, 'MANAGE_FUNDS')")
  public AccountActivity getAccountActivity(TypedId<AccountActivityId> accountActivityId) {

    return accountActivityRepository
        .findById(accountActivityId)
        .orElseThrow(() -> new RecordNotFoundException(Table.ACCOUNT_ACTIVITY, accountActivityId));
  }

  public byte[] createCSVFile(AccountActivityFilterCriteria filterCriteria) {

    Page<AccountActivity> accountActivityPage =
        accountActivityRepository.find(CurrentUser.get().businessId(), filterCriteria);

    List<String> headerFields =
        Arrays.asList(
            "Date & Time",
            "Card",
            "Cardholder Name",
            "Merchant Name",
            "Merchant Category",
            "Currency",
            "Amount",
            "Status");

    ByteArrayOutputStream csvFile = new ByteArrayOutputStream();
    try (CSVPrinter csvPrinter = new CSVPrinter(new PrintWriter(csvFile), CSVFormat.DEFAULT)) {
      csvPrinter.printRecord(headerFields);
      DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");
      accountActivityPage
          .getContent()
          .forEach(
              record -> {
                try {
                  String lastFour = "";
                  String cardholderName = "";
                  String merchantName = "";
                  String merchantCategory = "";
                  if (record.getCard() != null) {
                    if (record.getCard().getLastFour() != null) {
                      lastFour = "**** " + record.getCard().getLastFour();
                    }
                    if (record.getCard().getOwnerFirstName() != null) {
                      cardholderName = record.getCard().getOwnerFirstName().getEncrypted();
                    }
                    if (record.getCard().getOwnerLastName() != null) {
                      cardholderName += " " + record.getCard().getOwnerLastName().getEncrypted();
                    }
                  }

                  if (record.getMerchant() != null) {
                    if (record.getMerchant().getName() != null) {
                      merchantName = record.getMerchant().getName();
                    }
                    if (record.getMerchant().getType() != null) {
                      merchantCategory = record.getMerchant().getType().getDescription();
                    }
                  }

                  csvPrinter.printRecord(
                      Arrays.asList(
                          dateFormatter.format(record.getActivityTime()),
                          lastFour,
                          cardholderName,
                          merchantName,
                          merchantCategory,
                          record.getAmount().getCurrency(),
                          String.format("%.2f", record.getAmount().getAmount()),
                          record.getStatus()));
                } catch (IOException e) {
                  throw new RuntimeException(e.getMessage());
                }
              });
      csvPrinter.flush();
    } catch (IOException e) {
      throw new RuntimeException(e.getMessage());
    }
    return csvFile.toByteArray();
  }

  public Page<AccountActivity> find(
      TypedId<BusinessId> businessId, AccountActivityFilterCriteria filterCriteria) {
    return accountActivityRepository.find(businessId, filterCriteria);
  }

  public DashboardData findDataForLineGraph(
      TypedId<BusinessId> businessId, GraphFilterCriteria filterCriteria) {
    return accountActivityRepository.findDataForLineGraph(businessId, filterCriteria);
  }

  public ChartData findDataForChart(
      TypedId<BusinessId> businessId, ChartFilterCriteria filterCriteria) {
    return accountActivityRepository.findDataForChart(businessId, filterCriteria);
  }
}
