package com.clearspend.capital.controller.type.review;

public enum KycDocuments {
  IDENTITY_DOCUMENT_FRONT(
      "Government issued documents (national IDs, driver’s licenses or passports) - Front",
      DocumentType.IDENTITY_DOCUMENT_FRONT),
  IDENTITY_DOCUMENT_BACK(
      "Government issued documents (national IDs, driver’s licenses or passports) - Back",
      DocumentType.IDENTITY_DOCUMENT_BACK);

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
