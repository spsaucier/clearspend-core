package com.tranwall.capital.controller.type.review;

import java.util.List;

public enum KybErrorCode {
  BUSINESS_ADDRESS_NO_VERIFIED(
      "Business Address Not Verified",
      List.of(KybDocuments.BANK_STATEMENT, KybDocuments.UTILITY_BILL)),
  SEC_STATE_CHECK("Sec State Check", List.of(KybDocuments.CERTIFICATE_OF_INCORPORATION)),
  FEIN_UNMATCHED("FEIN Unmatched", List.of(KybDocuments.PROCESSED_SS4, KybDocuments.TAX_RETURN)),
  OFAC_MATCH("Ofac Match", List.of(KybDocuments.MANUAL_THIRD_PARTY_REVIEW));

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
