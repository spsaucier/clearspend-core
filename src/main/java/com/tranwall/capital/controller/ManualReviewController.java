package com.tranwall.capital.controller;

import com.tranwall.capital.client.alloy.AlloyClient;
import com.tranwall.capital.client.alloy.response.EntityInformation;
import com.tranwall.capital.client.alloy.response.OnboardResponse;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.CurrentUser;
import com.tranwall.capital.controller.type.review.AlloyWebHookResponse;
import com.tranwall.capital.controller.type.review.ChildEntity;
import com.tranwall.capital.controller.type.review.GroupManualReviewOutcome;
import com.tranwall.capital.controller.type.review.KybDocuments;
import com.tranwall.capital.controller.type.review.KybErrorCode;
import com.tranwall.capital.controller.type.review.KycDocuments;
import com.tranwall.capital.controller.type.review.KycErrorCode;
import com.tranwall.capital.controller.type.review.ManualReviewResponse;
import com.tranwall.capital.data.model.Alloy;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.BusinessOwner;
import com.tranwall.capital.data.model.enums.AlloyTokenType;
import com.tranwall.capital.data.model.enums.BusinessOnboardingStep;
import com.tranwall.capital.data.model.enums.BusinessStatus;
import com.tranwall.capital.data.model.enums.KnowYourBusinessStatus;
import com.tranwall.capital.data.model.enums.KnowYourCustomerStatus;
import com.tranwall.capital.data.repository.AlloyRepository;
import com.tranwall.capital.service.BusinessOwnerService;
import com.tranwall.capital.service.BusinessService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/manual-review")
@RequiredArgsConstructor
public class ManualReviewController {

  private final AlloyClient alloyClient;

  private final AlloyRepository alloyRepository;

  private final BusinessService businessService;
  private final BusinessOwnerService businessOwnerService;

  public record KycOwnerDocuments(
      String owner, String entityTokenId, List<KycErrorCode> kycErrorCodes) {}

  public record KybEntityTokenAndErrorCode(
      String entityTokenId, List<KybErrorCode> kybErrorCodeList) {}

  public record RequiredDocumentsForManualReview(
      KybEntityTokenAndErrorCode kybDocuments, List<KycOwnerDocuments> kycDocuments) {}

  @GetMapping
  public ManualReviewResponse getRequiredDocumentsForManualReview() {
    final RequiredDocumentsForManualReview documentsForManualReview = getDocumentsForManualReview();
    return new ManualReviewResponse(
        buildKybRequiredDocuments(documentsForManualReview),
        buildKycRequiredDocuments(documentsForManualReview));
  }

  @PostMapping
  public String uploadManualReviewDocuments(
      @RequestParam(name = "documentList") List<MultipartFile> files) {
    Business business = businessService.getBusiness(CurrentUser.get().businessId()).business();
    String group = business.getLegalName().replaceAll(" ", "") + business.getBusinessPhone();

    for (MultipartFile multipartFile : files) {
      alloyClient.uploadDocument(multipartFile, group);
    }

    businessService.updateBusiness(
        CurrentUser.get().businessId(),
        BusinessStatus.ONBOARDING,
        BusinessOnboardingStep.REVIEW,
        null);

    return "Files successfully sent for review.";
  }

  @PatchMapping("/alloy/web-hook")
  public void updateHookFromAlloy(@RequestBody AlloyWebHookResponse webHookResponse) {
    String reviewOutcome = webHookResponse.getData().getOutcome();

    if (reviewOutcome.equals(GroupManualReviewOutcome.APPROVED.getValue())) {
      List<Alloy> alloyEntities = new ArrayList<>();

      webHookResponse
          .getData()
          .getChildEntities()
          .forEach(
              childEntity -> {
                List<Alloy> alloyTokenReferenceList =
                    alloyRepository.findAllByEntityToken(childEntity.getEntityToken());
                if (alloyTokenReferenceList.size() > 0) {
                  Alloy alloyTokenReference = alloyTokenReferenceList.get(0);
                  if (alloyTokenReference != null) {
                    if (alloyTokenReference.getType() == AlloyTokenType.BUSINESS) {
                      businessService.updateBusiness(
                          alloyTokenReference.getBusinessId(),
                          BusinessStatus.ONBOARDING,
                          BusinessOnboardingStep.LINK_ACCOUNT,
                          KnowYourBusinessStatus.PASS);
                    } else {
                      businessOwnerService.updateBusinessOwnerStatus(
                          alloyTokenReference.getBusinessOwnerId(), KnowYourCustomerStatus.PASS);
                    }
                    alloyEntities.add(alloyTokenReference);
                  }
                }
              });

      if (alloyEntities.size() > 0) {
        businessService.updateBusiness(
            alloyEntities.get(0).getBusinessId(),
            BusinessStatus.ONBOARDING,
            BusinessOnboardingStep.LINK_ACCOUNT,
            KnowYourBusinessStatus.PASS);
        alloyRepository.deleteAll(alloyEntities);
      }
    }

    if (reviewOutcome.equals(GroupManualReviewOutcome.DENIED.getValue())) {
      ChildEntity childEntity =
          webHookResponse.getData().getChildEntities().stream().findFirst().orElse(null);

      assert childEntity != null;
      List<Alloy> alloyTokenReferenceList =
          alloyRepository.findAllByEntityToken(childEntity.getEntityToken());

      Alloy alloy = alloyTokenReferenceList.get(0);
      TypedId<BusinessId> businessId;
      if (alloy.getType() == AlloyTokenType.BUSINESS) {
        businessId = alloy.getBusinessId();
      } else {
        businessId =
            businessOwnerService.retrieveBusinessOwner(alloy.getBusinessOwnerId()).getBusinessId();
      }

      businessService.updateBusiness(
          businessId,
          BusinessStatus.CLOSED,
          BusinessOnboardingStep.REVIEW,
          KnowYourBusinessStatus.FAIL);
    }

    if (reviewOutcome.equals(GroupManualReviewOutcome.RESUBMIT_DOCUMENT.getValue())) {
      log.info("resubmit");

      ChildEntity childEntity =
          webHookResponse.getData().getChildEntities().stream().findFirst().orElse(null);

      assert childEntity != null;
      List<Alloy> alloyTokenReferenceList =
          alloyRepository.findAllByEntityToken(childEntity.getEntityToken());

      Alloy alloy = alloyTokenReferenceList.get(0);
      TypedId<BusinessId> businessId;
      if (alloy.getType() == AlloyTokenType.BUSINESS) {
        businessId = alloy.getBusinessId();
      } else {
        businessId =
            businessOwnerService.retrieveBusinessOwner(alloy.getBusinessOwnerId()).getBusinessId();
      }

      businessService.updateBusiness(
          businessId,
          BusinessStatus.ONBOARDING,
          BusinessOnboardingStep.SOFT_FAIL,
          KnowYourBusinessStatus.REVIEW);
    }
  }

  private List<ManualReviewResponse.KycDocuments> buildKycRequiredDocuments(
      RequiredDocumentsForManualReview documentsForManualReview) {
    return documentsForManualReview.kycDocuments.stream()
        .map(
            kycOwnerDocuments ->
                new ManualReviewResponse.KycDocuments(
                    kycOwnerDocuments.owner,
                    kycOwnerDocuments.kycErrorCodes.stream()
                        .map(
                            kycErrorCode ->
                                new ManualReviewResponse.RequiredDocument(
                                    kycErrorCode.getDocuments().stream()
                                        .map(KycDocuments::getDocumentName)
                                        .collect(Collectors.joining(" or ")),
                                    kycErrorCode.getDocuments().get(0).getDocumentType().name(),
                                    kycOwnerDocuments.entityTokenId))
                        .distinct()
                        .collect(Collectors.toList())))
        .distinct()
        .collect(Collectors.toList());
  }

  private List<ManualReviewResponse.RequiredDocument> buildKybRequiredDocuments(
      RequiredDocumentsForManualReview documentsForManualReview) {
    return documentsForManualReview.kybDocuments != null
        ? documentsForManualReview.kybDocuments.kybErrorCodeList.stream()
            .map(
                kybErrorCode ->
                    new ManualReviewResponse.RequiredDocument(
                        kybErrorCode.getDocuments().stream()
                            .map(KybDocuments::getDocumentName)
                            .collect(Collectors.joining(" or ")),
                        kybErrorCode.getDocuments().stream()
                            .map(kybDocuments -> kybDocuments.getDocumentType().name())
                            .distinct()
                            .collect(Collectors.joining("|")),
                        documentsForManualReview.kybDocuments.entityTokenId))
            .distinct()
            .collect(Collectors.toList())
        : Collections.emptyList();
  }

  private RequiredDocumentsForManualReview getDocumentsForManualReview() {
    List<Alloy> alloyList = alloyRepository.findAllByBusinessId(CurrentUser.get().businessId());
    List<KycOwnerDocuments> kycDocuments = new ArrayList<>();
    List<KybErrorCode> kybErrorCodeList = new ArrayList<>();
    KybEntityTokenAndErrorCode kybEntityTokenAndErrorCode = null;
    for (Alloy alloyEntity : alloyList) {
      if (alloyEntity.getType() == AlloyTokenType.BUSINESS_OWNER) {
        kycDocuments = extractKycRequiredDocuments(alloyEntity);
      } else {
        kybEntityTokenAndErrorCode = getKybEntityTokenAndErrorCode(kybErrorCodeList, alloyEntity);
      }
    }

    return new RequiredDocumentsForManualReview(kybEntityTokenAndErrorCode, kycDocuments);
  }

  private KybEntityTokenAndErrorCode getKybEntityTokenAndErrorCode(
      List<KybErrorCode> kybErrorCodeList, Alloy alloyEntity) {
    KybEntityTokenAndErrorCode kybEntityTokenAndErrorCode = null;
    EntityInformation entityInformation;
    entityInformation =
        alloyClient.getEntityInformationForBusinessEntity(alloyEntity.getEntityToken());
    if (entityInformation
        .getEvaluations()
        .get(entityInformation.getEvaluations().size() - 1)
        .getOutcome()
        .equals("Manual Review")) {
      OnboardResponse onboardResponse =
          alloyClient.getEvaluationForBusinessEntity(
              alloyEntity.getEntityToken(),
              entityInformation
                  .getEvaluations()
                  .get(entityInformation.getEvaluations().size() - 1)
                  .getEvaluationToken());
      List<String> tags = onboardResponse.getSummary().getTags();
      kybErrorCodeList.addAll(
          Arrays.stream(KybErrorCode.values())
              .filter(kybErrorCode -> tags.contains(kybErrorCode.getName()))
              .collect(Collectors.toList()));
      kybEntityTokenAndErrorCode =
          new KybEntityTokenAndErrorCode(alloyEntity.getEntityToken(), kybErrorCodeList);
    }
    return kybEntityTokenAndErrorCode;
  }

  private List<KycOwnerDocuments> extractKycRequiredDocuments(Alloy a) {
    List<KycOwnerDocuments> kycDocuments = new ArrayList<>();
    EntityInformation entityInformation;
    entityInformation = alloyClient.getEntityInformationForIndividualEntity(a.getEntityToken());
    if (entityInformation
        .getEvaluations()
        .get(entityInformation.getEvaluations().size() - 1)
        .getOutcome()
        .equals("Manual Review")) {
      OnboardResponse onboardResponse =
          alloyClient.getEvaluationForIndividualEntity(
              a.getEntityToken(),
              entityInformation
                  .getEvaluations()
                  .get(entityInformation.getEvaluations().size() - 1)
                  .getEvaluationToken());
      List<String> tags = onboardResponse.getSummary().getTags();
      List<KycErrorCode> errorCodesList =
          Arrays.stream(KycErrorCode.values())
              .filter(kycErrorCode -> tags.contains(kycErrorCode.getName()))
              .collect(Collectors.toList());
      if (errorCodesList.size() > 0) {
        BusinessOwner businessOwner =
            businessOwnerService.retrieveBusinessOwner(a.getBusinessOwnerId());
        kycDocuments.add(
            new KycOwnerDocuments(
                businessOwner.getFirstName().getEncrypted()
                    + " "
                    + businessOwner.getLastName().getEncrypted(),
                a.getEntityToken(),
                errorCodesList));
      }
    }
    return kycDocuments;
  }
}
