package com.clearspend.capital.controller.business;

import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessProspectId;
import com.clearspend.capital.controller.type.business.Business;
import com.clearspend.capital.controller.type.business.prospect.BusinessProspectData;
import com.clearspend.capital.controller.type.business.prospect.ConvertBusinessProspectRequest;
import com.clearspend.capital.controller.type.business.prospect.ConvertBusinessProspectResponse;
import com.clearspend.capital.controller.type.business.prospect.CreateBusinessProspectRequest;
import com.clearspend.capital.controller.type.business.prospect.CreateBusinessProspectResponse;
import com.clearspend.capital.controller.type.business.prospect.SetBusinessProspectPasswordRequest;
import com.clearspend.capital.controller.type.business.prospect.SetBusinessProspectPhoneRequest;
import com.clearspend.capital.controller.type.business.prospect.ValidateBusinessProspectIdentifierRequest;
import com.clearspend.capital.service.BusinessProspectService;
import com.clearspend.capital.service.BusinessProspectService.BusinessProspectRecord;
import com.clearspend.capital.service.BusinessProspectService.ConvertBusinessProspectRecord;
import io.swagger.v3.oas.annotations.Parameter;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/business-prospects")
@RequiredArgsConstructor
public class BusinessProspectController {

  public static final boolean CREATOR_CONSIDER_AS_DEFAULT_REPRESENTATIVE = true;

  // This can enable/disable send email and phone validation codes
  @Value("${clearspend.onboarding-validation:true}")
  private boolean onboardingEmailPhoneValidation;

  private final BusinessProspectService businessProspectService;

  @PostMapping("")
  private CreateBusinessProspectResponse createBusinessProspect(
      @Validated @RequestBody CreateBusinessProspectRequest request) {
    BusinessProspectRecord record =
        businessProspectService.createBusinessProspect(
            request.getFirstName(),
            request.getLastName(),
            request.getBusinessType(),
            request.getRelationshipOwner(),
            CREATOR_CONSIDER_AS_DEFAULT_REPRESENTATIVE,
            request.getRelationshipExecutive(),
            request.getRelationshipDirector(),
            request.getEmail(),
            onboardingEmailPhoneValidation);

    return new CreateBusinessProspectResponse(
        record.businessProspect().getId(), record.businessProspectStatus());
  }

  @PostMapping("/{businessProspectId}/validate-identifier")
  private void validateBusinessProspectIdentifier(
      @PathVariable(value = "businessProspectId")
          @Parameter(
              required = true,
              name = "businessProspectId",
              description = "ID of the businessProspect record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<BusinessProspectId> businessProspectId,
      @Validated @RequestBody ValidateBusinessProspectIdentifierRequest request) {
    businessProspectService.validateBusinessProspectIdentifier(
        businessProspectId,
        request.getIdentifierType(),
        request.getOtp(),
        onboardingEmailPhoneValidation);
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
    businessProspectService.setBusinessProspectPhone(
        businessProspectId, request.getPhone(), onboardingEmailPhoneValidation);
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
    businessProspectService.setBusinessProspectPassword(
        businessProspectId, request.getPassword(), onboardingEmailPhoneValidation);
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
      @Validated @RequestBody ConvertBusinessProspectRequest request,
      HttpServletRequest httpServletRequest) {
    ConvertBusinessProspectRecord convertBusinessProspectRecord =
        businessProspectService.convertBusinessProspect(
            request.toConvertBusinessProspect(businessProspectId),
            httpServletRequest.getRemoteHost());

    return new ConvertBusinessProspectResponse(
        new Business(convertBusinessProspectRecord.business()),
        convertBusinessProspectRecord.businessOwner().getId());
  }

  @GetMapping("/{businessProspectId}")
  private BusinessProspectData getBusinessProspectData(
      @PathVariable(value = "businessProspectId")
          @Parameter(
              required = true,
              name = "businessProspectId",
              description = "ID of the businessProspect record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<BusinessProspectId> businessProspectId) {
    return BusinessProspectData.fromBusinessProspectEntity(
        businessProspectService.retrieveBusinessProspectById(businessProspectId));
  }
}
