package com.clearspend.capital.controller.type.review;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessOwnerId;
import com.clearspend.capital.service.ApplicationReviewService.RequiredDocumentsForStripe;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public class ApplicationReviewRequirements {

  public record RequiredDocument(String documentName, String type, String entityTokenId) {}

  public record KycDocuments(String owner, List<RequiredDocument> documents) {}

  @JsonProperty("kybRequiredFields")
  private final List<String> kybRequiredFields;

  @JsonProperty("kycRequiredFields")
  private final Map<TypedId<BusinessOwnerId>, List<String>> kycRequiredFields;

  @JsonProperty("kybRequiredDocuments")
  private final List<RequiredDocument> kybRequiredDocuments;

  @JsonProperty("kycRequiredDocuments")
  private final List<KycDocuments> kycRequiredDocuments;

  @JsonProperty("requireOwner")
  private final Boolean requireOwner;

  @JsonProperty("requireRepresentative")
  private final Boolean requireRepresentative;

  @JsonProperty("pendingVerification")
  private final List<String> pendingVerification;

  @JsonProperty("errorCodes")
  private final List<StripeRequirementsErrorCode> errorCodes;

  public static ApplicationReviewRequirements from(
      List<String> businessRequiredFields,
      Map<TypedId<BusinessOwnerId>, List<String>> personRequiredFields,
      RequiredDocumentsForStripe requiredDocumentsForStripe,
      Boolean requireOwner,
      Boolean requireRepresentative,
      List<String> pendingVerification,
      List<StripeRequirementsErrorCode> errorCodes) {
    List<RequiredDocument> kybRequiredDocuments =
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
                .toList()
            : Collections.emptyList();
    List<KycDocuments> kycRequiredDocuments =
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
                            .toList()))
            .distinct()
            .toList();

    return new ApplicationReviewRequirements(
        businessRequiredFields,
        personRequiredFields,
        kybRequiredDocuments,
        kycRequiredDocuments,
        requireOwner,
        requireRepresentative,
        pendingVerification,
        errorCodes);
  }
}
