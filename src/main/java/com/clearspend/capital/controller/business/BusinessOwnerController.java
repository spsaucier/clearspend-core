package com.clearspend.capital.controller.business;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.data.business.BusinessOwnerId;
import com.clearspend.capital.controller.type.business.Business;
import com.clearspend.capital.controller.type.business.owner.BusinessOwnerInfo;
import com.clearspend.capital.controller.type.business.owner.CreateBusinessOwnerResponse;
import com.clearspend.capital.controller.type.business.owner.CreateOrUpdateBusinessOwnerRequest;
import com.clearspend.capital.controller.type.business.owner.OwnersProvidedRequest;
import com.clearspend.capital.controller.type.business.owner.OwnersProvidedResponse;
import com.clearspend.capital.service.BusinessOwnerService;
import com.clearspend.capital.service.BusinessOwnerService.BusinessAndAccountErrorMessages;
import com.clearspend.capital.service.BusinessOwnerService.BusinessOwnerAndStripePersonRecord;
import com.clearspend.capital.service.type.BusinessOwnerData;
import com.clearspend.capital.service.type.CurrentUser;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/business-owners")
@RequiredArgsConstructor
public class BusinessOwnerController {

  private final BusinessOwnerService businessOwnerService;

  @PostMapping(
      value = "/create",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  CreateBusinessOwnerResponse createBusinessOwner(
      @Validated @RequestBody CreateOrUpdateBusinessOwnerRequest request) {

    log.info("Create business owner. {}", request);
    TypedId<BusinessId> businessId = CurrentUser.get().businessId();
    BusinessOwnerData businessOwnerData = request.toBusinessOwnerData(businessId);
    businessOwnerService.validateOwner(businessOwnerData);
    BusinessOwnerAndStripePersonRecord businessOwnerAndStripePersonRecord =
        businessOwnerService.createBusinessOwnerAndStripePerson(businessId, businessOwnerData);

    return new CreateBusinessOwnerResponse(
        businessOwnerAndStripePersonRecord.businessOwner().getId(), null);
  }

  @PatchMapping(
      value = "/update",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  CreateBusinessOwnerResponse updateBusinessOwner(
      @Validated @RequestBody CreateOrUpdateBusinessOwnerRequest request) {

    log.info("Update business owner. {}", request);
    TypedId<BusinessId> businessId = CurrentUser.get().businessId();
    BusinessOwnerData businessOwnerData = request.toBusinessOwnerData(businessId);
    businessOwnerService.validateOwner(businessOwnerData);
    BusinessOwnerAndStripePersonRecord businessOwnerAndStripePersonRecord =
        businessOwnerService.updateBusinessOwnerAndStripePerson(businessId, businessOwnerData);

    return new CreateBusinessOwnerResponse(
        businessOwnerAndStripePersonRecord.businessOwner().getId(), null);
  }

  @DeleteMapping(value = "/{businessOwnerId}")
  ResponseEntity<?> deleteBusinessOwner(
      @PathVariable(value = "businessOwnerId")
          @Parameter(
              required = true,
              name = "businessOwnerId",
              description = "ID of the businessOwner record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<BusinessOwnerId> businessOwnerId) {
    log.info("Delete business owner. {}", businessOwnerId);
    TypedId<BusinessId> businessId = CurrentUser.get().businessId();

    businessOwnerService.deleteBusinessOwner(businessOwnerId, businessId);

    return ResponseEntity.ok().build();
  }

  @GetMapping(value = "/list")
  List<BusinessOwnerInfo> getBusinessOwnersForCurrentLoggedBusiness() {
    TypedId<BusinessId> businessId = CurrentUser.get().businessId();
    return businessOwnerService.findBusinessOwnerByBusinessId(businessId).stream()
        .map(BusinessOwnerInfo::fromBusinessOwner)
        .toList();
  }

  @PostMapping("/trigger-all-owners-provided")
  OwnersProvidedResponse allOwnersProvided(
      @Validated @RequestBody(required = false) OwnersProvidedRequest ownersProvidedRequest) {

    log.info("Trigger end of onboarding owners process.");
    TypedId<BusinessId> businessId = CurrentUser.get().businessId();

    businessOwnerService.validateBusinessOwners(businessId, ownersProvidedRequest);

    Assert.notNull(businessId, "Action not possible!");
    BusinessAndAccountErrorMessages businessAndAccountErrorMessages =
        businessOwnerService.allOwnersProvided(businessId, ownersProvidedRequest);

    return new OwnersProvidedResponse(
        new Business(businessAndAccountErrorMessages.business()),
        businessAndAccountErrorMessages.errorMessages());
  }
}
