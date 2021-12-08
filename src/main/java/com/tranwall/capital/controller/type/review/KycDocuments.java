package com.tranwall.capital.controller.type.review;

import com.tranwall.capital.client.alloy.request.DocumentType;

public enum KycDocuments {
  CERTIFICATE_OF_INCORPORATION("Certificate Of Incorporation", DocumentType.contract),
  BANK_STATEMENT("Bank Statement", DocumentType.contract),
  UTILITY_BILL("Utility Bill", DocumentType.utility),
  TAX_RETURN("Tax Return", DocumentType.contract),
  PROCESSED_SS4(null, null),
  MANUAL_THIRD_PARTY_REVIEW(null, null),
  UNEXPIRED_GOVERNMENT_ISSUED_ID("Unexpired Government Issued ID", DocumentType.passport);

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
