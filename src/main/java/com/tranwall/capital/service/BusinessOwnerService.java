package com.tranwall.capital.service;

import com.tranwall.capital.common.data.model.Address;
import com.tranwall.capital.common.error.RecordNotFoundException;
import com.tranwall.capital.common.error.RecordNotFoundException.Table;
import com.tranwall.capital.crypto.data.model.embedded.NullableEncryptedString;
import com.tranwall.capital.crypto.data.model.embedded.RequiredEncryptedString;
import com.tranwall.capital.crypto.data.model.embedded.RequiredEncryptedStringWithHash;
import com.tranwall.capital.data.model.BusinessOwner;
import com.tranwall.capital.data.model.enums.BusinessOwnerStatus;
import com.tranwall.capital.data.model.enums.BusinessOwnerType;
import com.tranwall.capital.data.model.enums.Country;
import com.tranwall.capital.data.model.enums.KnowYourCustomerStatus;
import com.tranwall.capital.data.model.enums.RelationshipToBusiness;
import com.tranwall.capital.data.repository.BusinessOwnerRepository;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BusinessOwnerService {

  private final BusinessOwnerRepository businessOwnerRepository;

  @Transactional
  BusinessOwner createBusinessOwner(
      UUID businessOwnerId,
      UUID businessId,
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

    return businessOwnerRepository.save(businessOwner);
  }

  public BusinessOwner retrieveBusinessOwner(UUID businessOwnerId) {
    return businessOwnerRepository
        .findById(businessOwnerId)
        .orElseThrow(() -> new RecordNotFoundException(Table.BUSINESS_OWNER, businessOwnerId));
  }

  @Transactional
  public BusinessOwner updateBusinessOwner(
      UUID businessOwnerId,
      String firstName,
      String lastName,
      String email,
      String taxIdentificationNumber,
      LocalDate dateOfBirth,
      Address address) {

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

    return businessOwnerRepository.save(businessOwner);
  }
}
