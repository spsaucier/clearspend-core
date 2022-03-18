package com.clearspend.capital.service;

import com.clearspend.capital.controller.type.activity.CardStatementRequest;
import com.clearspend.capital.data.model.Account;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.data.repository.CardRepositoryCustom.CardDetailsRecord;
import com.clearspend.capital.service.type.CardStatementData;
import com.clearspend.capital.service.type.CardStatementFilterCriteria;
import com.clearspend.capital.service.type.CurrentUser;
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
import java.time.format.DateTimeFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardStatementService {

  @Value("classpath:reports/logo_300px.png")
  Resource logoResource;

  private final AccountService accountService;
  private final CardService cardService;
  private final UserService userService;
  private final AccountActivityRepository accountActivityRepository;

  public record CardStatementRecord(String fileName, byte[] pdf) {}

  private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
  private static final DateTimeFormatter dateFormatterFileName =
      DateTimeFormatter.ofPattern("MM_dd_yyyy");

  private static class PdfPCellLeft extends PdfPCell {

    public PdfPCellLeft(Paragraph paragraph) {
      super(paragraph);
      this.setBorder(0);
      this.setPaddingBottom(5);
    }
  }

  private static class PDFCellActivitiesLeft extends PdfPCell {

    public PDFCellActivitiesLeft(Paragraph paragraph) {
      super(paragraph);
      this.setBorderColor(Color.white);
      this.setBorderColorTop(new Color(240, 240, 240));
      this.setBorderWidth(0);
      this.setBorderWidthTop(1);
      this.setPaddingBottom(10);
      this.setPaddingTop(5);
    }
  }

  private static class PdfCellActivitiesRight extends PdfPCell {

    public PdfCellActivitiesRight(Paragraph paragraph) {
      super(paragraph);
      this.setHorizontalAlignment(Element.ALIGN_RIGHT);
      this.setBorderColor(Color.white);
      this.setBorderColorTop(new Color(230, 230, 230));
      this.setBorderWidth(0);
      this.setBorderWidthTop(1);
      this.setPaddingBottom(10);
      this.setPaddingTop(5);
    }
  }

  private static class PdfPCellRight extends PdfPCell {

    public PdfPCellRight(Paragraph paragraph) {
      super(paragraph);
      this.setBorder(0);
      this.setHorizontalAlignment(Element.ALIGN_RIGHT);
      this.setPaddingBottom(5);
    }
  }

  @PreAuthorize(
      "hasAllocationPermission(#card.allocation().getId(), 'READ') or hasGlobalPermission('GLOBAL_READ')")
  public CardStatementRecord generatePdf(
      final CardStatementRequest request, final CardDetailsRecord card) throws IOException {

    CardStatementData generalStatementData =
        accountActivityRepository.findDataForCardStatement(
            CurrentUser.get().businessId(),
            new CardStatementFilterCriteria(
                request.getCardId(), request.getStartDate(), request.getEndDate()));

    Document document = new Document(PageSize.A4);
    ByteArrayOutputStream pdfStream = new ByteArrayOutputStream();
    PdfWriter writer = PdfWriter.getInstance(document, pdfStream);
    document.open();

    // Draw background pattern, which consists of a thick angular line, a straight thick line
    // and a circle which creates effect of the curly edge
    // Use this value to adjust the background pattern's vertical position
    float deltaY = -20;
    PdfContentByte directContent = writer.getDirectContent();

    directContent.setLineWidth(500);
    directContent.setRGBColorStroke(15, 50, 51);
    directContent.moveTo(document.left() - 100, 650 + deltaY);
    directContent.lineTo(document.right() - 48, 753 + deltaY);
    directContent.stroke();
    directContent.moveTo(document.left(), 802 + deltaY);
    directContent.lineTo(document.right() + 100, 802 + deltaY);
    directContent.stroke();

    directContent.setLineWidth(50f);
    directContent.circle(document.right() - 14, 556 + deltaY, 25f);
    directContent.stroke();

    // Create base font which will be used for all fonts
    BaseFont defaultBaseFont = BaseFont.createFont(BaseFont.HELVETICA, "UTF8", BaseFont.EMBEDDED);

    // For visual spacing
    Paragraph emptyParagraph = new Paragraph(" ");

    // Table for first logo image
    PdfPTable headerLogo = new PdfPTable(2);
    headerLogo.setWidthPercentage(100);
    headerLogo.setWidths(new int[] {2, 8});
    PdfPCell cell =
        new PdfPCell(Image.getInstance(IOUtils.toByteArray(logoResource.getInputStream())), true);
    cell.setBorder(0);
    cell.setPaddingLeft(4);
    headerLogo.addCell(cell);
    headerLogo.addCell(new PdfPCellLeft(emptyParagraph));
    document.add(headerLogo);

    // Table for Monthly Statement text
    PdfPTable headerTable = new PdfPTable(1);
    headerTable.setWidthPercentage(100);
    Font fontNormal40White = new Font(defaultBaseFont, 40, Font.NORMAL);
    fontNormal40White.setColor(Color.white);
    headerTable.addCell(
        new PdfPCellLeft(new Paragraph(new Chunk("Monthly Statement", fontNormal40White))));
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

    Font fontBoldItalic9White = new Font(defaultBaseFont, 9, Font.BOLDITALIC);
    fontBoldItalic9White.setColor(Color.white);
    Font fontNormal9White = new Font(defaultBaseFont, 9, Font.NORMAL);
    fontNormal9White.setColor(Color.white);

    // Column
    midTable.addCell(
        new PdfPCellLeft(
            new Paragraph(new Chunk("Total amount spent this period:", fontNormal9White))));

    // Column
    Paragraph statementParagraph = new Paragraph();
    statementParagraph.add(new Chunk("VISA ", fontBoldItalic9White));
    statementParagraph.add(
        new Chunk(
            "Statement "
                + dateFormatter.format(request.getStartDate())
                + " - "
                + dateFormatter.format(request.getEndDate()),
            fontNormal9White));
    midTable.addCell(new PdfPCellRight(statementParagraph));

    // Column
    Font fontBold32Green = new Font(defaultBaseFont, 32, Font.BOLD);
    fontBold32Green.setColor(125, 245, 133);
    midTable.addCell(
        new PdfPCellLeft(
            new Paragraph(
                new Chunk(
                    "$" + String.format("%,.2f", generalStatementData.getTotalAmount()),
                    fontBold32Green))));

    // Column
    Account account = accountService.retrieveAccountById(card.account().getId(), true);

    Font fontNormal8White = new Font(defaultBaseFont, 8, Font.NORMAL);
    fontNormal8White.setColor(Color.white);
    Font fontBold8White = new Font(defaultBaseFont, 8, Font.BOLD);
    fontBold8White.setColor(Color.white);

    User user = userService.retrieveUser(card.card().getUserId());
    Paragraph paragraph = new Paragraph();
    paragraph.add(new Chunk("Cardholder: ", fontBold8White));
    paragraph.add(
        new Chunk(user.getFirstName() + " " + user.getLastName() + "\n\n", fontNormal8White));

    paragraph.add(new Chunk("Card number: **** ", fontBold8White));
    paragraph.add(new Chunk(card.card().getLastFour() + "\n\n", fontNormal8White));

    paragraph.add(new Chunk("Allocation: ", fontBold8White));
    paragraph.add(new Chunk(card.allocation().getName() + "\n\n", fontNormal8White));

    midTable.addCell(new PdfPCellRight(paragraph));

    // Column
    midTable.addCell(
        new PdfPCellLeft(
            new Paragraph(
                new Chunk(
                    "Available to spend as of " + dateFormatter.format(request.getEndDate()) + ": ",
                    fontNormal9White))));

    // Column
    midTable.addCell(new PdfPCellLeft(emptyParagraph));

    // Column
    Font fontBold16 = new Font(defaultBaseFont, 16, Font.BOLD);
    fontBold16.setColor(Color.white);
    midTable.addCell(
        new PdfPCellLeft(
            new Paragraph(
                new Chunk(
                    "$" + String.format("%,.2f", account.getAvailableBalance().getAmount()),
                    fontBold16))));

    // Column
    midTable.addCell(new PdfPCellLeft(emptyParagraph));

    // Empty line
    midTable.addCell(new PdfPCellLeft(emptyParagraph));
    midTable.addCell(new PdfPCellLeft(emptyParagraph));

    // Column
    midTable.addCell(
        new PdfPCellLeft(
            new Paragraph(
                new Chunk(
                    "Thank you for using ClearSpend. For details and upcoming payments, \n\n log into your ClearSpend account",
                    fontNormal8White))));

    midTable.addCell(new PdfPCellLeft(emptyParagraph));

    document.add(midTable);

    // Table for 'Transactions' label
    PdfPTable tableLabel = new PdfPTable(1);
    tableLabel.setWidthPercentage(100);

    for (int i = 0; i < 6; i++) {
      tableLabel.addCell(new PdfPCellLeft(emptyParagraph));
    }

    Font fontBold24Black = new Font(defaultBaseFont, 24, Font.BOLD);
    fontBold24Black.setColor(Color.black);
    tableLabel.addCell(new PdfPCellLeft(new Paragraph(new Chunk("Transactions", fontBold24Black))));

    tableLabel.addCell(new PdfPCellLeft(emptyParagraph));
    tableLabel.addCell(new PdfPCellLeft(emptyParagraph));
    document.add(tableLabel);

    // Main statement table
    PdfPTable table = new PdfPTable(3);
    table.setWidthPercentage(100);

    Font fontBold10Gray = new Font(defaultBaseFont, 10, Font.BOLD);
    fontBold10Gray.setColor(Color.gray);

    table.addCell(new PdfPCellLeft(new Paragraph(new Chunk("DATE", fontBold10Gray))));
    table.addCell(new PdfPCellLeft(new Paragraph(new Chunk("Merchant", fontBold10Gray))));
    table.addCell(new PdfPCellRight(new Paragraph(new Chunk("AMOUNT", fontBold10Gray))));

    Font fontNormal12 = new Font(defaultBaseFont, 12, Font.NORMAL);
    fontNormal12.setColor(Color.black);

    // Display activities in the table
    generalStatementData
        .getActivities()
        .forEach(
            row -> {
              table.addCell(
                  new PDFCellActivitiesLeft(
                      new Paragraph(
                          new Chunk(dateFormatter.format(row.getActivityDate()), fontNormal12))));

              table.addCell(
                  new PDFCellActivitiesLeft(
                      new Paragraph(new Chunk(row.getDescription(), fontNormal12))));

              table.addCell(
                  new PdfCellActivitiesRight(
                      new Paragraph(
                          new Chunk("$" + String.format("%,.2f", row.getAmount()), fontNormal12))));
            });

    CardStatementService.PDFCellActivitiesLeft dummy = new PDFCellActivitiesLeft(emptyParagraph);
    dummy.setBackgroundColor(new Color(230, 230, 230));
    table.addCell(dummy);
    table.addCell(dummy);

    // Total column
    CardStatementService.PdfCellActivitiesRight totalCol =
        new PdfCellActivitiesRight(
            new Paragraph(
                new Chunk(
                    "$" + String.format("%,.2f", generalStatementData.getTotalAmount()),
                    fontNormal12)));
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

    return new CardStatementRecord(fileName, pdfStream.toByteArray());
  }
}
