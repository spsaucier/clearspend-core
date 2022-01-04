package com.clearspend.capital.controller.type.review;

import com.clearspend.capital.service.SoftFailService.RequiredDocumentsForManualReview;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
@NoArgsConstructor
public class SoftFailRequiredDocumentsResponse {

  public record RequiredDocument(String documentName, String type, String entityTokenId) {}

  public record KycDocuments(String owner, List<RequiredDocument> documents) {}

  @JsonProperty("kybRequiredDocuments")
  @NonNull
  List<RequiredDocument> kybRequiredDocuments;

  @JsonProperty("kycRequiredDocuments")
  @NonNull
  List<KycDocuments> kycRequiredDocuments;

  public SoftFailRequiredDocumentsResponse(
      RequiredDocumentsForManualReview requestedDocumentsByAlloy) {
    this.kybRequiredDocuments =
        requestedDocumentsByAlloy.kybDocuments() != null
            ? requestedDocumentsByAlloy.kybDocuments().kybErrorCodeList().stream()
                .map(
                    kybErrorCode ->
                        new SoftFailRequiredDocumentsResponse.RequiredDocument(
                            kybErrorCode.getDocuments().stream()
                                .map(KybDocuments::getDocumentName)
                                .collect(Collectors.joining(" or ")),
                            kybErrorCode.getDocuments().stream()
                                .map(kybDocuments -> kybDocuments.getDocumentType().name())
                                .distinct()
                                .collect(Collectors.joining("|")),
                            requestedDocumentsByAlloy.kybDocuments().entityTokenId()))
                .distinct()
                .collect(Collectors.toList())
            : Collections.emptyList();
    this.kycRequiredDocuments =
        requestedDocumentsByAlloy.kycDocuments().stream()
            .map(
                kycOwnerDocuments ->
                    new SoftFailRequiredDocumentsResponse.KycDocuments(
                        kycOwnerDocuments.owner(),
                        kycOwnerDocuments.kycErrorCodes().stream()
                            .map(
                                kycErrorCode ->
                                    new SoftFailRequiredDocumentsResponse.RequiredDocument(
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
