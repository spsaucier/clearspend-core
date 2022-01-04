package com.clearspend.capital.service;

import com.clearspend.capital.client.alloy.AlloyClient;
import com.clearspend.capital.client.alloy.AlloyClient.KycEvaluationResponse;
import com.clearspend.capital.client.stripe.StripeClient;
import com.clearspend.capital.common.data.model.Address;
import com.clearspend.capital.common.error.RecordNotFoundException;
import com.clearspend.capital.common.error.RecordNotFoundException.Table;
import com.clearspend.capital.common.typedid.data.BusinessId;
import com.clearspend.capital.common.typedid.data.BusinessOwnerId;
import com.clearspend.capital.common.typedid.data.TypedId;
import com.clearspend.capital.crypto.data.model.embedded.NullableEncryptedString;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedString;
import com.clearspend.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.clearspend.capital.data.model.Alloy;
import com.clearspend.capital.data.model.BusinessOwner;
import com.clearspend.capital.data.model.User;
import com.clearspend.capital.data.model.enums.AlloyTokenType;
import com.clearspend.capital.data.model.enums.BusinessOwnerStatus;
import com.clearspend.capital.data.model.enums.BusinessOwnerType;
import com.clearspend.capital.data.model.enums.Country;
import com.clearspend.capital.data.model.enums.KnowYourCustomerStatus;
import com.clearspend.capital.data.model.enums.RelationshipToBusiness;
import com.clearspend.capital.data.model.enums.UserType;
import com.clearspend.capital.data.repository.AlloyRepository;
import com.clearspend.capital.data.repository.BusinessOwnerRepository;
import com.stripe.model.Person;
import java.time.LocalDate;
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
public class BusinessOwnerService {

  private final TwilioService twilioService;

  private final BusinessOwnerRepository businessOwnerRepository;

  private final AlloyRepository alloyRepository;

  private final UserService userService;

  private final AlloyClient alloyClient;

  private final StripeClient stripeClient;

  private final BusinessService businessService;

  public record BusinessOwnerAndUserRecord(BusinessOwner businessOwner, User user) {}

  @Transactional
  public BusinessOwnerAndUserRecord createBusinessOwner(
      TypedId<BusinessOwnerId> businessOwnerId,
      TypedId<BusinessId> businessId,
      String firstName,
      String lastName,
      Address address,
      String email,
      String phone,
      String subjectRef,
      boolean isOnboarding,
      String alloyGroup) {
    BusinessOwner businessOwner =
        new BusinessOwner(
            businessId,
            BusinessOwnerType.UNSPECIFIED,
            new NullableEncryptedString(firstName),
            new NullableEncryptedString(lastName),
            RelationshipToBusiness.UNSPECIFIED,
            address,
            new RequiredEncryptedStringWithHash(email),
            new RequiredEncryptedString(phone),
            Country.UNSPECIFIED,
            KnowYourCustomerStatus.PENDING,
            BusinessOwnerStatus.ACTIVE);
    businessOwner.setId(businessOwnerId != null ? businessOwnerId : new TypedId<>());
    businessOwner.setSubjectRef(subjectRef);

    if (isOnboarding) {
      KycEvaluationResponse kycEvaluationResponse =
          alloyClient.onboardIndividual(businessOwner, alloyGroup);
      businessOwner.setKnowYourCustomerStatus(kycEvaluationResponse.status());

      if (kycEvaluationResponse.status() == KnowYourCustomerStatus.REVIEW) {
        Alloy alloy =
            new Alloy(
                businessOwner.getBusinessId(),
                businessOwnerId,
                AlloyTokenType.BUSINESS_OWNER,
                kycEvaluationResponse.entityToken());
        alloyRepository.save(alloy);
      }
    }

    businessOwner = businessOwnerRepository.save(businessOwner);
    businessOwnerRepository.flush();

    Person stripePerson =
        stripeClient.createPerson(
            businessOwner, businessService.retrieveBusiness(businessId).getExternalRef());
    businessOwner.setExternalRef(stripePerson.getId());
    //
    //    User user =
    //        Objects.isNull(subjectRef)
    //            ? userService
    //                .createUser(
    //                    businessId, UserType.BUSINESS_OWNER, firstName, lastName, address, email,
    // phone)
    //                .user()
    //            : userService.createUserForFusionAuthUser(
    //                new TypedId<>(businessOwner.getId().toUuid()),
    //                businessId,
    //                UserType.BUSINESS_OWNER,
    //                firstName,
    //                lastName,
    //                address,
    //                email,
    //                phone,
    //                subjectRef);

    User user = null;
    // check if this is the first business owner added.
    // https://tranwall.atlassian.net/browse/CAP-288
    if (businessOwnerRepository.findByBusinessId(businessId).size() == 1) {
      user =
          Objects.isNull(subjectRef)
              ? userService
                  .createUser(
                      businessId,
                      UserType.BUSINESS_OWNER,
                      firstName,
                      lastName,
                      address,
                      email,
                      phone)
                  .user()
              : userService.createUserForFusionAuthUser(
                  new TypedId<>(businessOwner.getId().toUuid()),
                  businessId,
                  UserType.BUSINESS_OWNER,
                  firstName,
                  lastName,
                  address,
                  email,
                  phone,
                  subjectRef);
    }

    return new BusinessOwnerAndUserRecord(businessOwner, user);
  }

  public BusinessOwner retrieveBusinessOwner(TypedId<BusinessOwnerId> businessOwnerId) {
    return businessOwnerRepository
        .findById(businessOwnerId)
        .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS_OWNER, businessOwnerId));
  }

  public Optional<BusinessOwner> retrieveBusinessOwnerNotThrowingException(
      TypedId<BusinessOwnerId> businessOwnerId) {
    return businessOwnerRepository.findById(businessOwnerId);
  }

  @Transactional
  public BusinessOwner updateBusinessOwnerStatus(
      TypedId<BusinessOwnerId> businessOwnerId, KnowYourCustomerStatus KnowYourCustomerStatus) {

    BusinessOwner businessOwner =
        businessOwnerRepository
            .findById(businessOwnerId)
            .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS_OWNER, businessOwnerId));

    if (KnowYourCustomerStatus != null) {
      businessOwner.setKnowYourCustomerStatus(KnowYourCustomerStatus);
    }

    return businessOwnerRepository.save(businessOwner);
  }

  @Transactional
  public BusinessOwner updateBusinessOwner(
      TypedId<BusinessOwnerId> businessOwnerId,
      String firstName,
      String lastName,
      String email,
      String taxIdentificationNumber,
      LocalDate dateOfBirth,
      Address address,
      String alloyGroup,
      boolean isOnboarding) {

    BusinessOwner businessOwner =
        businessOwnerRepository
            .findById(businessOwnerId)
            .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS_OWNER, businessOwnerId));

    if (StringUtils.isNotBlank(firstName)) {
      businessOwner.setFirstName(new NullableEncryptedString(firstName));
    }
    if (StringUtils.isNotBlank(lastName)) {
      businessOwner.setLastName(new NullableEncryptedString(lastName));
    }
    if (StringUtils.isNotBlank(email)) {
      businessOwner.setEmail(new RequiredEncryptedStringWithHash(email));
    }
    if (StringUtils.isNotBlank(taxIdentificationNumber)) {
      businessOwner.setTaxIdentificationNumber(
          new NullableEncryptedString(taxIdentificationNumber));
    }
    if (dateOfBirth != null) {
      businessOwner.setDateOfBirth(dateOfBirth);
    }
    if (address != null) {
      businessOwner.setAddress(address);
    }

    if (isOnboarding) {
      KycEvaluationResponse kycEvaluationResponse =
          alloyClient.onboardIndividual(businessOwner, alloyGroup);
      businessOwner.setKnowYourCustomerStatus(kycEvaluationResponse.status());

      switch (kycEvaluationResponse.status()) {
        case FAIL -> twilioService.sendKybKycFailEmail(
            businessOwner.getEmail().getEncrypted(),
            businessOwner.getFirstName().getEncrypted(),
            kycEvaluationResponse.reasons());
        case REVIEW -> {
          Alloy alloy =
              new Alloy(
                  businessOwner.getBusinessId(),
                  businessOwnerId,
                  AlloyTokenType.BUSINESS_OWNER,
                  kycEvaluationResponse.entityToken());
          alloyRepository.save(alloy);
        }
        case PASS -> twilioService.sendKybKycPassEmail(
            businessOwner.getEmail().getEncrypted(), businessOwner.getFirstName().getEncrypted());
      }
    }

    return businessOwnerRepository.save(businessOwner);
  }

  public Optional<BusinessOwner> retrieveBusinessOwnerBySubjectRef(String subjectRef) {
    return businessOwnerRepository.findBySubjectRef(subjectRef);
  }

  public List<BusinessOwner> findBusinessOwnerByBusinessId(TypedId<BusinessId> businessIdTypedId) {
    return businessOwnerRepository.findByBusinessId(businessIdTypedId);
  }

  public BusinessOwner findBusinessOwnerPrincipalByBusinessId(
      TypedId<BusinessId> businessIdTypedId) {
    return businessOwnerRepository
        .findByBusinessIdAndSubjectRefIsNotNull(businessIdTypedId)
        .orElseThrow();
  }
}
