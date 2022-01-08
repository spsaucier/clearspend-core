package com.clearspend.capital.service;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.review.ChildEntity;
import com.clearspend.capital.controller.type.review.Data;
import com.clearspend.capital.controller.type.review.GroupManualReviewOutcome;
import com.clearspend.capital.controller.type.review.KybDocuments;
import com.clearspend.capital.controller.type.review.KycDocuments;
import com.clearspend.capital.data.model.Alloy;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.enums.AlloyTokenType;
import com.clearspend.capital.data.model.enums.BusinessOnboardingStep;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.clearspend.capital.data.model.enums.KnowYourBusinessStatus;
import com.clearspend.capital.data.model.enums.KnowYourCustomerStatus;
import com.clearspend.capital.data.repository.AlloyRepository;
import com.clearspend.capital.service.SoftFailService.RequiredDocumentsForManualReview;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlloyWebHookService {

  private final AlloyRepository alloyRepository;

  private final BusinessService businessService;
  private final BusinessOwnerService businessOwnerService;
  private final TwilioService twilioService;
  private final SoftFailService softFailService;

  public void processWebHookFromAlloy(
      GroupManualReviewOutcome reviewOutcome, Data alloyReviewData) {

    List<ChildEntity> reviewDataEntities = alloyReviewData.getChildEntities();

    switch (reviewOutcome) {
      case APPROVED -> allowCustomerToPassToNextStep(reviewDataEntities);
      case RESUBMIT_DOCUMENT -> requestNewSetOfDocumentsOnTheOnboardingPhase(reviewDataEntities);
      case DENIED -> blockCustomerToNotContinueOnboarding(
          reviewDataEntities, alloyReviewData.getReasons());
    }
  }

  private void blockCustomerToNotContinueOnboarding(
      List<ChildEntity> reviewDataEntities, List<String> reasons) {
    ChildEntity childEntity = reviewDataEntities.stream().findFirst().orElse(null);

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

    BusinessOwner businessOwnerPrincipal =
        businessOwnerService.findBusinessOwnerPrincipalByBusinessId(businessId);
    twilioService.sendKybKycFailEmail(
        businessOwnerPrincipal.getEmail().getEncrypted(),
        businessOwnerPrincipal.getFirstName().getEncrypted(),
        reasons);
  }

  private void requestNewSetOfDocumentsOnTheOnboardingPhase(List<ChildEntity> reviewDataEntities) {
    ChildEntity childEntity = reviewDataEntities.stream().findFirst().orElse(null);

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

    RequiredDocumentsForManualReview documentsForManualReview =
        softFailService.getDocumentsForManualReview(businessId);
    List<String> businessDocuments = getBusinessDocuments(documentsForManualReview);
    List<String> ownersDocuments = getOwnersDocuments(documentsForManualReview);

    BusinessOwner businessOwnerPrincipal =
        businessOwnerService.findBusinessOwnerPrincipalByBusinessId(businessId);
    twilioService.sendNotificationEmail(
        businessOwnerPrincipal.getEmail().getEncrypted(),
        "To continue the onboaring to ClearSpend please submit the new set of required documents. \n"
            + String.join("\n", businessDocuments)
            + "\n"
            + String.join("\n", ownersDocuments));
  }

  private List<String> getBusinessDocuments(
      RequiredDocumentsForManualReview documentsForManualReview) {
    return documentsForManualReview.kybDocuments() != null
        ? documentsForManualReview.kybDocuments().kybErrorCodeList().stream()
            .map(
                kybErrorCode ->
                    kybErrorCode.getDocuments().stream()
                        .map(KybDocuments::getDocumentName)
                        .collect(Collectors.joining(" or ")))
            .distinct()
            .collect(Collectors.toList())
        : Collections.emptyList();
  }

  private List<String> getOwnersDocuments(
      RequiredDocumentsForManualReview documentsForManualReview) {
    return documentsForManualReview.kycDocuments().stream()
        .map(
            kycOwnerDocuments ->
                kycOwnerDocuments.owner()
                    + " : "
                    + kycOwnerDocuments.kycErrorCodes().stream()
                        .map(
                            kycErrorCode ->
                                kycErrorCode.getDocuments().stream()
                                    .map(KycDocuments::getDocumentName)
                                    .collect(Collectors.joining(" or ")))
                        .distinct()
                        .collect(Collectors.joining("\n")))
        .distinct()
        .collect(Collectors.toList());
  }

  private void allowCustomerToPassToNextStep(List<ChildEntity> reviewDataEntities) {
    List<Alloy> alloyEntities = new ArrayList<>();

    reviewDataEntities.forEach(
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
      BusinessOwner businessOwnerPrincipal =
          businessOwnerService.findBusinessOwnerPrincipalByBusinessId(
              alloyEntities.get(0).getBusinessId());
      twilioService.sendKybKycPassEmail(
          businessOwnerPrincipal.getEmail().getEncrypted(),
          businessOwnerPrincipal.getFirstName().getEncrypted());
      businessService.updateBusiness(
          alloyEntities.get(0).getBusinessId(),
          BusinessStatus.ONBOARDING,
          BusinessOnboardingStep.LINK_ACCOUNT,
          KnowYourBusinessStatus.PASS);
      alloyRepository.deleteAll(alloyEntities);
    }
  }
}
