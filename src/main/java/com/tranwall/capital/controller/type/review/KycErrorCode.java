package com.tranwall.capital.controller.type.review;

import java.util.List;

public enum KycErrorCode {
  OFAC_MATCH("OFAC Match", List.of(KycDocuments.MANUAL_THIRD_PARTY_REVIEW)),
  SSN_MATCH("SSN Match", List.of(KycDocuments.TAX_RETURN)),
  DOB_MISMATCH("DOB Mismatch", List.of(KycDocuments.UNEXPIRED_GOVERNMENT_ISSUED_ID)),
  NAME_MISMATCH("Name Mismatch", List.of(KycDocuments.UNEXPIRED_GOVERNMENT_ISSUED_ID)),
  ADDRESS_MISMATCH(
      "Address Mismatch", List.of(KycDocuments.BANK_STATEMENT, KycDocuments.UTILITY_BILL));

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
