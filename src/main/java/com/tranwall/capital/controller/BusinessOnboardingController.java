package com.tranwall.capital.controller;

import com.tranwall.capital.common.data.model.ClearAddress;
import com.tranwall.capital.controller.type.ConvertBusinessProspectRequest;
import com.tranwall.capital.controller.type.CreateBusinessProspectRequest;
import com.tranwall.capital.controller.type.SetBusinessProspectPasswordRequest;
import com.tranwall.capital.controller.type.SetBusinessProspectPhoneRequest;
import com.tranwall.capital.controller.type.ValidateBusinessProspectIdentifierRequest;
import com.tranwall.capital.service.BusinessProspectService;
import io.swagger.annotations.ApiParam;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class BusinessOnboardingController {

  private final BusinessProspectService businessProspectService;

  @PostMapping("/business-prospect")
  private UUID createBusinessProspect(
      @Validated @RequestBody CreateBusinessProspectRequest request) {
    return businessProspectService
        .createBusinessProspect(request.getFirstName(), request.getLastName(), request.getEmail())
        .getId();
  }

  @PostMapping("/business-prospect/{businessProspectId}/validate-identifier")
  private void validateBusinessProspectEmail(
      @PathVariable(value = "businessProspectId")
          @ApiParam(
              required = true,
              name = "businessProspectId",
              value = "ID of the businessProspect record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          UUID businessProspectId,
      @Validated @RequestBody ValidateBusinessProspectIdentifierRequest request) {
    businessProspectService.validateBusinessProspectIdentifier(
        businessProspectId, request.getIdentifierType(), request.getOtp());
  }

  @PostMapping("/business-prospect/{businessProspectId}/phone")
  private void setBusinessProspectPhone(
      @PathVariable(value = "businessProspectId")
          @ApiParam(
              required = true,
              name = "businessProspectId",
              value = "ID of the businessProspect record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          UUID businessProspectId,
      @Validated @RequestBody SetBusinessProspectPhoneRequest request) {
    businessProspectService.setBusinessProspectPhone(businessProspectId, request.getPhone());
  }

  @PostMapping("/business-prospect/{businessProspectId}/password")
  private void setBusinessProspectPassword(
      @PathVariable(value = "businessProspectId")
          @ApiParam(
              required = true,
              name = "businessProspectId",
              value = "ID of the businessProspect record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          UUID businessProspectId,
      @Validated @RequestBody SetBusinessProspectPasswordRequest request) {
    businessProspectService.setBusinessProspectPassword(businessProspectId, request.getPassword());
  }

  @PostMapping("/business-prospect/{businessProspectId}/convert")
  private UUID convertBusinessProspect(
      @PathVariable(value = "businessProspectId")
          @ApiParam(
              required = true,
              name = "businessProspectId",
              value = "ID of the businessProspect record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          UUID businessProspectId,
      @Validated @RequestBody ConvertBusinessProspectRequest request) {
    return businessProspectService
        .convertBusinessProspect(
            businessProspectId,
            request.getLegalName(),
            request.getBusinessType(),
            request.getFormationDate(),
            new ClearAddress(
                request.getAddress().getStreetLine1(),
                request.getAddress().getStreetLine2(),
                request.getAddress().getLocality(),
                request.getAddress().getRegion(),
                request.getAddress().getPostalCode(),
                request.getAddress().getCountry()))
        .business()
        .getId();
  }
}
