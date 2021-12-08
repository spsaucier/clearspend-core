package com.tranwall.capital.service;

import com.tranwall.capital.client.alloy.AlloyClient;
import com.tranwall.capital.client.alloy.AlloyClient.KycEvaluationResponse;
import com.tranwall.capital.common.data.model.Address;
import com.tranwall.capital.common.error.RecordNotFoundException;
import com.tranwall.capital.common.error.RecordNotFoundException.Table;
import com.tranwall.capital.common.typedid.data.BusinessId;
import com.tranwall.capital.common.typedid.data.BusinessOwnerId;
import com.tranwall.capital.common.typedid.data.TypedId;
import com.tranwall.capital.crypto.data.model.embedded.NullableEncryptedString;
import com.tranwall.capital.crypto.data.model.embedded.RequiredEncryptedString;
import com.tranwall.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.tranwall.capital.data.model.Alloy;
import com.tranwall.capital.data.model.BusinessOwner;
import com.tranwall.capital.data.model.User;
import com.tranwall.capital.data.model.enums.AlloyTokenType;
import com.tranwall.capital.data.model.enums.BusinessOwnerStatus;
import com.tranwall.capital.data.model.enums.BusinessOwnerType;
import com.tranwall.capital.data.model.enums.Country;
import com.tranwall.capital.data.model.enums.KnowYourCustomerStatus;
import com.tranwall.capital.data.model.enums.RelationshipToBusiness;
import com.tranwall.capital.data.model.enums.UserType;
import com.tranwall.capital.data.repository.AlloyRepository;
import com.tranwall.capital.data.repository.BusinessOwnerRepository;
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
      String subjectRef) {
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
    if (businessOwnerId != null) {
      businessOwner.setId(businessOwnerId);
    }
    businessOwner.setSubjectRef(subjectRef);

    businessOwner = businessOwnerRepository.save(businessOwner);

    User user =
        Objects.isNull(subjectRef)
            ? userService
                .createUser(
                    businessId, UserType.BUSINESS_OWNER, firstName, lastName, address, email, phone)
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
      String alloyGroup) {

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

    return businessOwnerRepository.save(businessOwner);
  }

  public Optional<BusinessOwner> retrieveBusinessOwnerBySubjectRef(String subjectRef) {
    return businessOwnerRepository.findBySubjectRef(subjectRef);
  }

  public List<BusinessOwner> findBusinessOwnerByBusinessId(TypedId<BusinessId> businessIdTypedId) {
    return businessOwnerRepository.findByBusinessId(businessIdTypedId);
  }
}
