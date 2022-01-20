package com.clearspend.capital.controller.business;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessOwnerId;
import com.clearspend.capital.controller.type.business.owner.CreateBusinessOwnerResponse;
import com.clearspend.capital.controller.type.business.owner.CreateOrUpdateBusinessOwnerRequest;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.enums.BusinessOnboardingStep;
import com.clearspend.capital.data.model.enums.BusinessStatus;
import com.clearspend.capital.data.model.enums.KnowYourBusinessStatus;
import com.clearspend.capital.data.model.enums.KnowYourCustomerStatus;
import com.clearspend.capital.service.BusinessOwnerService;
import com.clearspend.capital.service.BusinessOwnerService.BusinessOwnerAndUserRecord;
import com.clearspend.capital.service.BusinessService;
import com.clearspend.capital.service.type.CurrentUser;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/business-owners")
@RequiredArgsConstructor
public class BusinessOwnerController {

  private final BusinessService businessService;
  private final BusinessOwnerService businessOwnerService;

  @PostMapping(
      value = "",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  private CreateBusinessOwnerResponse createBusinessOwner(
      @Validated @RequestBody CreateOrUpdateBusinessOwnerRequest request) {

    Business business = businessService.retrieveBusiness(CurrentUser.get().businessId());
    String alloyGroup = business.getLegalName().replaceAll(" ", "") + business.getBusinessPhone();

    BusinessOwnerAndUserRecord businessOwnerRecord =
        businessOwnerService.createBusinessOwner(
            null,
            CurrentUser.get().businessId(),
            request.getFirstName(),
            request.getLastName(),
            request.getAddress().toAddress(),
            request.getEmail(),
            request.getPhone() == null ? "0" : request.getPhone(),
            null,
            request.isOnboarding(),
            alloyGroup);

    return new CreateBusinessOwnerResponse(businessOwnerRecord.businessOwner().getId(), null);
  }

  @PatchMapping(
      value = "/{businessOwnerId}",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  private void updateBusinessOwner(
      @PathVariable(value = "businessOwnerId")
          @Parameter(
              required = true,
              name = "businessOwnerId",
              description = "ID of the businessOwner record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<BusinessOwnerId> businessOwnerId,
      @Validated @RequestBody CreateOrUpdateBusinessOwnerRequest request) {

    Business business =
        businessService.retrieveBusiness(
            businessOwnerService.retrieveBusinessOwner(businessOwnerId).getBusinessId());
    String alloyGroup = business.getLegalName().replaceAll(" ", "") + business.getBusinessPhone();

    BusinessOwner businessOwner =
        businessOwnerService.updateBusinessOwner(
            businessOwnerId,
            request.getFirstName(),
            request.getLastName(),
            request.getEmail(),
            request.getTaxIdentificationNumber(),
            request.getDateOfBirth(),
            request.getAddress().toAddress(),
            alloyGroup,
            request.isOnboarding());

    if (request.isOnboarding()) {
      List<BusinessOwner> businessOwners =
          businessOwnerService.findBusinessOwnerByBusinessId(business.getId());
      if (businessOwners.stream()
          .anyMatch(
              businessOwner1 ->
                  businessOwner1.getKnowYourCustomerStatus() == KnowYourCustomerStatus.FAIL)) {
        businessService.updateBusiness(
            businessOwner.getBusinessId(), BusinessStatus.CLOSED, null, null);
      } else if (business.getKnowYourBusinessStatus() == KnowYourBusinessStatus.REVIEW
          || businessOwners.stream()
              .anyMatch(
                  businessOwner1 ->
                      businessOwner1.getKnowYourCustomerStatus()
                          == KnowYourCustomerStatus.REVIEW)) {
        businessService.updateBusiness(
            businessOwner.getBusinessId(),
            business.getStatus(),
            BusinessOnboardingStep.SOFT_FAIL,
            null);
      } else {
        businessService.updateBusiness(
            businessOwner.getBusinessId(),
            BusinessStatus.ONBOARDING,
            BusinessOnboardingStep.LINK_ACCOUNT,
            null);
      }
    }
  }
}
