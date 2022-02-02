package com.clearspend.capital.controller.type.review;

import com.clearspend.capital.service.ApplicationReviewService.RequiredDocumentsForStripe;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ApplicationReviewRequirements {

  public record RequiredDocument(String documentName, String type, String entityTokenId) {}

  public record KycDocuments(String owner, List<RequiredDocument> documents) {}

  @JsonProperty("kybRequiredFields")
  List<String> kybRequiredFields;

  @JsonProperty("kycRequiredFields")
  List<String> kycRequiredFields;

  @JsonProperty("kybRequiredDocuments")
  List<RequiredDocument> kybRequiredDocuments;

  @JsonProperty("kycRequiredDocuments")
  List<KycDocuments> kycRequiredDocuments;

  public ApplicationReviewRequirements(RequiredDocumentsForStripe requiredDocumentsForStripe) {
    this.kybRequiredDocuments =
        requiredDocumentsForStripe.kybDocuments() != null
            ? requiredDocumentsForStripe.kybDocuments().kybErrorCodeList().stream()
                .map(
                    kybErrorCode ->
                        new ApplicationReviewRequirements.RequiredDocument(
                            kybErrorCode.getDocuments().stream()
                                .map(KybDocuments::getDocumentName)
                                .collect(Collectors.joining(" or ")),
                            kybErrorCode.getDocuments().stream()
                                .map(kybDocuments -> kybDocuments.getDocumentType().name())
                                .distinct()
                                .collect(Collectors.joining("|")),
                            requiredDocumentsForStripe.kybDocuments().entityTokenId()))
                .distinct()
                .collect(Collectors.toList())
            : Collections.emptyList();
    this.kycRequiredDocuments =
        requiredDocumentsForStripe.kycDocuments().stream()
            .map(
                kycOwnerDocuments ->
                    new ApplicationReviewRequirements.KycDocuments(
                        kycOwnerDocuments.owner(),
                        kycOwnerDocuments.kycErrorCodes().stream()
                            .map(
                                kycErrorCode ->
                                    new ApplicationReviewRequirements.RequiredDocument(
                                        kycErrorCode.getDocuments().stream()
                                            .map(
                                                com.clearspend.capital.controller.type.review
                                                        .KycDocuments
                                                    ::getDocumentName)
                                            .collect(Collectors.joining(" or ")),
                                        kycErrorCode.getDocuments().get(0).getDocumentType().name(),
                                        kycOwnerDocuments.entityTokenId()))
                            .distinct()
                            .collect(Collectors.toList())))
            .distinct()
            .collect(Collectors.toList());
  }
}
