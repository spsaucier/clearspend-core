package com.clearspend.capital.service;

import com.clearspend.capital.common.data.model.Address;
import com.clearspend.capital.common.error.InvalidRequestException;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.RecordNotFoundException.Table;
import com.clearspend.capital.common.typedid.data.BusinessOwnerId;
import com.clearspend.capital.common.typedid.data.BusinessProspectId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.controller.type.business.prospect.BusinessProspectStatus;
import com.clearspend.capital.controller.type.business.prospect.ValidateBusinessProspectIdentifierRequest.IdentifierType;
import com.clearspend.capital.crypto.data.model.embedded.NullableEncryptedString;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedString;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.clearspend.capital.data.model.Business;
import com.clearspend.capital.data.model.BusinessOwner;
import com.clearspend.capital.data.model.BusinessProspect;
import com.clearspend.capital.data.model.enums.BusinessType;
import com.clearspend.capital.data.model.enums.Currency;
import com.clearspend.capital.data.repository.BusinessProspectRepository;
import com.clearspend.capital.service.AllocationService.AllocationRecord;
import com.clearspend.capital.service.BusinessOwnerService.BusinessOwnerAndUserRecord;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
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
  public BusinessProspectRecord createBusinessProspect(
      String firstName, String lastName, String email) {
    BusinessProspect businessProspect =
        businessProspectRepository
            .findByEmailHash(new RequiredEncryptedStringWithHash(email).getHash())
            .orElseGet(
                () ->
                    businessProspectRepository.save(
                        new BusinessProspect(
                            new RequiredEncryptedString(firstName),
                            new RequiredEncryptedString(lastName),
                            new RequiredEncryptedStringWithHash(email))));

    // Update first/last names in case a prospect has been resumed with different values
    if (!Objects.equals(businessProspect.getFirstName().getEncrypted(), firstName)) {
      businessProspect.setFirstName(new RequiredEncryptedString(firstName));
    }

    if (!Objects.equals(businessProspect.getLastName().getEncrypted(), lastName)) {
      businessProspect.setLastName(new RequiredEncryptedString(lastName));
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

    Verification verification = twilioService.sendVerificationEmail(email, businessProspect);
    log.debug("createBusinessProspect: {}", verification);
    if (!"pending".equals(verification.getStatus())) {
      throw new RuntimeException(
          String.format("expected pending, got %s", verification.getStatus()));
    }

    return new BusinessProspectRecord(businessProspect, BusinessProspectStatus.NEW);
  }

  @Transactional
  public BusinessProspect setBusinessProspectPhone(
      TypedId<BusinessProspectId> businessProspectId, String phone) {
    BusinessProspect businessProspect =
        businessProspectRepository
            .findById(businessProspectId)
            .orElseThrow(
                () -> new RecordNotFoundException(Table.BUSINESS_PROSPECT, businessProspectId));

    if (businessProspect.getPhone() != null
        && StringUtils.isNotBlank(businessProspect.getPhone().getEncrypted())
        && businessProspect.isPhoneVerified()) {
      throw new InvalidRequestException("phone already set");
    }

    businessProspect.setPhone(new NullableEncryptedString(phone));

    Verification verification = twilioService.sendVerificationSms(phone);
    log.debug("verification: {}", verification);

    return businessProspectRepository.save(businessProspect);
  }

  @Transactional
  public BusinessProspect validateBusinessProspectIdentifier(
      TypedId<BusinessProspectId> businessProspectId, IdentifierType identifierType, String otp) {
    BusinessProspect businessProspect =
        businessProspectRepository
            .findById(businessProspectId)
            .orElseThrow(
                () -> new RecordNotFoundException(Table.BUSINESS_PROSPECT, businessProspectId));

    switch (identifierType) {
      case EMAIL -> {
        if (businessProspect.isEmailVerified()) {
          throw new InvalidRequestException("email already validated");
        }
        VerificationCheck verificationCheck =
            twilioService.checkVerification(businessProspect.getEmail().getEncrypted(), otp);
        log.debug("verificationCheck: {}", verificationCheck);
        if (!verificationCheck.getValid()) {
          throw new InvalidRequestException("email otp does not match");
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
        VerificationCheck verificationCheck =
            twilioService.checkVerification(businessProspect.getPhone().getEncrypted(), otp);
        log.debug("verificationCheck: {}", verificationCheck);
        if (!verificationCheck.getValid()) {
          throw new InvalidRequestException("phone otp does not match");
        }
        businessProspect.setPhoneVerified(true);
      }
    }

    return businessProspectRepository.save(businessProspect);
  }

  @Transactional
  public BusinessProspect setBusinessProspectPassword(
      TypedId<BusinessProspectId> businessProspectId, String password) {
    BusinessProspect businessProspect =
        businessProspectRepository
            .findById(businessProspectId)
            .orElseThrow(
                () -> new RecordNotFoundException(Table.BUSINESS_PROSPECT, businessProspectId));

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

    twilioService.sendOnboardingWelcomeEmail(
        businessProspect.getEmail().toString(), businessProspect);

    return businessProspectRepository.save(businessProspect);
  }

  public record ConvertBusinessProspectRecord(
      Business business, AllocationRecord rootAllocationRecord, BusinessOwner businessOwner) {}

  @Transactional
  public ConvertBusinessProspectRecord convertBusinessProspect(
      TypedId<BusinessProspectId> businessProspectId,
      String legalName,
      BusinessType businessType,
      String businessPhone,
      String employerIdentificationNumber,
      Address toAddress) {
    BusinessProspect businessProspect =
        businessProspectRepository
            .findById(businessProspectId)
            .orElseThrow(
                () -> new RecordNotFoundException(Table.BUSINESS_PROSPECT, businessProspectId));

    if (StringUtils.isBlank(businessProspect.getSubjectRef())) {
      throw new InvalidRequestException("password has not been set");
    }

    Business business =
        businessService.createBusiness(
            businessProspect.getBusinessId(),
            legalName,
            businessType,
            toAddress,
            employerIdentificationNumber,
            businessProspect.getEmail().getEncrypted(),
            businessPhone,
            Currency.USD);

    BusinessOwnerAndUserRecord businessOwner =
        businessOwnerService.createBusinessOwner(
            businessProspect.getBusinessOwnerId(),
            business.getId(),
            businessProspect.getFirstName().getEncrypted(),
            businessProspect.getLastName().getEncrypted(),
            toAddress,
            businessProspect.getEmail().getEncrypted(),
            businessProspect.getPhone().getEncrypted(),
            businessProspect.getSubjectRef(),
            false,
            null);

    // delete the business prospect so that the owner of the email could register a new business
    // later
    businessProspectRepository.delete(businessProspect);

    AllocationRecord allocationRecord =
        allocationService.createRootAllocation(
            business.getId(), businessOwner.user(), business.getLegalName() + " - root");

    return new ConvertBusinessProspectRecord(
        business, allocationRecord, businessOwner.businessOwner());
  }

  public Optional<BusinessProspect> retrieveBusinessProspectBySubjectRef(String subjectRef) {
    return businessProspectRepository.findBySubjectRef(subjectRef);
  }
}
