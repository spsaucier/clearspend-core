package com.clearspend.capital.controller.business;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessProspectId;
import com.clearspend.capital.controller.type.business.Business;
import com.clearspend.capital.controller.type.business.prospect.ConvertBusinessProspectRequest;
import com.clearspend.capital.controller.type.business.prospect.ConvertBusinessProspectResponse;
import com.clearspend.capital.controller.type.business.prospect.CreateBusinessProspectRequest;
import com.clearspend.capital.controller.type.business.prospect.CreateBusinessProspectResponse;
import com.clearspend.capital.controller.type.business.prospect.SetBusinessProspectPasswordRequest;
import com.clearspend.capital.controller.type.business.prospect.SetBusinessProspectPhoneRequest;
import com.clearspend.capital.controller.type.business.prospect.ValidateBusinessProspectIdentifierRequest;
import com.clearspend.capital.data.model.business.BusinessProspect;
import com.clearspend.capital.service.BusinessProspectService;
import com.clearspend.capital.service.BusinessProspectService.BusinessProspectRecord;
import com.clearspend.capital.service.BusinessProspectService.ConvertBusinessProspectRecord;
import io.swagger.v3.oas.annotations.Parameter;
import java.io.IOException;
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
    BusinessProspectRecord record =
        businessProspectService.createBusinessProspect(
            request.getFirstName(), request.getLastName(), request.getEmail());

    return new CreateBusinessProspectResponse(
        record.businessProspect().getId(), record.businessProspectStatus());
  }

  @PostMapping("/{businessProspectId}/validate-identifier")
  private void validateBusinessProspectEmail(
      @PathVariable(value = "businessProspectId")
          @Parameter(
              required = true,
              name = "businessProspectId",
              description = "ID of the businessProspect record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<BusinessProspectId> businessProspectId,
      @Validated @RequestBody ValidateBusinessProspectIdentifierRequest request) {
    businessProspectService.validateBusinessProspectIdentifier(
        businessProspectId, request.getIdentifierType(), request.getOtp());
  }

  @PostMapping("/{businessProspectId}/phone")
  private void setBusinessProspectPhone(
      @PathVariable(value = "businessProspectId")
          @Parameter(
              required = true,
              name = "businessProspectId",
              description = "ID of the businessProspect record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<BusinessProspectId> businessProspectId,
      @Validated @RequestBody SetBusinessProspectPhoneRequest request) {
    businessProspectService.setBusinessProspectPhone(businessProspectId, request.getPhone());
  }

  @PostMapping("/{businessProspectId}/password")
  private void setBusinessProspectPassword(
      @PathVariable(value = "businessProspectId")
          @Parameter(
              required = true,
              name = "businessProspectId",
              description = "ID of the businessProspect record.",
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
          @Parameter(
              required = true,
              name = "businessProspectId",
              description = "ID of the businessProspect record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<BusinessProspectId> businessProspectId,
      @Validated @RequestBody ConvertBusinessProspectRequest request)
      throws IOException {
    ConvertBusinessProspectRecord convertBusinessProspectRecord =
        businessProspectService.convertBusinessProspect(
            businessProspectId,
            request.getLegalName(),
            request.getBusinessType(),
            request.getBusinessPhone(),
            request.getEmployerIdentificationNumber(),
            request.getAddress().toAddress());

    return new ConvertBusinessProspectResponse(
        new Business(convertBusinessProspectRecord.business()),
        convertBusinessProspectRecord.businessOwner().getId());
  }
}
