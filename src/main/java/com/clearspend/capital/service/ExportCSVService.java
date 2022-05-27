package com.clearspend.capital.service;

import com.clearspend.capital.controller.type.activity.AllocationInfo;
import com.clearspend.capital.controller.type.activity.Merchant;
import com.clearspend.capital.controller.type.common.CardInfo;
import com.clearspend.capital.controller.type.ledger.BankInfo;
import com.clearspend.capital.controller.type.ledger.LedgerAccount;
import com.clearspend.capital.controller.type.ledger.LedgerActivityResponse;
import com.clearspend.capital.controller.type.ledger.LedgerAllocationAccount;
import com.clearspend.capital.controller.type.ledger.LedgerBankAccount;
import com.clearspend.capital.controller.type.ledger.LedgerCardAccount;
import com.clearspend.capital.controller.type.ledger.LedgerMerchantAccount;
import com.clearspend.capital.controller.type.ledger.UserInfo;
import com.clearspend.capital.data.model.AccountActivity;
import com.clearspend.capital.data.model.embedded.ExpenseDetails;
import com.clearspend.capital.data.model.embedded.ReceiptDetails;
import com.clearspend.capital.permissioncheck.annotations.OpenAccessAPI;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class ExportCSVService {

  public static final DateTimeFormatter DATE_TIME_FORMATTER =
      DateTimeFormatter.ofPattern("MM/dd/yyyy HH:mm:ss");

  @OpenAccessAPI(
      explanation = "Only uses data user acquires from another secured call",
      reviewer = "vakimov")
  public byte[] fromAccountActivity(List<AccountActivity> accountActivities) {
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
      accountActivities.forEach(
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
                      DATE_TIME_FORMATTER.format(record.getActivityTime()),
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

  public byte[] fromLedgerActivity(
      List<LedgerActivityResponse> ledgerActivities, boolean transactionsOnly) {
    List<String> headerFields =
        Arrays.asList(
            "Date & Time",
            "Transaction",
            "User",
            "Initiating Account",
            "Target Account",
            "Currency",
            "Amount",
            "Status");

    if (transactionsOnly) {
      headerFields = new ArrayList<>(headerFields);
      headerFields.add("Expense Category");
      headerFields.add("Receipt");
    }

    ByteArrayOutputStream csvFile = new ByteArrayOutputStream();
    try (CSVPrinter csvPrinter = new CSVPrinter(new PrintWriter(csvFile), CSVFormat.DEFAULT)) {
      csvPrinter.printRecord(headerFields);
      ledgerActivities.forEach(
          record -> {
            try {
              String user =
                  switch (record.getUser().getType()) {
                    case SYSTEM -> "SYSTEM";
                    case EXTERNAL -> "EXTERNAL";
                    case USER -> "%s %s %s"
                        .formatted(
                            BeanUtils.getOrDefault(
                                record.getUser().getUserInfo(),
                                UserInfo::getFirstName,
                                StringUtils.EMPTY),
                            BeanUtils.getOrDefault(
                                record.getUser().getUserInfo(),
                                UserInfo::getLastName,
                                StringUtils.EMPTY),
                            BeanUtils.getOrDefault(
                                record.getUser().getUserInfo(),
                                UserInfo::getEmail,
                                StringUtils.EMPTY))
                        .trim();
                  };

              String sourceAccount = getLedgerAccountName(record.getAccount());
              String targetAccount = getLedgerAccountName(record.getReferenceAccount());

              List<Object> values =
                  Arrays.asList(
                      DATE_TIME_FORMATTER.format(record.getActivityTime()),
                      record.getType(),
                      user,
                      sourceAccount,
                      targetAccount,
                      record.getAmount().getCurrency(),
                      String.format("%.2f", record.getAmount().getAmount()),
                      record.getStatus());

              if (transactionsOnly) {
                values = new ArrayList<>(values);
                Boolean expenseCategoryExists =
                    BeanUtils.getOrDefault(
                            record.getAccountActivity().getExpenseDetails(),
                            ExpenseDetails::getExpenseCategoryId,
                            null)
                        != null;
                Boolean receiptExists =
                    !BeanUtils.getOrDefault(
                            record.getAccountActivity().getReceipt(),
                            ReceiptDetails::getReceiptIds,
                            Set.of())
                        .isEmpty();

                values.add(expenseCategoryExists);
                values.add(receiptExists);
              }
              csvPrinter.printRecord(values);
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

  private String getLedgerAccountName(LedgerAccount ledgerAccount) {
    if (ledgerAccount instanceof LedgerCardAccount ledgerCardAccount) {
      String lastFour =
          BeanUtils.getOrDefault(
              ledgerCardAccount.getCardInfo(), CardInfo::getLastFour, StringUtils.EMPTY);
      return StringUtils.isNotEmpty(lastFour)
          ? StringUtils.prependIfMissing(lastFour, "**** ")
          : StringUtils.EMPTY;
    } else if (ledgerAccount instanceof LedgerAllocationAccount ledgerAllocationAccount) {
      return BeanUtils.getOrDefault(
          ledgerAllocationAccount.getAllocationInfo(), AllocationInfo::getName, StringUtils.EMPTY);
    } else if (ledgerAccount instanceof LedgerBankAccount ledgerBankAccount) {
      String bankName =
          BeanUtils.getOrDefault(
              ledgerBankAccount.getBankInfo(), BankInfo::getName, StringUtils.EMPTY);
      String lastFour =
          BeanUtils.getOrDefault(
              ledgerBankAccount.getBankInfo(),
              BankInfo::getAccountNumberLastFour,
              StringUtils.EMPTY);
      return "%s %s"
          .formatted(
              bankName,
              StringUtils.isNotEmpty(lastFour)
                  ? StringUtils.prependIfMissing(lastFour, "**** ")
                  : StringUtils.EMPTY)
          .trim();
    } else if (ledgerAccount instanceof LedgerMerchantAccount ledgerMerchantAccount) {
      return BeanUtils.getOrDefault(
          ledgerMerchantAccount.getMerchantInfo(), Merchant::getName, StringUtils.EMPTY);
    } else {
      return StringUtils.EMPTY;
    }
  }
}
