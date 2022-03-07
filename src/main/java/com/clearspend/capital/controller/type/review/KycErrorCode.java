package com.clearspend.capital.controller.type.review;

import java.util.List;

public enum KycErrorCode {
  IDENTITY_DOCUMENT_FRONT(
      "Government issued documents (national IDs, driver’s licenses or passports) - Front",
      List.of(KycDocuments.IDENTITY_DOCUMENT_FRONT)),
  IDENTITY_DOCUMENT_BACK(
      "Government issued documents (national IDs, driver’s licenses or passports) - Back",
      List.of(KycDocuments.IDENTITY_DOCUMENT_BACK));

  private String name;
  private List<KycDocuments> documents;

  KycErrorCode(String name, List<KycDocuments> documents) {
    this.name = name;
    this.documents = documents;
  }

  public String getName() {
    return name;
  }

  public List<KycDocuments> getDocuments() {
    return documents;
  }
}
