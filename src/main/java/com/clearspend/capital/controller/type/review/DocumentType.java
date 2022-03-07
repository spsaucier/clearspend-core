package com.clearspend.capital.controller.type.review;

public enum DocumentType {
  ACCOUNT_REQUIREMENT("account_requirement"),
  ADDITIONAL_VERIFICATION("additional_verification"),
  BUSINESS_ICON("business_icon"),
  BUSINESS_LOGO("business_logo"),
  CUSTOMER_SIGNATURE("customer_signature"),
  DISPUTE_EVIDENCE("dispute_evidence"),
  IDENTITY_DOCUMENT("identity_document"),
  IDENTITY_DOCUMENT_FRONT("identity_document_front"),
  IDENTITY_DOCUMENT_BACK("identity_document_back"),
  PCI_DOCUMENT("pci_document"),
  TAX_DOCUMENT_USER_UPLOAD("tax_document_user_upload");

  private final String name;

  DocumentType(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }
}
