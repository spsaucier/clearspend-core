package com.clearspend.capital.controller.type.review;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@NoArgsConstructor
public class ManualReviewResponse {

  public record RequiredDocument(String documentName, String type, String entityTokenId) {}

  public record KycDocuments(String owner, List<RequiredDocument> documents) {}

  @JsonProperty("kybRequiredDocuments")
  @NonNull
  List<RequiredDocument> kybRequiredDocuments;

  @JsonProperty("kycRequiredDocuments")
  @NonNull
  List<KycDocuments> kycRequiredDocuments;
}
