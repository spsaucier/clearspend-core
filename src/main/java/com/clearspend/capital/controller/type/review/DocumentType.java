package com.clearspend.capital.controller.type.review;

import com.clearspend.capital.common.error.InvalidKycDataException;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public enum DocumentType {
  ACCOUNT_REQUIREMENT(
      "account_requirement",
      List.of("application/pdf", "image/jpeg", "image/png"),
      16 * 1024 * 1024),
  ADDITIONAL_VERIFICATION(
      "additional_verification",
      List.of("application/pdf", "image/jpeg", "image/png"),
      16 * 1024 * 1024),
  BUSINESS_ICON(
      "business_icon", List.of("application/pdf", "image/jpeg", "image/png"), 16 * 1024 * 1024),
  BUSINESS_LOGO(
      "business_logo", List.of("application/pdf", "image/jpeg", "image/png"), 16 * 1024 * 1024),
  CUSTOMER_SIGNATURE("customer_signature", List.of("image/jpeg", "image/png"), 4 * 1024 * 1024),
  DISPUTE_EVIDENCE(
      "dispute_evidence", List.of("application/pdf", "image/jpeg", "image/png"), 5 * 1024 * 1024),
  IDENTITY_DOCUMENT(
      "identity_document", List.of("application/pdf", "image/jpeg", "image/png"), 16 * 1024 * 1024),
  IDENTITY_DOCUMENT_FRONT(
      "identity_document_front",
      List.of("application/pdf", "image/jpeg", "image/png"),
      16 * 1024 * 1024),
  IDENTITY_DOCUMENT_BACK(
      "identity_document_back",
      List.of("application/pdf", "image/jpeg", "image/png"),
      16 * 1024 * 1024),
  PCI_DOCUMENT("pci_document", List.of("application/pdf"), 16 * 1024 * 1024),
  TAX_DOCUMENT_USER_UPLOAD(
      "tax_document_user_upload",
      List.of(
          "application/pdf",
          "image/jpeg",
          "image/png",
          "text/csv",
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
          "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
      16 * 1024 * 1024);

  private final String name;
  private final List<String> supportedMimetypes;
  private final Integer maxBytesSize;

  DocumentType(String name, List<String> supportedMimetypes, Integer maxSize) {
    this.name = name;
    this.supportedMimetypes = supportedMimetypes;
    this.maxBytesSize = maxSize;
  }

  public String getName() {
    return name;
  }

  public List<String> getSupportedMimetypes() {
    return supportedMimetypes;
  }

  public Integer getMaxBytesSize() {
    return maxBytesSize;
  }

  public void validateDocument(MultipartFile file) {
    if (this.maxBytesSize < file.getSize()) {
      throw new InvalidKycDataException(
          String.format("Max allowed size for %s is %s", this.name, this.maxBytesSize));
    }

    if (!this.supportedMimetypes.contains(file.getContentType())) {
      throw new InvalidKycDataException(
          String.format("Document %s accepts just %s", this.name, this.supportedMimetypes));
    }
  }
}
