package com.tranwall.capital.controller;

import com.tranwall.capital.common.typedid.data.BusinessOwnerId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.business.owner.UpdateBusinessOwnerRequest;
import com.tranwall.capital.service.BusinessOwnerService;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/business-owners")
@RequiredArgsConstructor
public class BusinessOwnerController {

  private final BusinessOwnerService businessOwnerService;

  @PatchMapping("/{businessOwnerId}")
  private void updateBusinessOwner(
      @PathVariable(value = "businessOwnerId")
          @Parameter(
              required = true,
              name = "businessOwnerId",
              description = "ID of the businessOwner record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<BusinessOwnerId> businessOwnerId,
      @Validated @RequestBody UpdateBusinessOwnerRequest request) {
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
