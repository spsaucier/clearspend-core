package com.clearspend.capital.controller.business;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessId;
import com.clearspend.capital.common.typedid.data.business.BusinessOwnerId;
import com.clearspend.capital.controller.type.business.owner.CreateBusinessOwnerResponse;
import com.clearspend.capital.controller.type.business.owner.CreateOrUpdateBusinessOwnerRequest;
import com.clearspend.capital.controller.type.business.owner.UpdateBusinessOwnerRequest;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.service.BusinessOwnerService;
import com.clearspend.capital.service.type.BusinessOwnerData;
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

  private final BusinessOwnerService businessOwnerService;

  @PostMapping(
      value = "",
      produces = MediaType.APPLICATION_JSON_VALUE,
      consumes = MediaType.APPLICATION_JSON_VALUE)
  private List<CreateBusinessOwnerResponse> createOrUpdateBusinessOwner(
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
      @Validated @RequestBody UpdateBusinessOwnerRequest request) {

    // TODO:gb: should we inform Stripe for changed or updates on owners details?
    BusinessOwnerData businessOwnerData =
        request.toBusinessOwnerData(CurrentUser.get().businessId(), businessOwnerId);
    businessOwnerService.updateBusinessOwner(businessOwnerData);
  }
}
