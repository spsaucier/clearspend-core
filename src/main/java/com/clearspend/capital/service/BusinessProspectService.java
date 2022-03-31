package com.clearspend.capital.service;

import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.Table;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.common.typedid.data.business.BusinessOwnerId;
import com.clearspend.capital.common.typedid.data.business.BusinessProspectId;
import com.clearspend.capital.controller.type.business.prospect.BusinessProspectStatus;
import com.clearspend.capital.controller.type.business.prospect.ValidateBusinessProspectIdentifierRequest.IdentifierType;
import com.clearspend.capital.controller.type.business.prospect.ValidateIdentifierResponse;
import com.clearspend.capital.crypto.data.model.embedded.NullableEncryptedString;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedString;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.business.Business;
import com.clearspend.capital.data.model.business.BusinessOwner;
import com.clearspend.capital.data.model.business.BusinessProspect;
import com.clearspend.capital.data.model.business.TosAcceptance;
import com.clearspend.capital.data.model.enums.BusinessType;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.repository.business.BusinessProspectRepository;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.AllocationService.CreatesRootAllocation;
import com.clearspend.capital.service.BusinessOwnerService.BusinessOwnerAndUserRecord;
import com.clearspend.capital.service.BusinessService.BusinessAndStripeAccount;
import com.clearspend.capital.service.FusionAuthService.FusionAuthUserCreator;
import com.clearspend.capital.service.type.BusinessOwnerData;
import com.clearspend.capital.service.type.ConvertBusinessProspect;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessProspectService {

  private final BusinessProspectRepository businessProspectRepository;

  private final BusinessService businessService;
  private final UserService userService;
  private final AllocationService allocationService;
  private final BusinessOwnerService businessOwnerService;
  private final FusionAuthService fusionAuthService;
  private final TwilioService twilioService;

  public BusinessProspect retrieveBusinessProspect(TypedId<BusinessOwnerId> businessOwnerId) {
    return businessProspectRepository
        .findByBusinessOwnerId(businessOwnerId)
        .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS_PROSPECT, businessOwnerId));
  }

  public record BusinessProspectRecord(
      BusinessProspect businessProspect, BusinessProspectStatus businessProspectStatus) {}

  @Transactional
  public BusinessProspectRecord createOrUpdateBusinessProspect(
      String firstName,
      String lastName,
      BusinessType businessType,
      Boolean relationshipOwner,
      Boolean relationshipRepresentative,
      Boolean relationshipExecutive,
      Boolean relationshipDirector,
      String email,
      String tosAcceptanceIp,
      String userAgent,
      boolean live) {
    BusinessProspect businessProspect =
        businessProspectRepository
            .findByEmailHash(new RequiredEncryptedStringWithHash(email).getHash())
            .orElseGet(
                () -> {
                  BusinessProspect entity =
                      new BusinessProspect(
                          new RequiredEncryptedString(firstName),
                          new RequiredEncryptedString(lastName),
                          new RequiredEncryptedStringWithHash(email),
                          new TosAcceptance(
                              OffsetDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MICROS),
                              tosAcceptanceIp,
                              userAgent));
                  entity.setBusinessType(businessType);
                  entity.setRelationshipOwner(relationshipOwner);
                  entity.setRelationshipRepresentative(relationshipRepresentative);
                  entity.setRelationshipExecutive(relationshipExecutive);
                  entity.setRelationshipDirector(relationshipDirector);
                  return businessProspectRepository.save(entity);
                });

    if (StringUtils.isEmpty(businessProspect.getTosAcceptance().getIp())
        || StringUtils.isEmpty(businessProspect.getTosAcceptance().getUserAgent())) {
      throw new InvalidRequestException("Tos acceptance should have ip and userAgent values");
    }

    // Update first/last names in case a prospect has been resumed with different values
    if (!Objects.equals(businessProspect.getFirstName().getEncrypted(), firstName)) {
      businessProspect.setFirstName(new RequiredEncryptedString(firstName));
    }

    if (!Objects.equals(businessProspect.getLastName().getEncrypted(), lastName)) {
      businessProspect.setLastName(new RequiredEncryptedString(lastName));
    }

    if (!Objects.equals(businessProspect.getBusinessType(), businessType)) {
      businessProspect.setBusinessType(businessType);
    }

    if (!Objects.equals(businessProspect.getRelationshipOwner(), relationshipOwner)) {
      businessProspect.setRelationshipOwner(relationshipOwner);
    }

    if (!Objects.equals(
        businessProspect.getRelationshipRepresentative(), relationshipRepresentative)) {
      businessProspect.setRelationshipRepresentative(relationshipRepresentative);
    }

    if (!Objects.equals(businessProspect.getRelationshipExecutive(), relationshipExecutive)) {
      businessProspect.setRelationshipExecutive(relationshipExecutive);
    }

    if (!Objects.equals(businessProspect.getRelationshipDirector(), relationshipDirector)) {
      businessProspect.setRelationshipDirector(relationshipDirector);
    }

    // calculating the prospect status
    if (StringUtils.isNotEmpty(businessProspect.getSubjectRef())) {
      return new BusinessProspectRecord(businessProspect, BusinessProspectStatus.COMPLETED);
    }

    if (businessProspect.isPhoneVerified()) {
      return new BusinessProspectRecord(businessProspect, BusinessProspectStatus.MOBILE_VERIFIED);
    }

    if (businessProspect.isEmailVerified()) {
      return new BusinessProspectRecord(businessProspect, BusinessProspectStatus.EMAIL_VERIFIED);
    }

    if (live) {
      Verification verification = twilioService.sendVerificationEmail(email, businessProspect);
      log.debug("createBusinessProspect: {}", verification);
      if (!"pending".equals(verification.getStatus())) {
        throw new InvalidRequestException(
            String.format(
                "expected pending, got %s for business prospect id %s",
                verification.getStatus(), businessProspect.getId()));
      }
    }

    return new BusinessProspectRecord(businessProspect, BusinessProspectStatus.NEW);
  }

  @Transactional
  public BusinessProspect setBusinessProspectPhone(
      TypedId<BusinessProspectId> businessProspectId, String phone, Boolean live) {
    BusinessProspect businessProspect = retrieveBusinessProspectById(businessProspectId);

    if (businessProspect.getPhone() != null
        && StringUtils.isNotBlank(businessProspect.getPhone().getEncrypted())
        && businessProspect.isPhoneVerified()) {
      throw new InvalidRequestException("phone already set");
    }

    businessProspect.setPhone(new NullableEncryptedString(phone));

    if (Boolean.TRUE.equals(live)) {
      Verification verification = twilioService.sendVerificationSms(phone);
      log.debug("verification: {}", verification);
    }

    return businessProspectRepository.save(businessProspect);
  }

  public void resendValidationCode(
      TypedId<BusinessProspectId> businessProspectId, IdentifierType identifierType, Boolean live) {

    if (Boolean.FALSE.equals(live)) {
      return;
    }

    BusinessProspect businessProspect =
        businessProspectRepository
            .findById(businessProspectId)
            .orElseThrow(
                () -> new RecordNotFoundException(Table.BUSINESS_PROSPECT, businessProspectId));

    switch (identifierType) {
      case PHONE -> {
        if (businessProspect.isPhoneVerified()) {
          throw new InvalidRequestException("phone already validated");
        }
        if (businessProspect.getPhone() == null) {
          throw new InvalidRequestException(
              String.format("Phone is not set for %s.", businessProspectId));
        }
        Verification verification =
            twilioService.sendVerificationSms(businessProspect.getPhone().getEncrypted());
        log.debug("verification: {}", verification);
      }
      case EMAIL -> {
        if (businessProspect.isEmailVerified()) {
          throw new InvalidRequestException("email already validated");
        }
        Verification verification =
            twilioService.sendVerificationEmail(
                businessProspect.getEmail().getEncrypted(), businessProspect);
        log.debug("createBusinessProspectVerificationEmail: {}", verification);
        if (!"pending".equals(verification.getStatus())) {
          throw new InvalidRequestException(
              String.format(
                  "expected pending, got %s for business prospect id %s",
                  verification.getStatus(), businessProspect.getId()));
        }
      }
    }
  }

  @Transactional
  public ValidateIdentifierResponse validateBusinessProspectIdentifier(
      TypedId<BusinessProspectId> businessProspectId,
      IdentifierType identifierType,
      String otp,
      Boolean live) {
    BusinessProspect businessProspect = retrieveBusinessProspectById(businessProspectId);

    switch (identifierType) {
      case EMAIL -> {
        if (businessProspect.isEmailVerified()) {
          throw new InvalidRequestException("email already validated");
        }
        String email = businessProspect.getEmail().getEncrypted();

        if (Boolean.TRUE.equals(live)) {
          VerificationCheck verificationCheck = twilioService.checkVerification(email, otp);
          log.debug("verificationCheck: {}", verificationCheck);
          if (Boolean.FALSE.equals(verificationCheck.getValid())) {
            throw new InvalidRequestException("email otp does not match");
          }
        }
        boolean emailExists =
            businessOwnerService.retrieveBusinessOwnerByEmail(email).isPresent()
                || userService.retrieveUserByEmail(email).isPresent();
        if (emailExists) {
          return new ValidateIdentifierResponse(true);
        }
        businessProspect.setEmailVerified(true);
      }
      case PHONE -> {
        if (!businessProspect.isEmailVerified()) {
          throw new InvalidRequestException("email not yet validated");
        }
        if (businessProspect.isPhoneVerified()) {
          throw new InvalidRequestException("phone already validated");
        }
        if (Boolean.TRUE.equals(live)) {
          VerificationCheck verificationCheck =
              twilioService.checkVerification(businessProspect.getPhone().getEncrypted(), otp);
          log.debug("verificationCheck: {}", verificationCheck);
          if (Boolean.FALSE.equals(verificationCheck.getValid())) {
            throw new InvalidRequestException("phone otp does not match");
          }
        }
        businessProspect.setPhoneVerified(true);
      }
    }

    businessProspectRepository.save(businessProspect);

    return new ValidateIdentifierResponse(false);
  }

  @FusionAuthUserCreator(
      reviewer = "jscarbor",
      explanation = "Beginning of onboarding, no User object exists yet, so not UserService")
  @Transactional
  public BusinessProspect setBusinessProspectPassword(
      TypedId<BusinessProspectId> businessProspectId, String password, Boolean live) {
    BusinessProspect businessProspect = retrieveBusinessProspectById(businessProspectId);

    if (StringUtils.isNotBlank(businessProspect.getSubjectRef())) {
      throw new InvalidRequestException("password already set");
    }

    businessProspect.setSubjectRef(
        fusionAuthService
            .createBusinessOwner(
                businessProspect.getBusinessId(),
                businessProspect.getBusinessOwnerId(),
                businessProspect.getEmail().getEncrypted(),
                password)
            .toString());

    if (Boolean.TRUE.equals(live)) {
      twilioService.sendOnboardingWelcomeEmail(
          businessProspect.getEmail().toString(), businessProspect);
    }

    return businessProspectRepository.save(businessProspect);
  }

  public record ConvertBusinessProspectRecord(
      Business business,
      AllocationRecord rootAllocationRecord,
      BusinessOwner businessOwner,
      User user,
      List<String> stripeAccountCreationMessages) {}

  @CreatesRootAllocation(
      reviewer = "jscarbor",
      explanation = "This is where the business gets created")
  @Transactional
  public ConvertBusinessProspectRecord convertBusinessProspect(
      ConvertBusinessProspect convertBusinessProspect) {
    BusinessProspect businessProspect =
        retrieveBusinessProspectById(convertBusinessProspect.getBusinessProspectId());

    if (StringUtils.isBlank(businessProspect.getSubjectRef())) {
      throw new InvalidRequestException("password has not been set");
    }

    if (businessService.retrieveBusinessByEmployerIdentificationNumber(
            convertBusinessProspect.getEmployerIdentificationNumber())
        != null) {
      throw new InvalidRequestException("Duplicate employer identification number.");
    }

    // When a business is created, a corespondent into stripe will be created too
    BusinessAndStripeAccount businessAndStripeAccount =
        businessService.createBusiness(
            businessProspect.getBusinessId(),
            businessProspect.getBusinessType(),
            businessProspect.getEmail().getEncrypted(),
            convertBusinessProspect,
            businessProspect.getTosAcceptance());

    BusinessOwnerData businessOwnerData = new BusinessOwnerData(businessProspect);

    // On convert step we will create owner without the person stripe corespondent
    BusinessOwnerAndUserRecord businessOwner =
        createMainBusinessOwnerAndRepresentative(businessOwnerData);

    // delete the business prospect so that the owner of the email could register a new business
    businessProspectRepository.delete(businessProspect);

    Business business = businessAndStripeAccount.business();
    AllocationRecord allocationRecord =
        allocationService.createRootAllocation(
            business.getId(), businessOwner.user(), business.getLegalName());

    return new ConvertBusinessProspectRecord(
        business,
        allocationRecord,
        businessOwner.businessOwner(),
        businessOwner.user(),
        Collections.emptyList());
  }

  @Transactional
  public BusinessOwnerAndUserRecord createMainBusinessOwnerAndRepresentative(
      BusinessOwnerData businessOwnerData) {

    BusinessOwner businessOwner = businessOwnerService.createBusinessOwner(businessOwnerData);

    User user =
        userService.createUserForFusionAuthUser(
            new TypedId<>(businessOwner.getId().toUuid()),
            businessOwnerData.getBusinessId(),
            UserType.BUSINESS_OWNER,
            businessOwnerData.getFirstName(),
            businessOwnerData.getLastName(),
            businessOwnerData.getAddress(),
            businessOwnerData.getEmail(),
            businessOwnerData.getPhone(),
            businessOwnerData.getSubjectRef());

    return new BusinessOwnerAndUserRecord(businessOwner, user);
  }

  public Optional<BusinessProspect> retrieveBusinessProspectBySubjectRef(String subjectRef) {
    return businessProspectRepository.findBySubjectRef(subjectRef);
  }

  public BusinessProspect retrieveBusinessProspectById(
      TypedId<BusinessProspectId> businessProspectId) {
    return businessProspectRepository
        .findById(businessProspectId)
        .orElseThrow(
            () -> new RecordNotFoundException(Table.BUSINESS_PROSPECT, businessProspectId));
  }
}
