package com.tranwall.capital.controller;

import com.tranwall.capital.common.typedid.data.BusinessOwnerId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.CurrentUser;
import com.tranwall.capital.controller.type.business.owner.CreateBusinessOwnerResponse;
import com.tranwall.capital.controller.type.business.owner.CreateOrUpdateBusinessOwnerRequest;
import com.tranwall.capital.data.model.BusinessOwner;
import com.tranwall.capital.service.BusinessOwnerService;
import io.swagger.v3.oas.annotations.Parameter;
import java.io.IOException;
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

  private final BusinessOwnerService businessOwnerService;

  @PostMapping("")
  private CreateBusinessOwnerResponse createBusinessOwner(
      @Validated @RequestBody CreateOrUpdateBusinessOwnerRequest request) throws IOException {
    BusinessOwner businessOwner =
        businessOwnerService.createBusinessOwner(
            null,
            CurrentUser.get().businessId(),
            request.getFirstName(),
            request.getLastName(),
            request.getAddress().toAddress(),
            request.getEmail(),
            request.getPhone(),
            true,
            null);
    return new CreateBusinessOwnerResponse(businessOwner.getId(), null);
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
    businessOwnerService.updateBusinessOwner(
        businessOwnerId,
        request.getFirstName(),
        request.getLastName(),
        request.getEmail(),
        request.getTaxIdentificationNumber(),
        request.getDateOfBirth(),
        request.getAddress().toAddress());
  }
}
