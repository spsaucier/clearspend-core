package com.clearspend.capital.service;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.activity.CardStatementRequest;
import com.clearspend.capital.controller.type.ledger.LedgerAccount;
import com.clearspend.capital.controller.type.ledger.LedgerActivityResponse;
import com.clearspend.capital.controller.type.ledger.LedgerAllocationAccount;
import com.clearspend.capital.controller.type.ledger.LedgerBankAccount;
import com.clearspend.capital.controller.type.ledger.LedgerCardAccount;
import com.clearspend.capital.controller.type.ledger.LedgerMerchantAccount;
import com.clearspend.capital.controller.type.ledger.LedgerUser;
import com.clearspend.capital.controller.type.ledger.UserInfo;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.enums.AccountActivityStatus;
import com.clearspend.capital.data.model.enums.AccountActivityType;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.data.repository.AdjustmentRepositoryCustom.LedgerBalancePeriod;
import com.clearspend.capital.data.repository.CardRepositoryCustom.CardDetailsRecord;
import com.clearspend.capital.service.type.CardStatementData;
import com.clearspend.capital.service.type.CardStatementFilterCriteria;
import com.clearspend.capital.service.type.CurrentUser;
import com.clearspend.capital.service.type.PageToken;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.WordUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class StatementService {

  private static final List<AccountActivityType> BUSINESS_STATEMENT_TYPES =
      ListUtils.subtract(
          List.copyOf(EnumSet.allOf(AccountActivityType.class)),
          List.of(AccountActivityType.REALLOCATE, AccountActivityType.NETWORK_AUTHORIZATION));

  private static final List<AccountActivityStatus> BUSINESS_STATEMENT_STATES =
      ListUtils.subtract(
          List.copyOf(EnumSet.allOf(AccountActivityStatus.class)),
          List.of(AccountActivityStatus.PENDING));

  private static final Set<AccountActivityStatus> BUSINESS_BALANCE_CHANGE_STATES =
      Set.of(
          AccountActivityStatus.APPROVED,
          AccountActivityStatus.CREDIT,
          AccountActivityStatus.PROCESSED);

  private final UserService userService;
  private final AccountActivityRepository accountActivityRepository;
  private final AdjustmentService adjustmentService;

  private final BaseFont defaultBaseFont;
  private final Font fontNormal40;
  private final Font fontNormal9;
  private final Font fontBold32;
  private final Font fontNormal8;
  private final Font fontBold24;
  private final Font fontBold10Gray;
  private final Font fontNormal11;
  private final Font fontNormal12;
  private final Font fontBoldItalic9;
  private final Font fontBold8;
  private final Image logoImage;

  public StatementService(
      UserService userService,
      AccountActivityRepository accountActivityRepository,
      AdjustmentService adjustmentService,
      @Value("classpath:reports/logo_black_300px.png") Resource logoResource)
      throws IOException {
    this.userService = userService;
    this.accountActivityRepository = accountActivityRepository;
    this.adjustmentService = adjustmentService;

    defaultBaseFont = BaseFont.createFont(BaseFont.HELVETICA, "UTF8", BaseFont.EMBEDDED);

    fontNormal8 = new Font(defaultBaseFont, 8, Font.NORMAL, Color.BLACK);
    fontNormal9 = new Font(defaultBaseFont, 9, Font.NORMAL, Color.BLACK);
    fontNormal11 = new Font(defaultBaseFont, 11, Font.NORMAL, Color.BLACK);
    fontNormal12 = new Font(defaultBaseFont, 12, Font.NORMAL, Color.BLACK);
    fontNormal40 = new Font(defaultBaseFont, 40, Font.NORMAL, Color.BLACK);

    fontBold8 = new Font(defaultBaseFont, 8, Font.BOLD, Color.BLACK);
    fontBold24 = new Font(defaultBaseFont, 24, Font.BOLD, Color.BLACK);
    fontBold32 = new Font(defaultBaseFont, 32, Font.BOLD, Color.BLACK);

    fontBold10Gray = new Font(defaultBaseFont, 10, Font.BOLD, Color.GRAY);

    fontBoldItalic9 = new Font(defaultBaseFont, 9, Font.BOLDITALIC, Color.BLACK);

    logoImage = Image.getInstance(IOUtils.toByteArray(logoResource.getInputStream()));
  }

  public record StatementRecord(String fileName, byte[] pdf) {}

  private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
  private static final DateTimeFormatter dateFormatterFileName =
      DateTimeFormatter.ofPattern("MM_dd_yyyy");

  private static class PdfPCellLeft extends PdfPCell {

    public PdfPCellLeft(Paragraph paragraph) {
      super(paragraph);
      this.setBorder(0);
      this.setPaddingBottom(5);
    }

    public PdfPCellLeft(String text, Font font) {
      this(new Paragraph(new Chunk(text, font)));
    }
  }

  private static class PDFCellActivitiesLeft extends PdfPCell {

    public PDFCellActivitiesLeft(Paragraph paragraph, Color backgroundColor) {
      super(paragraph);
      this.setBorderColor(Color.WHITE);
      this.setBorderColorTop(backgroundColor);
      this.setBorderWidth(0);
      this.setBorderWidthTop(1);
      this.setPaddingBottom(10);
      this.setPaddingTop(5);
    }

    public PDFCellActivitiesLeft(Paragraph paragraph) {
      this(paragraph, new Color(240, 240, 240));
    }

    public PDFCellActivitiesLeft(String text, Font font) {
      this(new Paragraph(new Chunk(text, font)));
    }
  }

  private static class PdfCellActivitiesRight extends PdfPCell {

    public PdfCellActivitiesRight(Paragraph paragraph, Color backgroundColor) {
      super(paragraph);
      this.setHorizontalAlignment(Element.ALIGN_RIGHT);
      this.setBorderColor(Color.WHITE);
      this.setBorderColorTop(backgroundColor);
      this.setBorderWidth(0);
      this.setBorderWidthTop(1);
      this.setPaddingBottom(10);
      this.setPaddingTop(5);
    }

    public PdfCellActivitiesRight(Paragraph paragraph) {
      this(paragraph, new Color(230, 230, 230));
    }

    public PdfCellActivitiesRight(String text, Font font) {
      this(new Paragraph(new Chunk(text, font)));
    }

    public PdfCellActivitiesRight(String text, Font font, Color color) {
      this(new Paragraph(new Chunk(text, font)), color);
      setBackgroundColor(color);
    }
  }

  private static class PdfPCellRight extends PdfPCell {

    public PdfPCellRight(Paragraph paragraph) {
      super(paragraph);
      this.setBorder(0);
      this.setHorizontalAlignment(Element.ALIGN_RIGHT);
      this.setPaddingBottom(5);
    }

    public PdfPCellRight(String text, Font font) {
      this(new Paragraph(new Chunk(text, font)));
    }
  }

  @PreAuthorize("hasRootPermission(#businessId, 'READ|GLOBAL_READ|CUSTOMER_SERVICE')")
  public StatementRecord generateBusinessStatementPdf(
      TypedId<BusinessId> businessId, OffsetDateTime startDate, OffsetDateTime endDate) {
    LedgerBalancePeriod ledgerBalancePeriod =
        adjustmentService.getBusinessLedgerBalanceForPeriod(businessId, startDate, endDate);

    Document document = new Document(PageSize.A4.rotate());
    ByteArrayOutputStream pdfStream = new ByteArrayOutputStream();
    PdfWriter writer = PdfWriter.getInstance(document, pdfStream);
    document.open();

    // Draw background pattern, which consists of a thick angular line, a straight thick line
    // and a circle which creates effect of the curly edge
    // Use this value to adjust the background pattern's vertical position
    float deltaY = 30;
    PdfContentByte directContent = writer.getDirectContent();

    directContent.setLineWidth(500);
    directContent.setRGBColorStroke(230, 230, 230);
    directContent.moveTo(document.left() - 100, 500 + deltaY);
    directContent.lineTo(document.right() - 40, 603 + deltaY);
    directContent.stroke();
    directContent.moveTo(document.left(), 652 + deltaY);
    directContent.lineTo(document.right() + 100, 652 + deltaY);
    directContent.stroke();

    directContent.setLineWidth(50f);
    directContent.circle(document.right() - 14, 405 + deltaY, 25f);
    directContent.stroke();

    // For visual spacing
    Paragraph emptyParagraph = new Paragraph(" ");

    // Table for first logo image
    PdfPTable headerLogo = new PdfPTable(2);
    headerLogo.setWidthPercentage(100);
    headerLogo.setWidths(new int[] {2, 8});
    PdfPCell cell = new PdfPCell(logoImage, true);
    cell.setBorder(0);
    cell.setPaddingLeft(4);
    headerLogo.addCell(cell);
    headerLogo.addCell(new PdfPCellLeft(emptyParagraph));
    document.add(headerLogo);

    // Table for Monthly Statement text
    PdfPTable headerTable = new PdfPTable(1);
    headerTable.setWidthPercentage(100);

    headerTable.addCell(new PdfPCellLeft("Monthly Statement", fontNormal40));
    headerTable.addCell(new PdfPCellLeft(emptyParagraph));
    document.add(headerTable);

    // Table for other header details
    PdfPTable midTable = new PdfPTable(2);
    midTable.setWidthPercentage(100);

    // Give it enough spacing
    midTable.addCell(new PdfPCellLeft(emptyParagraph));
    midTable.addCell(new PdfPCellLeft(emptyParagraph));

    // Column
    midTable.addCell(new PdfPCellLeft("Balance delta this period:", fontNormal9));

    // Column
    midTable.addCell(
        new PdfPCellRight(
            "Statement " + dateFormatter.format(startDate) + " - " + dateFormatter.format(endDate),
            fontNormal9));

    // Column
    BigDecimal ledgerPeriodDelta =
        ledgerBalancePeriod.endingBalance().subtract(ledgerBalancePeriod.startingBalance());
    midTable.addCell(new PdfPCellLeft("$" + String.format("%,.2f", ledgerPeriodDelta), fontBold32));

    // empty cell to complete row
    midTable.addCell(new PdfPCellRight(emptyParagraph));

    // Empty line
    midTable.addCell(new PdfPCellLeft(emptyParagraph));
    midTable.addCell(new PdfPCellLeft(emptyParagraph));

    // Column
    midTable.addCell(
        new PdfPCellLeft(
            "Thank you for using ClearSpend.\n\nFor details and upcoming payments, log into your ClearSpend account",
            fontNormal8));

    midTable.addCell(new PdfPCellRight(emptyParagraph));

    document.add(midTable);

    // Table for 'Transactions' label
    PdfPTable tableLabel = new PdfPTable(1);
    tableLabel.setWidthPercentage(100);

    for (int i = 0; i < 3; i++) {
      tableLabel.addCell(new PdfPCellLeft(emptyParagraph));
    }

    tableLabel.addCell(new PdfPCellLeft("Transactions", fontBold24));

    tableLabel.addCell(new PdfPCellLeft(emptyParagraph));
    tableLabel.addCell(new PdfPCellLeft(emptyParagraph));
    document.add(tableLabel);

    // Main statement table
    PdfPTable table = new PdfPTable(new float[] {2, 1.5f, 1.5f, 2.5f, 2, 1.5f, 1.5f});
    table.setWidthPercentage(100);

    table.addCell(new PdfPCellLeft("DATE", fontBold10Gray));
    table.addCell(new PdfPCellLeft("Account", fontBold10Gray));
    table.addCell(new PdfPCellLeft("Transaction Type", fontBold10Gray));
    table.addCell(new PdfPCellLeft("Reference", fontBold10Gray));
    table.addCell(new PdfPCellLeft("Employee", fontBold10Gray));
    table.addCell(new PdfPCellRight("AMOUNT", fontBold10Gray));
    table.addCell(new PdfPCellRight("BALANCE", fontBold10Gray));

    // activity record rows
    AtomicReference<BigDecimal> operationLedgerBalance =
        new AtomicReference<>(ledgerBalancePeriod.endingBalance());
    accountActivityRepository
        .find(
            businessId,
            new AccountActivityFilterCriteria(
                businessId,
                BUSINESS_STATEMENT_TYPES,
                BUSINESS_STATEMENT_STATES,
                startDate,
                endDate,
                new PageToken(0, Integer.MAX_VALUE, List.of())))
        .stream()
        .map(LedgerActivityResponse::of)
        .filter(
            activity ->
                activity.getType() != AccountActivityType.BANK_DEPOSIT_STRIPE
                    || activity.getStatus() != AccountActivityStatus.PENDING)
        .forEach(
            ledgerActivity -> {
              table.addCell(
                  new PDFCellActivitiesLeft(
                      dateFormatter.format(ledgerActivity.getActivityTime()), fontNormal11));
              table.addCell(
                  new PDFCellActivitiesLeft(
                      buildAccountParagraph(ledgerActivity.getAccount(), fontNormal9)));
              table.addCell(
                  new PDFCellActivitiesLeft(
                      buildTransactionTypeParagraph(ledgerActivity, fontNormal9)));
              table.addCell(
                  new PDFCellActivitiesLeft(
                      buildAccountParagraph(ledgerActivity.getReferenceAccount(), fontNormal9)));
              table.addCell(
                  new PDFCellActivitiesLeft(
                      buildEmployeeParagraph(ledgerActivity.getUser(), fontNormal9)));
              table.addCell(
                  new PdfCellActivitiesRight(
                      "$" + String.format("%,.2f", ledgerActivity.getAmount().getAmount()),
                      fontNormal11));
              table.addCell(
                  new PdfCellActivitiesRight(
                      "$" + String.format("%,.2f", operationLedgerBalance.get()), fontNormal11));

              if (BUSINESS_BALANCE_CHANGE_STATES.contains(ledgerActivity.getStatus())) {
                operationLedgerBalance.accumulateAndGet(
                    ledgerActivity.getAmount().getAmount(), BigDecimal::subtract);
              }
            });
    // ledger balance sanity check
    // FIXME: Restore when CAP-1222 is done
    /*
        if (operationLedgerBalance.get().compareTo(ledgerBalancePeriod.endingBalance()) != 0) {
          throw new RuntimeException(
              "DB ledger balance %s doesn't match statement calculated ledger balance %s for period %s %s, businessId: %s"
                  .formatted(
                      ledgerBalancePeriod.endingBalance(),
                      operationLedgerBalance.get(),
                      startDate,
                      endDate,
                      businessId));
        }
    */

    // empty cells before total
    PDFCellActivitiesLeft emptyCell = new PDFCellActivitiesLeft(emptyParagraph);
    emptyCell.setBackgroundColor(new Color(230, 230, 230));
    for (int i = 0; i < table.getNumberOfColumns() - 2; i++) {
      table.addCell(emptyCell);
    }

    // total columns
    PdfCellActivitiesRight totalAmountCell =
        new PdfCellActivitiesRight(
            "$" + String.format("%,.2f", ledgerPeriodDelta),
            fontNormal12,
            new Color(230, 230, 230));
    PdfCellActivitiesRight totalBalanceCell =
        new PdfCellActivitiesRight(
            "$" + String.format("%,.2f", ledgerBalancePeriod.endingBalance()),
            fontNormal12,
            new Color(230, 230, 230));

    table.addCell(totalAmountCell);
    table.addCell(totalBalanceCell);

    document.add(table);

    // End of tables

    document.close();

    String fileName =
        "statement_"
            + dateFormatterFileName.format(startDate)
            + "_"
            + dateFormatterFileName.format(endDate)
            + ".pdf";

    return new StatementRecord(fileName, pdfStream.toByteArray());
  }

  @PreAuthorize(
      "hasAllocationPermission(#card.allocation().getId(), 'READ') or hasGlobalPermission('GLOBAL_READ|CUSTOMER_SERVICE')")
  public StatementRecord generateCardStatementPdf(
      final CardStatementRequest request, final CardDetailsRecord card) {

    CardStatementData generalStatementData =
        accountActivityRepository.findDataForCardStatement(
            CurrentUser.getBusinessId(),
            new CardStatementFilterCriteria(
                request.getCardId(), request.getStartDate(), request.getEndDate()));

    Document document = new Document(PageSize.A4);
    ByteArrayOutputStream pdfStream = new ByteArrayOutputStream();
    PdfWriter writer = PdfWriter.getInstance(document, pdfStream);
    document.open();

    // Draw background pattern, which consists of a thick angular line, a straight thick line
    // and a circle which creates effect of the curly edge
    // Use this value to adjust the background pattern's vertical position
    float deltaY = 30;
    PdfContentByte directContent = writer.getDirectContent();

    directContent.setLineWidth(500);
    directContent.setRGBColorStroke(230, 230, 230);
    directContent.moveTo(document.left() - 100, 650 + deltaY);
    directContent.lineTo(document.right() - 48, 753 + deltaY);
    directContent.stroke();
    directContent.moveTo(document.left(), 802 + deltaY);
    directContent.lineTo(document.right() + 100, 802 + deltaY);
    directContent.stroke();

    directContent.setLineWidth(50f);
    directContent.circle(document.right() - 14, 556 + deltaY, 25f);
    directContent.stroke();

    // For visual spacing
    Paragraph emptyParagraph = new Paragraph(" ");

    // Table for first logo image
    PdfPTable headerLogo = new PdfPTable(2);
    headerLogo.setWidthPercentage(100);
    headerLogo.setWidths(new int[] {2, 8});
    PdfPCell cell = new PdfPCell(logoImage, true);
    cell.setBorder(0);
    cell.setPaddingLeft(4);
    headerLogo.addCell(cell);
    headerLogo.addCell(new PdfPCellLeft(emptyParagraph));
    document.add(headerLogo);

    // Table for Monthly Statement text
    PdfPTable headerTable = new PdfPTable(1);
    headerTable.setWidthPercentage(100);
    headerTable.addCell(new PdfPCellLeft("Monthly Statement", fontNormal40));
    headerTable.addCell(new PdfPCellLeft(emptyParagraph));
    document.add(headerTable);

    // Table for other header details
    PdfPTable midTable = new PdfPTable(2);
    midTable.setWidthPercentage(100);

    // Give it enough spacing
    midTable.addCell(new PdfPCellLeft(emptyParagraph));
    midTable.addCell(new PdfPCellLeft(emptyParagraph));
    midTable.addCell(new PdfPCellLeft(emptyParagraph));
    midTable.addCell(new PdfPCellLeft(emptyParagraph));
    midTable.addCell(new PdfPCellLeft(emptyParagraph));
    midTable.addCell(new PdfPCellLeft(emptyParagraph));

    // Column
    midTable.addCell(new PdfPCellLeft("Total amount spent this period:", fontNormal9));

    // Column
    Paragraph statementParagraph = new Paragraph();
    statementParagraph.add(new Chunk("VISA ", fontBoldItalic9));
    statementParagraph.add(
        new Chunk(
            "Statement "
                + dateFormatter.format(request.getStartDate())
                + " - "
                + dateFormatter.format(request.getEndDate()),
            fontNormal9));
    midTable.addCell(new PdfPCellRight(statementParagraph));

    // Column
    midTable.addCell(
        new PdfPCellLeft(
            "$" + String.format("%,.2f", generalStatementData.getTotalAmount()), fontBold32));

    // Column
    User user = userService.retrieveUserForService(card.card().getUserId());
    Paragraph paragraph = new Paragraph();
    paragraph.add(new Chunk("Cardholder: ", fontBold8));
    paragraph.add(new Chunk(user.getFirstName() + " " + user.getLastName() + "\n\n", fontNormal8));

    paragraph.add(new Chunk("Card number: **** ", fontBold8));
    paragraph.add(new Chunk(card.card().getLastFour() + "\n\n", fontNormal8));

    paragraph.add(new Chunk("Allocation: ", fontBold8));
    paragraph.add(new Chunk(card.allocation().getName() + "\n\n", fontNormal8));

    midTable.addCell(new PdfPCellRight(paragraph));

    // Empty line
    midTable.addCell(new PdfPCellLeft(emptyParagraph));
    midTable.addCell(new PdfPCellLeft(emptyParagraph));

    // Column
    midTable.addCell(
        new PdfPCellLeft(
            "Thank you for using ClearSpend.\n\nFor details and upcoming payments, log into your ClearSpend account",
            fontNormal8));

    midTable.addCell(new PdfPCellLeft(emptyParagraph));

    document.add(midTable);

    // Table for 'Transactions' label
    PdfPTable tableLabel = new PdfPTable(1);
    tableLabel.setWidthPercentage(100);

    for (int i = 0; i < 6; i++) {
      tableLabel.addCell(new PdfPCellLeft(emptyParagraph));
    }

    tableLabel.addCell(new PdfPCellLeft("Transactions", fontBold24));

    tableLabel.addCell(new PdfPCellLeft(emptyParagraph));
    tableLabel.addCell(new PdfPCellLeft(emptyParagraph));
    document.add(tableLabel);

    // Main statement table
    PdfPTable table = new PdfPTable(3);
    table.setWidthPercentage(100);

    table.addCell(new PdfPCellLeft("DATE", fontBold10Gray));
    table.addCell(new PdfPCellLeft("Merchant", fontBold10Gray));
    table.addCell(new PdfPCellRight("AMOUNT", fontBold10Gray));

    // Display activities in the table
    generalStatementData
        .getActivities()
        .forEach(
            row -> {
              table.addCell(
                  new PDFCellActivitiesLeft(
                      dateFormatter.format(row.getActivityDate()), fontNormal12));

              table.addCell(new PDFCellActivitiesLeft(row.getDescription(), fontNormal12));

              table.addCell(
                  new PdfCellActivitiesRight(
                      "$" + String.format("%,.2f", row.getAmount()), fontNormal12));
            });

    StatementService.PDFCellActivitiesLeft dummy = new PDFCellActivitiesLeft(emptyParagraph);
    dummy.setBackgroundColor(new Color(230, 230, 230));
    table.addCell(dummy);
    table.addCell(dummy);

    // Total column
    StatementService.PdfCellActivitiesRight totalCol =
        new PdfCellActivitiesRight(
            "$" + String.format("%,.2f", generalStatementData.getTotalAmount()), fontNormal12);
    totalCol.setBackgroundColor(new Color(230, 230, 230));
    table.addCell(totalCol);

    document.add(table);

    // End of tables

    document.close();

    String fileName =
        "statement_"
            + dateFormatterFileName.format(request.getStartDate())
            + "_"
            + dateFormatterFileName.format(request.getEndDate())
            + ".pdf";

    return new StatementRecord(fileName, pdfStream.toByteArray());
  }

  private Paragraph buildAccountParagraph(LedgerAccount ledgerAccount, Font font) {
    String paragraphText;

    if (ledgerAccount instanceof LedgerCardAccount cardAccount) {
      paragraphText = "**** %s".formatted(cardAccount.getCardInfo().getLastFour());
    } else if (ledgerAccount instanceof LedgerMerchantAccount merchantAccount) {
      paragraphText = merchantAccount.getMerchantInfo().getName();
    } else if (ledgerAccount instanceof LedgerAllocationAccount allocationAccount) {
      paragraphText = allocationAccount.getAllocationInfo().getName();
    } else if (ledgerAccount instanceof LedgerBankAccount bankAccount) {
      paragraphText =
          "%s\n**** %s"
              .formatted(
                  bankAccount.getBankInfo().getName(),
                  bankAccount.getBankInfo().getAccountNumberLastFour());
    } else {
      throw new RuntimeException(
          "Unexpected ledger account type found: " + ledgerAccount.getClass().getSimpleName());
    }

    return new Paragraph(new Chunk(paragraphText, font));
  }

  private Paragraph buildTransactionTypeParagraph(
      LedgerActivityResponse ledgerActivityResponse, Font font) {
    String transactionType =
        switch (ledgerActivityResponse.getType()) {
          case BANK_DEPOSIT_STRIPE, BANK_DEPOSIT_ACH, BANK_DEPOSIT_WIRE -> "Transfer In";
          case BANK_DEPOSIT_RETURN -> "Transfer In Return";
          case BANK_WITHDRAWAL -> "Transfer Out";
          case BANK_WITHDRAWAL_RETURN -> "Transfer Out Return";
          case REALLOCATE -> ledgerActivityResponse.getAmount().isGreaterThanZero()
              ? "Transfer In"
              : "Transfer Out";
          case FEE -> "Fee";
          case MANUAL -> "Manual";
          case NETWORK_REFUND, CARD_FUND_RETURN -> "Refund";
          case NETWORK_AUTHORIZATION -> "Card Payment Hold";
          case NETWORK_CAPTURE -> "Card Payment";
        };

    String paragraphText;
    if (ledgerActivityResponse.getAccount() instanceof LedgerMerchantAccount merchantAccount) {
      String mccGroup =
          WordUtils.capitalizeFully(
              merchantAccount.getMerchantInfo().getMerchantCategoryGroup().name());
      paragraphText = "%s\n%s".formatted(transactionType, mccGroup);
    } else {
      paragraphText = "%s".formatted(transactionType);
    }

    return new Paragraph(new Chunk(paragraphText, font));
  }

  private Paragraph buildEmployeeParagraph(LedgerUser ledgerUser, Font font) {
    String paragraphText;

    UserInfo userInfo = ledgerUser.getUserInfo();
    if (userInfo != null) {
      paragraphText = "%s %s".formatted(userInfo.getFirstName(), userInfo.getLastName());
    } else {
      paragraphText = StringUtils.EMPTY;
    }

    return new Paragraph(new Chunk(paragraphText, font));
  }
}
