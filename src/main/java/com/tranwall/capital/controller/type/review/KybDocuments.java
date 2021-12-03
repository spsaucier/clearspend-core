package com.tranwall.capital.controller.type.review;

import com.tranwall.capital.client.alloy.request.DocumentType;

public enum KybDocuments {
  CERTIFICATE_OF_INCORPORATION(DocumentType.contract),
  BANK_STATEMENT(DocumentType.contract),
  UTILITY_BILL(DocumentType.utility),
  TAX_RETURN(DocumentType.utility),
  PROCESSED_SS4(null),
  MANUAL_THIRD_PARTY_REVIEW(null),
  UNEXPIRED_GOVERNMENT_ISSUED_ID(DocumentType.passport);

  DocumentType documentType;

  KybDocuments(DocumentType documentType) {
    this.documentType = documentType;
  }

  public DocumentType getDocumentType() {
    return documentType;
  }
}
