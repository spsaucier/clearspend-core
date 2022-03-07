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
import com.clearspend.capital.common.typedid.data.ReceiptId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.UserId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.Adjustment;
import com.clearspend.capital.data.model.Allocation;
import com.clearspend.capital.data.model.ExpenseCategory;
import com.clearspend.capital.data.model.Hold;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.embedded.CardDetails;
import com.clearspend.capital.data.model.embedded.ExpenseDetails;
import com.clearspend.capital.data.model.embedded.MerchantDetails;
import com.clearspend.capital.data.model.embedded.PaymentDetails;
import com.clearspend.capital.data.model.enums.AccountActivityIntegrationSyncStatus;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.model.enums.AuthorizationMethod;
import com.clearspend.capital.data.model.enums.MccGroup;
import com.clearspend.capital.data.model.enums.PaymentType;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.data.repository.CardRepositoryCustom.CardDetailsRecord;
import com.clearspend.capital.service.type.CurrentUser;
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
import java.util.Optional;
import javax.transaction.Transactional;
import javax.transaction.Transactional.TxType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
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

  private final ExpenseCategoryService expenseCategoryService;

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
            adjustment.getAmount(),
            AccountActivityIntegrationSyncStatus.NOT_READY);
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
              adjustment.getAmount(),
              AccountActivityIntegrationSyncStatus.NOT_READY);
      holdAccountActivity.setHideAfter(hold.getExpirationDate());
      holdAccountActivity.setHoldId(hold.getId());

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
            adjustment.getAmount(),
            AccountActivityIntegrationSyncStatus.NOT_READY);
    accountActivity.setAdjustmentId(adjustment.getId());

    return accountActivityRepository.save(accountActivity);
  }

  @Transactional(TxType.REQUIRED)
  public void recordHoldReleaseAccountActivity(Hold hold) {
    AccountActivity accountActivity =
        accountActivityRepository.findByHoldId(hold.getId()).stream()
            .min(Comparator.comparing(Versioned::getCreated))
            .orElseThrow(() -> new RecordNotFoundException(Table.ACCOUNT_ACTIVITY, hold.getId()));

    accountActivity.setHideAfter(OffsetDateTime.now(Clock.systemUTC()));
    log.debug(
        "updating account activity {}: hideAfter: {}",
        accountActivity.getId(),
        accountActivity.getHideAfter());

    accountActivityRepository.save(accountActivity);
  }

  @Transactional(TxType.REQUIRED)
  public AccountActivity recordManualAdjustmentActivity(
      Allocation allocation, Adjustment adjustment, String notes) {
    final AccountActivity accountActivity =
        new AccountActivity(
            adjustment.getBusinessId(),
            allocation.getId(),
            allocation.getName(),
            adjustment.getAccountId(),
            AccountActivityType.MANUAL,
            AccountActivityStatus.PROCESSED,
            adjustment.getEffectiveDate(),
            adjustment.getAmount(),
            AccountActivityIntegrationSyncStatus.NOT_READY);
    accountActivity.setAdjustmentId(adjustment.getId());
    accountActivity.setNotes(notes);

    return accountActivityRepository.save(accountActivity);
  }

  @Transactional(TxType.REQUIRED)
  public void recordNetworkAdjustmentAccountActivity(NetworkCommon common, Adjustment adjustment) {
    recordNetworkAccountActivity(common, adjustment.getAmount(), null, adjustment);
  }

  @Transactional(TxType.REQUIRED)
  public void recordNetworkHoldAccountActivity(NetworkCommon common, Hold hold) {
    recordNetworkAccountActivity(common, hold.getAmount(), hold, null);
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
            amount,
            AccountActivityIntegrationSyncStatus.NOT_READY);
    accountActivity.setUserId(common.getCard().getUserId());

    accountActivity.setMerchant(
        new MerchantDetails(
            common.getMerchantName(),
            common.getMerchantType(),
            common.getMerchantNumber(),
            common.getMerchantCategoryCode(),
            MccGroup.fromMcc(common.getMerchantCategoryCode()),
            common.getAccountActivityDetails().getMerchantLogoUrl(),
            common.getAccountActivityDetails().getMerchantLatitude(),
            common.getAccountActivityDetails().getMerchantLongitude()));

    User cardOwner = userService.retrieveUser(common.getCard().getUserId());
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
      accountActivity.setHoldId(hold.getId());
      accountActivity.setHideAfter(hold.getExpirationDate());
    }

    AuthorizationMethod authorizationMethod = common.getAuthorizationMethod();
    if (authorizationMethod != null) {
      accountActivity.setPaymentDetails(
          new PaymentDetails(authorizationMethod, PaymentType.from(authorizationMethod)));
    }

    common.setAccountActivity(accountActivityRepository.save(accountActivity));
  }

  public AccountActivity updateAccountActivity(
      TypedId<BusinessId> businessId,
      TypedId<UserId> userId,
      TypedId<AccountActivityId> accountActivityId,
      String notes,
      Optional<Integer> iconRef) {
    AccountActivity accountActivity = getUserAccountActivity(businessId, userId, accountActivityId);
    String note = StringUtils.isNotEmpty(notes) ? notes : "";
    accountActivity.setNotes(note);
    ExpenseCategory expenseCategory;
    if (iconRef != null) {
      accountActivity.setExpenseDetails(
          iconRef
              .map(expenseCategoryService::retrieveExpenseCategory)
              .map(
                  category -> new ExpenseDetails(category.getIconRef(), category.getCategoryName()))
              .orElse(null));
    }
    log.debug(
        "Set expense details {} to accountActivity {}",
        accountActivity.getExpenseDetails(),
        accountActivity.getId());

    return accountActivityRepository.save(accountActivity);
  }

  public AccountActivity retrieveAccountActivity(
      TypedId<BusinessId> businessId, TypedId<AccountActivityId> accountActivityId) {
    return accountActivityRepository
        .findByBusinessIdAndId(businessId, accountActivityId)
        .orElse(null);
  }

  public AccountActivity retrieveAccountActivityByAdjustmentId(
      TypedId<BusinessId> businessId, TypedId<AdjustmentId> adjustmentId) {
    return accountActivityRepository
        .findByBusinessIdAndAdjustmentId(businessId, adjustmentId)
        .orElseThrow(
            () -> new RecordNotFoundException(Table.ACCOUNT_ACTIVITY, businessId, adjustmentId));
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

  public AccountActivity findByReceiptId(
      TypedId<BusinessId> businessId, TypedId<ReceiptId> receiptId) {

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
                          record.getAmount().getCurrency()
                              + " "
                              + String.format("%.2f", record.getAmount().getAmount()),
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
}
