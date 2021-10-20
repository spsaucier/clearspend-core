package com.tranwall.capital.controller;

import com.tranwall.capital.common.typedid.data.BusinessProspectId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.controller.type.business.prospect.ConvertBusinessProspectRequest;
import com.tranwall.capital.controller.type.business.prospect.ConvertBusinessProspectResponse;
import com.tranwall.capital.controller.type.business.prospect.CreateBusinessProspectRequest;
import com.tranwall.capital.controller.type.business.prospect.CreateBusinessProspectResponse;
import com.tranwall.capital.controller.type.business.prospect.SetBusinessProspectPasswordRequest;
import com.tranwall.capital.controller.type.business.prospect.SetBusinessProspectPhoneRequest;
import com.tranwall.capital.controller.type.business.prospect.SetBusinessProspectPhoneResponse;
import com.tranwall.capital.controller.type.business.prospect.ValidateBusinessProspectIdentifierRequest;
import com.tranwall.capital.data.model.BusinessProspect;
import com.tranwall.capital.service.BusinessProspectService;
import com.tranwall.capital.service.BusinessProspectService.ConvertBusinessProspectRecord;
import com.tranwall.capital.service.BusinessProspectService.CreateBusinessProspectRecord;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/business-prospects")
@RequiredArgsConstructor
public class BusinessProspectController {

  private final BusinessProspectService businessProspectService;

  @PostMapping("")
  private CreateBusinessProspectResponse createBusinessProspect(
      @Validated @RequestBody CreateBusinessProspectRequest request) {
    CreateBusinessProspectRecord createBusinessProspectRecord =
        businessProspectService.createBusinessProspect(
            request.getFirstName(), request.getLastName(), request.getEmail());

    return new CreateBusinessProspectResponse(
        createBusinessProspectRecord.businessProspect().getId());
  }

  @PostMapping("/{businessProspectId}/validate-identifier")
  private void validateBusinessProspectEmail(
      @PathVariable(value = "businessProspectId")
          @ApiParam(
              required = true,
              name = "businessProspectId",
              value = "ID of the businessProspect record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<BusinessProspectId> businessProspectId,
      @Validated @RequestBody ValidateBusinessProspectIdentifierRequest request) {
    businessProspectService.validateBusinessProspectIdentifier(
        businessProspectId, request.getIdentifierType(), request.getOtp());
  }

  @PostMapping("/{businessProspectId}/phone")
  private SetBusinessProspectPhoneResponse setBusinessProspectPhone(
      @PathVariable(value = "businessProspectId")
          @ApiParam(
              required = true,
              name = "businessProspectId",
              value = "ID of the businessProspect record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<BusinessProspectId> businessProspectId,
      @Validated @RequestBody SetBusinessProspectPhoneRequest request) {
    businessProspectService.setBusinessProspectPhone(businessProspectId, request.getPhone());
    return new SetBusinessProspectPhoneResponse("");
  }

  @PostMapping("/{businessProspectId}/password")
  private void setBusinessProspectPassword(
      @PathVariable(value = "businessProspectId")
          @ApiParam(
              required = true,
              name = "businessProspectId",
              value = "ID of the businessProspect record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<BusinessProspectId> businessProspectId,
      @Validated @RequestBody SetBusinessProspectPasswordRequest request) {
    BusinessProspect businessProspect =
        businessProspectService.setBusinessProspectPassword(
            businessProspectId, request.getPassword());
  }

  @PostMapping("/{businessProspectId}/convert")
  private ConvertBusinessProspectResponse convertBusinessProspect(
      @PathVariable(value = "businessProspectId")
          @ApiParam(
              required = true,
              name = "businessProspectId",
              value = "ID of the businessProspect record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<BusinessProspectId> businessProspectId,
      @Validated @RequestBody ConvertBusinessProspectRequest request) {
    ConvertBusinessProspectRecord convertBusinessProspectRecord =
        businessProspectService.convertBusinessProspect(
            businessProspectId,
            request.getLegalName(),
            request.getBusinessType(),
            request.getBusinessPhone(),
            request.getEmployerIdentificationNumber(),
            request.getAddress().toAddress());

    return new ConvertBusinessProspectResponse(
        convertBusinessProspectRecord.business().getId(),
        convertBusinessProspectRecord.businessOwner().getId());
  }
}
