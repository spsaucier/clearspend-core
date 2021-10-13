package com.tranwall.capital.controller;

import com.tranwall.capital.controller.type.business.owner.UpdateBusinessOwnerRequest;
import com.tranwall.capital.service.BusinessOwnerService;
import io.swagger.annotations.ApiParam;
import java.util.UUID;
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
          @ApiParam(
              required = true,
              name = "businessOwnerId",
              value = "ID of the businessOwner record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          UUID businessOwnerId,
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
