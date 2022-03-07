package com.clearspend.capital.controller.type.review;

import java.util.List;

public enum KybErrorCode {
  COMPANY_VERIFICATION_DOCUMENT(
      "an IRS Letter 147C document or an IRS SS-4 confirmation letter",
      List.of(KybDocuments.IRS_LETTER_147C, KybDocuments.IRS_SS4_CONFIRMATION_LETTER));

  private String name;
  private List<KybDocuments> documents;

  KybErrorCode(String name, List<KybDocuments> documents) {
    this.name = name;
    this.documents = documents;
  }

  public String getName() {
    return name;
  }

  public List<KybDocuments> getDocuments() {
    return documents;
  }
}
