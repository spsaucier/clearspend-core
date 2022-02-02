package com.clearspend.capital.controller.type.review;

public enum KybDocuments {
  CERTIFICATE_OF_INCORPORATION("Certificate Of Incorporation", DocumentType.ACCOUNT_REQUIREMENT),
  BANK_STATEMENT("Bank Statement", DocumentType.ACCOUNT_REQUIREMENT),
  UTILITY_BILL("Utility Bill", DocumentType.ACCOUNT_REQUIREMENT),
  TAX_RETURN("Tax Return", DocumentType.ACCOUNT_REQUIREMENT),
  PROCESSED_SS4("Social Security Card", DocumentType.ACCOUNT_REQUIREMENT),
  MANUAL_THIRD_PARTY_REVIEW(null, DocumentType.ACCOUNT_REQUIREMENT),
  UNEXPIRED_GOVERNMENT_ISSUED_ID(
      "Unexpired Government Issued Id", DocumentType.ACCOUNT_REQUIREMENT);

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
