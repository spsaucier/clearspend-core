package com.clearspend.capital.controller.business;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.data.business.BusinessOwnerId;
import com.clearspend.capital.controller.type.business.Business;
import com.clearspend.capital.controller.type.business.owner.BusinessOwnerInfo;
import com.clearspend.capital.controller.type.business.owner.CreateBusinessOwnerResponse;
import com.clearspend.capital.controller.type.business.owner.CreateOrUpdateBusinessOwnerRequest;
import com.clearspend.capital.controller.type.business.owner.OwnersProvidedResponse;
import com.clearspend.capital.data.model.business.BusinessOwner;
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

  @Deprecated(forRemoval = true)
  @PostMapping(
      value = "",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  private List<CreateBusinessOwnerResponse> createOrUpdateBusinessOwners(
      @Validated @RequestBody List<CreateOrUpdateBusinessOwnerRequest> request) {

    TypedId<BusinessId> businessId = CurrentUser.get().businessId();
    List<BusinessOwner> businessOwners =
        businessOwnerService.createOrUpdateBusinessOwners(
            businessId,
            request.stream()
                .map(businessOwnerRequest -> businessOwnerRequest.toBusinessOwnerData(businessId))
                .toList());

    return businessOwners.stream()
        .map(businessOwner -> new CreateBusinessOwnerResponse(businessOwner.getId(), null))
        .toList();
  }

  @PostMapping(
      value = "/create",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  private CreateBusinessOwnerResponse createBusinessOwner(
      @Validated @RequestBody CreateOrUpdateBusinessOwnerRequest request) {

    log.info("Create business owner. {}", request);
    TypedId<BusinessId> businessId = CurrentUser.get().businessId();
    BusinessOwnerData businessOwnerData = request.toBusinessOwnerData(businessId);
    businessOwnerService.validateOwner(businessOwnerData);
    BusinessOwnerAndStripePersonRecord businessOwnerAndStripePersonRecord =
        businessOwnerService.createBusinessOwnerAndStripePerson(businessId, businessOwnerData);

    return new CreateBusinessOwnerResponse(
        businessOwnerAndStripePersonRecord.businessOwner().getId(),
        businessOwnerAndStripePersonRecord.personReport().errorMessages());
  }

  @PatchMapping(
      value = "/update",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  private CreateBusinessOwnerResponse updateBusinessOwner(
      @Validated @RequestBody CreateOrUpdateBusinessOwnerRequest request) {

    log.info("Update business owner. {}", request);
    TypedId<BusinessId> businessId = CurrentUser.get().businessId();
    BusinessOwnerData businessOwnerData = request.toBusinessOwnerData(businessId);
    businessOwnerService.validateOwner(businessOwnerData);
    BusinessOwnerAndStripePersonRecord businessOwnerAndStripePersonRecord =
        businessOwnerService.updateBusinessOwnerAndStripePerson(businessId, businessOwnerData);

    return new CreateBusinessOwnerResponse(
        businessOwnerAndStripePersonRecord.businessOwner().getId(),
        businessOwnerAndStripePersonRecord.personReport().errorMessages());
  }

  @DeleteMapping(value = "/{businessOwnerId}")
  private ResponseEntity<?> deleteBusinessOwner(
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
  private List<BusinessOwnerInfo> getBusinessOwnersForCurrentLoggedBusiness() {
    TypedId<BusinessId> businessId = CurrentUser.get().businessId();
    return businessOwnerService.findBusinessOwnerByBusinessId(businessId).stream()
        .map(BusinessOwnerInfo::fromBusinessOwner)
        .toList();
  }

  @GetMapping("/trigger-all-owners-provided")
  private OwnersProvidedResponse allOwnersProvided(
      @Validated @RequestBody(required = false) Boolean ignoreValidation) {

    log.info("Trigger end of onboarding owners process.");
    TypedId<BusinessId> businessId = CurrentUser.get().businessId();
    if (!Boolean.TRUE.equals(ignoreValidation)) {
      businessOwnerService.validateBusinessOwners(businessId);
    }
    Assert.notNull(businessId, "Action not possible!");
    BusinessAndAccountErrorMessages businessAndAccountErrorMessages =
        businessOwnerService.allOwnersProvided(businessId);

    return new OwnersProvidedResponse(
        new Business(businessAndAccountErrorMessages.business()),
        businessAndAccountErrorMessages.errorMessages());
  }
}
