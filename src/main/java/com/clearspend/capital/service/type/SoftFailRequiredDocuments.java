package com.clearspend.capital.service.type;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SoftFailRequiredDocuments {

  public record RequiredDocument(String documentName, String type, String entityTokenId) {}

  public record KycDocuments(String owner, List<RequiredDocument> documents) {}

  List<RequiredDocument> kybRequiredDocuments;
  List<KycDocuments> kycRequiredDocuments;
}
