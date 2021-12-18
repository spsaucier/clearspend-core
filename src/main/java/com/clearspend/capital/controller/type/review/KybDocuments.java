package com.clearspend.capital.controller.type.review;

import com.clearspend.capital.client.alloy.request.DocumentType;

public enum KybDocuments {
  CERTIFICATE_OF_INCORPORATION("Certificate Of Incorporation", DocumentType.contract),
  BANK_STATEMENT("Bank Statement", DocumentType.contract),
  UTILITY_BILL("Utility Bill", DocumentType.utility),
  TAX_RETURN("Tax Return", DocumentType.utility),
  PROCESSED_SS4(null, null),
  MANUAL_THIRD_PARTY_REVIEW(null, null),
  UNEXPIRED_GOVERNMENT_ISSUED_ID("Unexpired Government Issued Id", DocumentType.passport);

  DocumentType documentType;

  String documentName;

  KybDocuments(String documentName, DocumentType documentType) {
    this.documentType = documentType;
  }

  public DocumentType getDocumentType() {
    return documentType;
  }

  public String getDocumentName() {
    return documentName;
  }
}
