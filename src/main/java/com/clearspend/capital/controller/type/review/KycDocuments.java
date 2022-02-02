package com.clearspend.capital.controller.type.review;

public enum KycDocuments {
  CERTIFICATE_OF_INCORPORATION(
      "Certificate Of Incorporation", DocumentType.ADDITIONAL_VERIFICATION),
  BANK_STATEMENT("Bank Statement", DocumentType.PCI_DOCUMENT),
  UTILITY_BILL("Utility Bill", DocumentType.ADDITIONAL_VERIFICATION),
  TAX_RETURN("Tax Return", DocumentType.TAX_DOCUMENT_USER_UPLOAD),
  PROCESSED_SS4("Processed SS4", DocumentType.TAX_DOCUMENT_USER_UPLOAD),
  MANUAL_THIRD_PARTY_REVIEW(null, null),
  UNEXPIRED_GOVERNMENT_ISSUED_ID("Unexpired Government Issued ID", DocumentType.IDENTITY_DOCUMENT);

  DocumentType documentType;
  String documentName;

  KycDocuments(String documentName, DocumentType documentType) {
    this.documentType = documentType;
    this.documentName = documentName;
  }

  public DocumentType getDocumentType() {
    return documentType;
  }

  public String getDocumentName() {
    return documentName;
  }
}
