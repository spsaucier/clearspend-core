package com.tranwall.capital.controller;

import com.tranwall.capital.common.typedid.data.BusinessOwnerId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.CurrentUser;
import com.tranwall.capital.controller.type.business.owner.CreateBusinessOwnerResponse;
import com.tranwall.capital.controller.type.business.owner.CreateOrUpdateBusinessOwnerRequest;
import com.tranwall.capital.data.model.Business;
import com.tranwall.capital.data.model.BusinessOwner;
import com.tranwall.capital.data.model.enums.BusinessOnboardingStep;
import com.tranwall.capital.data.model.enums.BusinessStatus;
import com.tranwall.capital.data.model.enums.KnowYourBusinessStatus;
import com.tranwall.capital.data.model.enums.KnowYourCustomerStatus;
import com.tranwall.capital.service.BusinessOwnerService;
import com.tranwall.capital.service.BusinessOwnerService.BusinessOwnerAndUserRecord;
import com.tranwall.capital.service.BusinessService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
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

  @PostMapping("")
  private CreateBusinessOwnerResponse createBusinessOwner(
      @Validated @RequestBody CreateOrUpdateBusinessOwnerRequest request) {
    BusinessOwnerAndUserRecord businessOwnerRecord =
        businessOwnerService.createBusinessOwner(
            null,
            CurrentUser.get().businessId(),
            request.getFirstName(),
            request.getLastName(),
            request.getAddress().toAddress(),
            request.getEmail(),
            request.getPhone(),
            null);
    return new CreateBusinessOwnerResponse(businessOwnerRecord.businessOwner().getId(), null);
  }

  @PatchMapping("/{businessOwnerId}")
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
            alloyGroup);

    if (businessOwner.getKnowYourCustomerStatus() == KnowYourCustomerStatus.FAIL) {
      businessService.updateBusiness(
          businessOwner.getBusinessId(),
          BusinessStatus.CLOSED,
          BusinessOnboardingStep.COMPLETE,
          null);
    } else if (business.getKnowYourBusinessStatus() == KnowYourBusinessStatus.REVIEW
        || businessOwner.getKnowYourCustomerStatus() == KnowYourCustomerStatus.REVIEW) {
      businessService.updateBusiness(
          businessOwner.getBusinessId(),
          business.getStatus(),
          BusinessOnboardingStep.SOFT_FAIL,
          null);
    } else if (request.isOnboarding()) {
      businessService.updateBusiness(
          businessOwner.getBusinessId(),
          BusinessStatus.ONBOARDING,
          BusinessOnboardingStep.LINK_ACCOUNT,
          null);
    }
  }
}
