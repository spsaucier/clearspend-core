package com.clearspend.capital.controller.business;

import com.clearspend.capital.common.data.util.HttpReqRespUtils;
import com.clearspend.capital.common.error.TosAndPrivacyPolicyException;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessProspectId;
import com.clearspend.capital.controller.type.business.Business;
import com.clearspend.capital.controller.type.business.prospect.BusinessProspectData;
import com.clearspend.capital.controller.type.business.prospect.ConvertBusinessProspectResponse;
import com.clearspend.capital.controller.type.business.prospect.ConvertClientBusinessProspectRequest;
import com.clearspend.capital.controller.type.business.prospect.ConvertPartnerBusinessProspectRequest;
import com.clearspend.capital.controller.type.business.prospect.CreateBusinessProspectResponse;
import com.clearspend.capital.controller.type.business.prospect.CreateOrUpdateBusinessProspectRequest;
import com.clearspend.capital.controller.type.business.prospect.SetBusinessProspectPasswordRequest;
import com.clearspend.capital.controller.type.business.prospect.SetBusinessProspectPhoneRequest;
import com.clearspend.capital.controller.type.business.prospect.ValidateBusinessProspectIdentifierRequest;
import com.clearspend.capital.controller.type.business.prospect.ValidateBusinessProspectIdentifierRequest.IdentifierType;
import com.clearspend.capital.controller.type.business.prospect.ValidateIdentifierResponse;
import com.clearspend.capital.data.model.enums.BusinessType;
import com.clearspend.capital.service.BusinessProspectService;
import com.clearspend.capital.service.BusinessProspectService.BusinessProspectRecord;
import com.clearspend.capital.service.BusinessProspectService.ConvertBusinessProspectRecord;
import com.clearspend.capital.service.BusinessProspectService.OnboardingBusinessProspectMethod;
import io.swagger.v3.oas.annotations.Parameter;
import java.util.Objects;
import javax.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class BusinessProspectController {

  public static final boolean CREATOR_CONSIDER_AS_DEFAULT_REPRESENTATIVE = true;

  // This can enable/disable send email and phone validation codes
  @Value("${clearspend.onboarding-validation:true}")
  private boolean onboardingEmailPhoneValidation;

  private final BusinessProspectService businessProspectService;

  @PostMapping("")
  @OnboardingBusinessProspectMethod(
      reviewer = "Craig Miller",
      explanation = "This is a part of the onboarding process before users exist for the business.")
  CreateBusinessProspectResponse createBusinessProspect(
      @Validated @RequestBody CreateOrUpdateBusinessProspectRequest request,
      HttpServletRequest httpServletRequest) {
    if (!request.getTosAndPrivacyPolicyAcceptance()) {
      throw new TosAndPrivacyPolicyException();
    }
    String clientIp = HttpReqRespUtils.getClientIpAddressIfServletRequestExist(httpServletRequest);
    String userAgent = HttpReqRespUtils.getUserAgent(httpServletRequest);
    log.info("ip of client: {} and userAgent: {}", clientIp, userAgent);

    BusinessProspectRecord businessProspect =
        businessProspectService.createOrUpdateBusinessProspect(
            request.getFirstName(),
            request.getLastName(),
            request.getBusinessType(),
            request.getRelationshipOwner(),
            request.getBusinessType() != null
                && !Objects.equals(BusinessType.INDIVIDUAL, request.getBusinessType())
                && CREATOR_CONSIDER_AS_DEFAULT_REPRESENTATIVE,
            request.getRelationshipExecutive(),
            false, // for now we decide to ignore director option
            request.getEmail(),
            clientIp,
            userAgent,
            onboardingEmailPhoneValidation);

    return new CreateBusinessProspectResponse(
        businessProspect.businessProspect().getId(), businessProspect.businessProspectStatus());
  }

  @OnboardingBusinessProspectMethod(
      reviewer = "Craig Miller",
      explanation = "This is a part of the onboarding process before users exist for the business.")
  @GetMapping("/{businessProspectId}/{otpType}/resend-otp")
  void resendOtp(
      @PathVariable(value = "businessProspectId")
          @Parameter(
              required = true,
              name = "businessProspectId",
              description = "ID of the businessProspect record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<BusinessProspectId> businessProspectId,
      @PathVariable(value = "otpType")
          @Parameter(
              required = true,
              name = "otpType",
              description = "Where otp will be send.",
              example = "EMAIL")
          IdentifierType otpType) {
    log.info("Resend otp code for {} on {}", businessProspectId, otpType);
    businessProspectService.resendValidationCode(
        businessProspectId, otpType, onboardingEmailPhoneValidation);
  }

  @OnboardingBusinessProspectMethod(
      reviewer = "Craig Miller",
      explanation = "This is a part of the onboarding process before users exist for the business.")
  @PostMapping("/{businessProspectId}/validate-identifier")
  ValidateIdentifierResponse validateBusinessProspectIdentifier(
      @PathVariable(value = "businessProspectId")
          @Parameter(
              required = true,
              name = "businessProspectId",
              description = "ID of the businessProspect record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<BusinessProspectId> businessProspectId,
      @Validated @RequestBody ValidateBusinessProspectIdentifierRequest request) {
    return businessProspectService.validateBusinessProspectIdentifier(
        businessProspectId,
        request.getIdentifierType(),
        request.getOtp(),
        onboardingEmailPhoneValidation);
  }

  @OnboardingBusinessProspectMethod(
      reviewer = "Craig Miller",
      explanation = "This is a part of the onboarding process before users exist for the business.")
  @PostMapping("/{businessProspectId}/phone")
  void setBusinessProspectPhone(
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

  @OnboardingBusinessProspectMethod(
      reviewer = "Craig Miller",
      explanation = "This is a part of the onboarding process before users exist for the business.")
  @PostMapping("/{businessProspectId}/password")
  void setBusinessProspectPassword(
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

  @OnboardingBusinessProspectMethod(
      reviewer = "Craig Miller",
      explanation = "This is a part of the onboarding process before users exist for the business.")
  @PostMapping("/{businessProspectId}/convert")
  ConvertBusinessProspectResponse convertClientBusinessProspect(
      @PathVariable(value = "businessProspectId")
          @Parameter(
              required = true,
              name = "businessProspectId",
              description = "ID of the businessProspect record.",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<BusinessProspectId> businessProspectId,
      @Validated @RequestBody ConvertClientBusinessProspectRequest request) {
    ConvertBusinessProspectRecord convertBusinessProspectRecord =
        businessProspectService.convertClientBusinessProspect(
            request.toConvertBusinessProspect(businessProspectId));

    return new ConvertBusinessProspectResponse(
        new Business(convertBusinessProspectRecord.business()),
        convertBusinessProspectRecord.businessOwner().getId());
  }

  @OnboardingBusinessProspectMethod(
      reviewer = "Patrick Morton",
      explanation = "This is part of the Partner onboarding process before the user exists")
  @PostMapping("/{businessProspectId}/partner-convert")
  ConvertBusinessProspectResponse convertPartnerBusinessProspect(
      @PathVariable(value = "businessProspectId")
          @Parameter(
              required = true,
              name = "businessProspectId",
              description = "ID of the businessProspect record",
              example = "48104ecb-1343-4cc1-b6f2-e6cc88e9a80f")
          TypedId<BusinessProspectId> businessProspectId,
      @Validated @RequestBody ConvertPartnerBusinessProspectRequest request) {
    ConvertBusinessProspectRecord convertBusinessProspectRecord =
        businessProspectService.convertPartnerBusinessProspect(
            request.toConvertBusinessProspect(businessProspectId));

    return new ConvertBusinessProspectResponse(
        new Business(convertBusinessProspectRecord.business()),
        convertBusinessProspectRecord.businessOwner().getId());
  }

  @OnboardingBusinessProspectMethod(
      reviewer = "Craig Miller",
      explanation = "This is a part of the onboarding process before users exist for the business.")
  @GetMapping("/{businessProspectId}")
  BusinessProspectData getBusinessProspectData(
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
