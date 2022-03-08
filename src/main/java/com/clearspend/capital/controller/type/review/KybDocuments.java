package com.clearspend.capital.controller.type.review;

public enum KybDocuments {
  CERTIFICATE_OF_INCORPORATION("Certificate Of Incorporation", DocumentType.IDENTITY_DOCUMENT),
  BANK_STATEMENT("Bank Statement", DocumentType.IDENTITY_DOCUMENT),
  UTILITY_BILL("Utility Bill", DocumentType.IDENTITY_DOCUMENT),
  TAX_RETURN("Tax Return", DocumentType.IDENTITY_DOCUMENT),
  PROCESSED_SS4("Social Security Card", DocumentType.IDENTITY_DOCUMENT),
  IRS_LETTER_147C("IRS Letter 147C document", DocumentType.IDENTITY_DOCUMENT),
  IRS_SS4_CONFIRMATION_LETTER("IRS SS-4 confirmation letter", DocumentType.IDENTITY_DOCUMENT);

  DocumentType documentType;
  String documentName;

  KybDocuments(String documentName, DocumentType documentType) {
    this.documentName = documentName;
    this.documentType = documentType;
  }

  public DocumentType getDocumentType() {
    return documentType;
  }

  public String getDocumentName() {
    return documentName;
  }
}
