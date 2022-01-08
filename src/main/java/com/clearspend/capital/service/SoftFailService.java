package com.clearspend.capital.service;

import com.clearspend.capital.client.alloy.AlloyClient;
import com.clearspend.capital.client.alloy.response.EntityInformation;
import com.clearspend.capital.client.alloy.response.OnboardResponse;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.controller.type.CurrentUser;
import com.clearspend.capital.controller.type.review.KybErrorCode;
import com.clearspend.capital.controller.type.review.KycErrorCode;
import com.clearspend.capital.data.model.Alloy;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.enums.AlloyTokenType;
import com.clearspend.capital.data.model.enums.BusinessOnboardingStep;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.clearspend.capital.data.repository.AlloyRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class SoftFailService {

  public static final String MANUAL_REVIEW = "Manual Review";
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

  public void uploadManualReviewDocuments(List<MultipartFile> files) {
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
  }

  public RequiredDocumentsForManualReview getDocumentsForManualReview(
      TypedId<BusinessId> businessId) {
    List<Alloy> alloyList = alloyRepository.findAllByBusinessId(businessId);
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
        .equals(MANUAL_REVIEW)) {
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
        .equals(MANUAL_REVIEW)) {
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
