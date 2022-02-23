package com.clearspend.capital.controller.type.review;

import java.util.List;

public enum KycErrorCode {
  OFAC_MATCH("OFAC Match", List.of(KycDocuments.MANUAL_THIRD_PARTY_REVIEW)),
  SSN_MATCH("SSN Match", List.of(KycDocuments.TAX_RETURN)),
  SSN_MISKEY("SSN Miskey", List.of(KycDocuments.TAX_RETURN)),
  SSN_WARNING("SSN Warning", List.of(KycDocuments.TAX_RETURN)),
  DOB_MISMATCH("DOB Mismatch", List.of(KycDocuments.IDENTITY_DOCUMENT)),
  DOB_MISKEY("DOB Miskey", List.of(KycDocuments.IDENTITY_DOCUMENT)),
  DOB_NOT_VERIFIED("DOB Not Verified", List.of(KycDocuments.IDENTITY_DOCUMENT)),
  NAME_MISMATCH("Name Mismatch", List.of(KycDocuments.IDENTITY_DOCUMENT)),
  ADDRESS_MISMATCH(
      "Address Mismatch", List.of(KycDocuments.BANK_STATEMENT, KycDocuments.UTILITY_BILL)),
  EMAIL_WARNING("Email Warning", List.of(KycDocuments.BANK_STATEMENT, KycDocuments.UTILITY_BILL)),
  FRAUD_RISK("Fraud Risk", List.of(KycDocuments.IDENTITY_DOCUMENT)),
  FRAUD_REVIEW("Fraud Review", List.of(KycDocuments.TAX_RETURN)),
  ADDRESS_WARNING(
      "Address Warning", List.of(KycDocuments.UTILITY_BILL, KycDocuments.BANK_STATEMENT)),
  IDENTITY_DOCUMENT("Identity document", List.of(KycDocuments.IDENTITY_DOCUMENT));

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
