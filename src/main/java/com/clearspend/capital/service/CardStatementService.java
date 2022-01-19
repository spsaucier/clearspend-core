package com.clearspend.capital.service;

import com.clearspend.capital.controller.type.CurrentUser;
import com.clearspend.capital.controller.type.activity.CardStatementRequest;
import com.clearspend.capital.data.repository.AccountActivityRepository;
import com.clearspend.capital.data.repository.CardRepositoryCustom.CardDetailsRecord;
import com.clearspend.capital.service.type.CardStatementData;
import com.clearspend.capital.service.type.CardStatementFilterCriteria;
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
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
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardStatementService {

  @Value("classpath:reports/logo.png")
  Resource logoResource;

  private final BusinessService businessService;
  private final CardService cardService;
  private final AccountActivityRepository accountActivityRepository;

  public record CardStatementRecord(String fileName, byte[] pdf) {}

  private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");
  private static final DateTimeFormatter dateFormatterFileName =
      DateTimeFormatter.ofPattern("MM_dd_yyyy");

  private class PdfPCellLeft extends PdfPCell {
    public PdfPCellLeft(Paragraph paragraph) {
      super(paragraph);
      this.setBorder(0);
      this.setPaddingBottom(5);
    }
  }

  private class PDFCellActivitiesLeft extends PdfPCell {
    public PDFCellActivitiesLeft(Paragraph paragraph) {
      super(paragraph);
      this.setBorderColor(Color.white);
      this.setBorderColorTop(Color.lightGray);
      this.setBorderWidth(0);
      this.setBorderWidthTop(1);
      this.setPaddingBottom(5);
    }
  }

  private class PdfCellActivitiesRight extends PdfPCell {
    public PdfCellActivitiesRight(Paragraph paragraph) {
      super(paragraph);
      this.setHorizontalAlignment(Element.ALIGN_RIGHT);
      this.setBorderColor(Color.white);
      this.setBorderColorTop(Color.lightGray);
      this.setBorderWidth(0);
      this.setBorderWidthTop(1);
      this.setPaddingBottom(5);
    }
  }

  private class PdfPCellRight extends PdfPCell {
    public PdfPCellRight(Paragraph paragraph) {
      super(paragraph);
      this.setBorder(0);
      this.setHorizontalAlignment(Element.ALIGN_RIGHT);
      this.setPaddingBottom(5);
    }

    public PdfPCellRight(Image image) {
      super(image);
      this.setBorder(0);
      this.setHorizontalAlignment(Element.ALIGN_RIGHT);
      this.setPaddingBottom(5);
    }
  }

  public CardStatementRecord generatePdf(CardStatementRequest request) throws IOException {

    CardStatementData generalStatementData =
        accountActivityRepository.findDataForCardStatement(
            CurrentUser.get().businessId(),
            new CardStatementFilterCriteria(
                request.getCardId(), request.getStartDate(), request.getEndDate()));

    Document document = new Document(PageSize.A4);
    ByteArrayOutputStream pdfStream = new ByteArrayOutputStream();
    PdfWriter.getInstance(document, pdfStream);
    document.open();

    // Create fonts which will be used for all parts of this document
    BaseFont defaultBaseFont = BaseFont.createFont(BaseFont.HELVETICA, "UTF8", BaseFont.EMBEDDED);

    Font fontBold16 = new Font(defaultBaseFont, 16, Font.BOLD);
    Font fontNormal14 = new Font(defaultBaseFont, 14, Font.NORMAL);

    Font fontNormal12Gray = new Font(defaultBaseFont, 12, Font.NORMAL);
    fontNormal12Gray.setColor(Color.gray);

    Font fontBold16Gray = new Font(defaultBaseFont, 16, Font.BOLD);
    fontBold16Gray.setColor(Color.gray);

    // For visual spacing
    Paragraph emptyLine = new Paragraph(" ");

    // Header as a table of 2 columns - left text and right logo image
    PdfPTable headerTable = new PdfPTable(2);
    headerTable.setWidthPercentage(100);

    // Left part of the header
    Chunk legalName =
        new Chunk(
            businessService.retrieveBusiness(CurrentUser.get().businessId()).getLegalName(),
            fontBold16);
    Chunk statementFor =
        new Chunk(
            "\n\n"
                + dateFormatter.format(request.getStartDate())
                + " - "
                + dateFormatter.format(request.getEndDate()),
            fontNormal14);

    CardDetailsRecord card =
        cardService.getCard(CurrentUser.get().businessId(), request.getCardId());
    Chunk lastFour = new Chunk("\n\ncard ending in " + card.card().getLastFour(), fontNormal12Gray);

    Paragraph leftHeaderCellParagraph = new Paragraph();
    leftHeaderCellParagraph.add(legalName);
    leftHeaderCellParagraph.add(statementFor);
    leftHeaderCellParagraph.add(lastFour);
    PdfPCellLeft leftHeaderCell = new PdfPCellLeft(leftHeaderCellParagraph);
    headerTable.addCell(leftHeaderCell);

    // Right part of the header, contains only logo image
    PdfPCellRight rightHeaderCell =
        new PdfPCellRight(Image.getInstance(IOUtils.toByteArray(logoResource.getInputStream())));
    headerTable.addCell(rightHeaderCell);

    document.add(headerTable);

    document.add(emptyLine);

    // Add total amount
    document.add(
        new Paragraph(
            new Chunk(
                "Total: " + String.format("%.2f", generalStatementData.getTotalAmount()),
                fontBold16)));

    document.add(emptyLine);

    // Main statement table
    PdfPTable table = new PdfPTable(3);
    table.setWidthPercentage(100);

    table.addCell(new PdfPCellLeft(new Paragraph(new Chunk("DATE", fontNormal12Gray))));
    table.addCell(new PdfPCellLeft(new Paragraph(new Chunk("DESCRIPTION", fontNormal12Gray))));
    table.addCell(new PdfPCellRight(new Paragraph(new Chunk("AMOUNT", fontNormal12Gray))));

    // Display activities in the table
    generalStatementData
        .getActivities()
        .forEach(
            row -> {
              table.addCell(
                  new PDFCellActivitiesLeft(
                      new Paragraph(
                          new Chunk(dateFormatter.format(row.getActivityDate()), fontNormal14))));

              table.addCell(
                  new PDFCellActivitiesLeft(
                      new Paragraph(new Chunk(row.getDescription(), fontNormal14))));

              table.addCell(
                  new PdfCellActivitiesRight(
                      new Paragraph(
                          new Chunk(String.format("%.2f", row.getAmount()), fontNormal14))));
            });

    document.add(table);

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
